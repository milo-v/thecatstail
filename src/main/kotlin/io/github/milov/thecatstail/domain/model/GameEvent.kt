package io.github.milov.thecatstail.domain.model

sealed class GameEvent {
    data class MatchStarted(val player1Id: String, val player2Id: String) : GameEvent()
    data class RoundStarted(val roundNumber: Int) : GameEvent()
    data class DiceRolled(val userId: String, val dice: List<Element>) : GameEvent()
    data class SkillUsed(val userId: String, val characterId: String, val skillId: String, val skillName: String) : GameEvent()
    data class DamageDealt(val targetCharacterId: String, val amount: Int, val reaction: String? = null, val isFatal: Boolean = false) : GameEvent()
    data class CharacterSwitched(val userId: String, val fromIndex: Int, val toIndex: Int) : GameEvent()
    data class EnergyGained(val characterId: String, val amount: Int) : GameEvent()
    data class EnergyReset(val characterId: String) : GameEvent()
    data class PlayerDeclaredEnd(val userId: String) : GameEvent()
    data class RoundEnded(val roundNumber: Int) : GameEvent()
    data class MatchEnded(val winnerId: String) : GameEvent()
}
