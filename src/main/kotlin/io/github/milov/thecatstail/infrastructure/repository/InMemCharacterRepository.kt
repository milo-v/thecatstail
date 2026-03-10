package io.github.milov.thecatstail.infrastructure.repository

import io.github.milov.thecatstail.domain.model.*
import io.github.milov.thecatstail.domain.repository.CharacterRepository
import org.springframework.stereotype.Repository

@Repository
class InMemCharacterRepository : CharacterRepository {
  private val characters =
      listOf(
          Character(
              "Diluc", "Diluc", Element.PYRO, maxEnergy = 3,
              skills = listOf(
                  Skill("diluc-normal", "Tempered Sword", SkillType.NORMAL_ATTACK, listOf(DiceCost(Element.PYRO, 1), DiceCost(Element.UNALIGNED, 2)), 2, Element.UNALIGNED),
                  Skill("diluc-skill", "Searing Onslaught", SkillType.ELEMENTAL_SKILL, listOf(DiceCost(Element.PYRO, 3)), 3, Element.PYRO),
                  Skill("diluc-burst", "Dawn", SkillType.ELEMENTAL_BURST, listOf(DiceCost(Element.PYRO, 4)), 8, Element.PYRO)
              )
          ),
          Character(
              "Kaeya", "Kaeya", Element.CRYO, maxEnergy = 2,
              skills = listOf(
                  Skill("kaeya-normal", "Ceremonial Bladework", SkillType.NORMAL_ATTACK, listOf(DiceCost(Element.CRYO, 1), DiceCost(Element.UNALIGNED, 2)), 2, Element.UNALIGNED),
                  Skill("kaeya-skill", "Frostgnaw", SkillType.ELEMENTAL_SKILL, listOf(DiceCost(Element.CRYO, 3)), 3, Element.CRYO),
                  Skill("kaeya-burst", "Glacial Waltz", SkillType.ELEMENTAL_BURST, listOf(DiceCost(Element.CRYO, 4)), 1, Element.CRYO, summonEffect = Summon("glacial-waltz", "Glacial Waltz", Element.CRYO, 2, 3))
              )
          ),
          Character(
              "Sucrose", "Sucrose", Element.ANEMO, maxEnergy = 2,
              skills = listOf(
                  Skill("sucrose-normal", "Wind Spirit Creation", SkillType.NORMAL_ATTACK, listOf(DiceCost(Element.ANEMO, 1), DiceCost(Element.UNALIGNED, 2)), 1, Element.ANEMO),
                  Skill("sucrose-skill", "Astable Anemohypostasis", SkillType.ELEMENTAL_SKILL, listOf(DiceCost(Element.ANEMO, 3)), 3, Element.ANEMO),
                  Skill("sucrose-burst", "Forbidden Creation", SkillType.ELEMENTAL_BURST, listOf(DiceCost(Element.ANEMO, 3)), 1, Element.ANEMO, summonEffect = Summon("wind-spirit", "Large Wind Spirit", Element.ANEMO, 2, 2))
              )
          ),
          Character(
              "Fischl", "Fischl", Element.ELECTRO, maxEnergy = 3,
              skills = listOf(
                  Skill("fischl-normal", "Bolts of Downfall", SkillType.NORMAL_ATTACK, listOf(DiceCost(Element.ELECTRO, 1), DiceCost(Element.UNALIGNED, 2)), 2, Element.UNALIGNED),
                  Skill("fischl-skill", "Nightrider", SkillType.ELEMENTAL_SKILL, listOf(DiceCost(Element.ELECTRO, 3)), 1, Element.ELECTRO, summonEffect = Summon("oz", "Oz", Element.ELECTRO, 1, 2)),
                  Skill("fischl-burst", "Midnight Phantasmagoria", SkillType.ELEMENTAL_BURST, listOf(DiceCost(Element.ELECTRO, 3)), 4, Element.ELECTRO)
              )
          ),
          Character(
              "Mona", "Mona", Element.HYDRO, maxEnergy = 3,
              skills = listOf(
                  Skill("mona-normal", "Ripple of Fate", SkillType.NORMAL_ATTACK, listOf(DiceCost(Element.HYDRO, 1), DiceCost(Element.UNALIGNED, 2)), 1, Element.HYDRO),
                  Skill("mona-skill", "Mirror Reflection of Doom", SkillType.ELEMENTAL_SKILL, listOf(DiceCost(Element.HYDRO, 3)), 1, Element.HYDRO, summonEffect = Summon("reflection", "Reflection", Element.HYDRO, 1, 1)),
                  Skill("mona-burst", "Stellaris Phantasm", SkillType.ELEMENTAL_BURST, listOf(DiceCost(Element.HYDRO, 3)), 4, Element.HYDRO)
              )
          ),
          Character(
              "Collei", "Collei", Element.DENDRO, maxEnergy = 2,
              skills = listOf(
                  Skill("collei-normal", "Supplicant's Bowmanship", SkillType.NORMAL_ATTACK, listOf(DiceCost(Element.DENDRO, 1), DiceCost(Element.UNALIGNED, 2)), 2, Element.UNALIGNED),
                  Skill("collei-skill", "Floral Brush", SkillType.ELEMENTAL_SKILL, listOf(DiceCost(Element.DENDRO, 3)), 3, Element.DENDRO),
                  Skill("collei-burst", "Trump-Card Kitty", SkillType.ELEMENTAL_BURST, listOf(DiceCost(Element.DENDRO, 3)), 2, Element.DENDRO, summonEffect = Summon("cuilein-anbar", "Cuilein-Anbar", Element.DENDRO, 2, 2))
              )
          ),
          Character(
              "Noelle", "Noelle", Element.GEO, maxEnergy = 2,
              skills = listOf(
                  Skill("noelle-normal", "Favonius Bladework", SkillType.NORMAL_ATTACK, listOf(DiceCost(Element.GEO, 1), DiceCost(Element.UNALIGNED, 2)), 2, Element.UNALIGNED),
                  Skill("noelle-skill", "Breastplate", SkillType.ELEMENTAL_SKILL, listOf(DiceCost(Element.GEO, 3)), 1, Element.GEO),
                  Skill("noelle-burst", "Sweeping Time", SkillType.ELEMENTAL_BURST, listOf(DiceCost(Element.GEO, 4)), 4, Element.GEO, applyStatus = CharacterStatus("luminous-armor", "Luminous Armor", elementOverride = Element.GEO, damageBonus = 2, durationRounds = 2))
              )
          )
      ).associateBy { it.id }

  override fun getById(id: String): Character {
    return characters[id]
        ?: throw NoSuchElementException("Character with id $id not found")
  }
}
