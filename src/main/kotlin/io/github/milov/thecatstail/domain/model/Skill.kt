package io.github.milov.thecatstail.domain.model

data class Skill(
    val id: String,
    val name: String,
    val type: SkillType,
    val cost: List<DiceCost>,
    val baseDamage: Int = 0,
    val element: Element = Element.UNALIGNED,
    val energyGenerated: Int = 1,
    val description: String = ""
)

data class DiceCost(
    val element: Element,
    val amount: Int
)
