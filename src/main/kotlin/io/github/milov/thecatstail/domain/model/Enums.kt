package io.github.milov.thecatstail.domain.model

enum class Element {
    PYRO, HYDRO, ANEMO, ELECTRO, DENDRO, CRYO, GEO, OMNI, UNALIGNED
}

enum class SkillType {
    NORMAL_ATTACK, ELEMENTAL_SKILL, ELEMENTAL_BURST
}

enum class ActionCardType {
    EQUIPMENT, SUPPORT, EVENT
}

enum class EquipmentType {
    WEAPON, ARTIFACT, OTHER
}

enum class SupportType {
    LOCATION, COMPANION, ITEM, OTHER
}

enum class EventType {
    ELEMENTAL_RESONANCE, FOOD, OTHER
}

enum class Phase {
    ROLL, ACTION, END
}
