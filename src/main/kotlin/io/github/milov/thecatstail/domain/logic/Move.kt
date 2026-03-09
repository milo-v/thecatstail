package io.github.milov.thecatstail.domain.logic

import io.github.milov.thecatstail.domain.model.Element

sealed class Move {
    data class UseSkill(val skillId: String) : Move()
    data class SwitchCharacter(val targetIndex: Int) : Move()
    data class PlayCard(val cardId: String) : Move()
    data class ElementalTuning(val cardId: String, val targetDieElement: Element) : Move()
    object DeclareEnd : Move()
}
