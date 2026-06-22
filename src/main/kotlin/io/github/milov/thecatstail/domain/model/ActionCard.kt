package io.github.milov.thecatstail.domain.model

import io.github.milov.thecatstail.domain.base.DomainEntity

class ActionCard(
    id: String,
    val name: String,
    val type: ActionCardType,
    val subType: Any? = null, // Will hold EquipmentType, SupportType, or EventType
    val cost: List<DiceCost> = emptyList(),
    val description: String = "",
    val supportEffect: SupportCard? = null,
    val imageUrl: String = "",
    val tags: List<String> = emptyList(),
    val rawTags: List<String> = emptyList(),
    val talentCharacterId: String? = null
) : DomainEntity(id) {
    val isTalent: Boolean
        get() = tags.any { it.equals("Talent", ignoreCase = true) } ||
            rawTags.any { it.contains("TALENT", ignoreCase = true) }

    val isCombatAction: Boolean
        get() = rawTags.any { it.contains("GCG_TAG_SLOWLY", ignoreCase = true) } ||
            tags.any { it.equals("Combat Action", ignoreCase = true) }

    val isWeapon: Boolean
        get() = rawTags.any { it.contains("GCG_TAG_WEAPON", ignoreCase = true) } ||
            tags.any { it.equals("Weapon", ignoreCase = true) }

    val isArtifact: Boolean
        get() = rawTags.any { it.contains("GCG_TAG_ARTIFACT", ignoreCase = true) } ||
            tags.any { it.equals("Artifact", ignoreCase = true) }

    val isFood: Boolean
        get() = rawTags.any { it.contains("GCG_TAG_FOOD", ignoreCase = true) } ||
            tags.any { it.equals("Food", ignoreCase = true) }

    val isResonance: Boolean
        get() = rawTags.any { it.contains("GCG_TAG_RESONANCE", ignoreCase = true) } ||
            tags.any { it.contains("Resonance", ignoreCase = true) }
}
