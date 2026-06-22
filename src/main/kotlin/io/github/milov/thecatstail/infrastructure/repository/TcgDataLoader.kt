package io.github.milov.thecatstail.infrastructure.repository

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.milov.thecatstail.domain.model.*
import org.springframework.stereotype.Component
import java.io.InputStream

@JsonIgnoreProperties(ignoreUnknown = true)
data class JsonSkillCost(
    val costtype: String,
    val count: Int
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class JsonSkill(
    val id: Int,
    val name: String,
    val type: String,
    val typetag: String? = null,
    val basedamage: Int = 0,
    val baseelement: String? = null,
    val description: String = "",
    val playcost: List<JsonSkillCost> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class JsonImage(
    val filename_cardface: String = ""
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class JsonCharacter(
    val id: Int,
    val name: String,
    val hp: Int,
    val maxenergy: Int,
    val tagstext: List<String>,
    val skills: List<JsonSkill>,
    val storytext: String = "",
    val image: JsonImage? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class JsonSummon(
    val id: Int,
    val name: String,
    val description: String = "",
    val hinttype: String? = null,
    val countingtype: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class JsonStatus(
    val id: Int,
    val name: String,
    val statustypetext: String = "",
    val description: String = ""
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class JsonActionCard(
    val id: Int,
    val name: String,
    val cardtypetext: String,
    val description: String = "",
    val playcost: List<JsonSkillCost> = emptyList(),
    val tags: List<String> = emptyList(),
    val tagstext: List<String> = emptyList(),
    val image: JsonImage? = null
)

@Component
class TcgDataLoader {
    private val mapper = jacksonObjectMapper()
    private val imageBaseUrl = "https://raw.githubusercontent.com/piovium/assets/master/dist/assets/"

    fun loadCharacters(): List<Character> {
        val stream: InputStream = javaClass.getResourceAsStream("/data/characters_3_3.json") ?: return emptyList()
        val jsonCharacters: List<JsonCharacter> = mapper.readValue(stream)
        
        return jsonCharacters.map { jc ->
            Character(
                id = jc.name,
                name = jc.name,
                element = mapElement(jc.tagstext.firstOrNull { it.contains("Element") } ?: jc.tagstext.first()),
                maxHp = jc.hp,
                currentHp = jc.hp,
                maxEnergy = jc.maxenergy,
                description = jc.storytext,
                imageUrl = if (jc.image?.filename_cardface?.isNotEmpty() == true) "${imageBaseUrl}${jc.image.filename_cardface}.webp" else "",
                skills = jc.skills.map { js ->
                    Skill(
                        id = js.id.toString(),
                        name = js.name,
                        type = mapSkillType(js.type, js.typetag),
                        cost = js.playcost.map { DiceCost(mapElement(it.costtype), it.count) },
                        baseDamage = js.basedamage,
                        element = mapElement(js.baseelement ?: "GCG_ELEMENT_NONE"),
                        description = js.description
                    )
                }
            )
        }
    }

    fun loadActionCards(): List<ActionCard> {
        val stream: InputStream = javaClass.getResourceAsStream("/data/action_cards_3_3.json") ?: return emptyList()
        val jsonCards: List<JsonActionCard> = mapper.readValue(stream)

        return jsonCards.map { jc ->
            val isTalent = jc.tagstext.any { it.equals("Talent", true) } || jc.tags.any { it.contains("TALENT", true) }
            val talentChar = if (isTalent) extractTalentCharacterName(jc.description) else null
            ActionCard(
                id = jc.id.toString(),
                name = jc.name,
                type = mapCardType(jc.cardtypetext),
                cost = jc.playcost.map { DiceCost(mapElement(it.costtype), it.count) },
                description = jc.description,
                imageUrl = if (jc.image?.filename_cardface?.isNotEmpty() == true) "${imageBaseUrl}${jc.image.filename_cardface}.webp" else "",
                tags = jc.tagstext,
                rawTags = jc.tags,
                talentCharacterId = talentChar
            )
        }
    }

    fun loadSummons(): List<SummonMetadata> {
        val stream: InputStream = javaClass.getResourceAsStream("/data/summons_all.json") ?: return emptyList()
        // The file is a map { key: JsonSummon, ... }
        val map: Map<String, JsonSummon> = mapper.readValue(stream)
        val damageRegex = Regex("Deal (\\d+)", RegexOption.IGNORE_CASE)
        val usageRegex = Regex("Usage\\(s\\):\\s*(\\d+)", RegexOption.IGNORE_CASE)
        return map.values.map { js ->
            val damage = damageRegex.find(js.description)?.groupValues?.get(1)?.toIntOrNull() ?: 1
            val usages = usageRegex.find(js.description)?.groupValues?.get(1)?.toIntOrNull() ?: 1
            val element = js.hinttype?.let { mapElement(it) } ?: Element.UNALIGNED
            SummonMetadata(
                id = js.id.toString(),
                name = js.name,
                element = element,
                usages = usages,
                baseDamage = damage,
                description = js.description
            )
        }
    }

    fun loadStatuses(): List<StatusMetadata> {
        val stream: InputStream = javaClass.getResourceAsStream("/data/statuses_all.json") ?: return emptyList()
        val map: Map<String, JsonStatus> = mapper.readValue(stream)
        return map.values.map { js ->
            StatusMetadata(
                id = js.id.toString(),
                name = js.name,
                statusType = js.statustypetext,
                description = js.description
            )
        }
    }

    private fun extractTalentCharacterName(description: String): String? {
        // Patterns:
        //   "When your active character is Ganyu, equip this card."
        //   "(You must have Ganyu in your deck to add this card to your deck.)"
        val patterns = listOf(
            Regex("active character is ([A-Z][A-Za-z '\\-]+?),"),
            Regex("You must have ([A-Z][A-Za-z '\\-]+?) in your deck")
        )
        for (p in patterns) {
            val m = p.find(description)
            if (m != null) return m.groupValues[1].trim()
        }
        return null
    }

    private fun mapElement(text: String): Element {
        return when {
            text.contains("PYRO", true) -> Element.PYRO
            text.contains("HYDRO", true) -> Element.HYDRO
            text.contains("ANEMO", true) -> Element.ANEMO
            text.contains("ELECTRO", true) -> Element.ELECTRO
            text.contains("DENDRO", true) -> Element.DENDRO
            text.contains("CRYO", true) -> Element.CRYO
            text.contains("GEO", true) -> Element.GEO
            text.contains("OMNI", true) -> Element.OMNI
            text.contains("VOID", true) || text.contains("UNALIGNED", true) || text.contains("NONE", true) -> Element.UNALIGNED
            else -> Element.UNALIGNED
        }
    }

    private fun mapSkillType(text: String, typetag: String? = null): SkillType {
        if (typetag != null && typetag.contains("PASSIVE", true)) return SkillType.PASSIVE
        return when {
            text.contains("Passive", true) -> SkillType.PASSIVE
            text.contains("Normal Attack", true) -> SkillType.NORMAL_ATTACK
            text.contains("Elemental Skill", true) -> SkillType.ELEMENTAL_SKILL
            text.contains("Elemental Burst", true) -> SkillType.ELEMENTAL_BURST
            else -> SkillType.NORMAL_ATTACK
        }
    }

    private fun mapCardType(text: String): ActionCardType {
        return when {
            text.contains("Equipment", true) -> ActionCardType.EQUIPMENT
            text.contains("Support", true) -> ActionCardType.SUPPORT
            text.contains("Event", true) -> ActionCardType.EVENT
            else -> ActionCardType.EVENT
        }
    }
}
