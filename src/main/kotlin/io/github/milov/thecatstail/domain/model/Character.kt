package io.github.milov.thecatstail.domain.model

import io.github.milov.thecatstail.domain.base.DomainEntity

class Character(
    id: String,
    val name: String,
    val element: Element,
    val maxHp: Int = 10,
    var currentHp: Int = 10,
    val maxEnergy: Int,
    var currentEnergy: Int = 0,
    val skills: List<Skill> = emptyList(),
    var isAlive: Boolean = true,
    var isFront: Boolean = false,
    val appliedElements: MutableList<Element> = mutableListOf(),
    val activeStatuses: MutableList<CharacterStatus> = mutableListOf(),
    var equipment: CharacterEquipment = CharacterEquipment(),
    val description: String = "",
    val imageUrl: String = ""
) : DomainEntity(id) {
    fun deepCopy(): Character {
        return Character(
            id = id,
            name = name,
            element = element,
            maxHp = maxHp,
            currentHp = currentHp,
            maxEnergy = maxEnergy,
            currentEnergy = currentEnergy,
            skills = skills,
            isAlive = isAlive,
            isFront = isFront,
            appliedElements = appliedElements.toMutableList(),
            activeStatuses = activeStatuses.map { it.deepCopy() }.toMutableList(),
            equipment = equipment.deepCopy(),
            description = description,
            imageUrl = imageUrl
        )
    }
}
