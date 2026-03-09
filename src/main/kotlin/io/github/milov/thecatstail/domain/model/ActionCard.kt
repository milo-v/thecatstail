package io.github.milov.thecatstail.domain.model

import io.github.milov.thecatstail.domain.base.DomainEntity

class ActionCard(
    id: String,
    val name: String,
    val type: ActionCardType,
    val subType: Any? = null, // Will hold EquipmentType, SupportType, or EventType
    val cost: List<DiceCost> = emptyList(),
    val description: String = ""
) : DomainEntity(id)
