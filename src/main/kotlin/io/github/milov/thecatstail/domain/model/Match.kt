package io.github.milov.thecatstail.domain.model

import io.github.milov.thecatstail.domain.base.DomainEntity

class Match(
    id: String,
    val players: List<PlayerState>,
    var activePlayerIndex: Int = 0,
    var roundNumber: Int = 1,
    var phase: Phase = Phase.ROLL,
    var firstPlayerIndex: Int = 0,
    val eventLog: MutableList<GameEvent> = mutableListOf()
) : DomainEntity(id) {

    fun getActivePlayer(): PlayerState = players[activePlayerIndex]
    fun getInactivePlayer(): PlayerState = players[(activePlayerIndex + 1) % 2]

    fun isForcedSwitchRequired(userId: String): Boolean {
        val player = players.find { it.userId == userId } ?: return false
        return !player.getActiveCharacter().isAlive && player.characters.any { it.isAlive }
    }

    fun applyEvent(event: GameEvent) {
        eventLog.add(event)
        // Note: For now, MatchManager still modifies state directly during moves.
        // In a strict Event Sourcing model, this method would be the ONLY way to modify state.
        // For the prototype, we use events primarily for replayability and the action log.
    }

    fun generateTextLog(): List<String> {
        return eventLog.map { event ->
            when (event) {
                is GameEvent.SkillUsed -> "${event.userId}'s ${event.characterId} used ${event.skillName}!"
                is GameEvent.DamageDealt -> {
                    val reactionPart = event.reaction?.let { " ($it!)" } ?: ""
                    val swirlPart = if (event.reaction == "Swirl") " (All benched characters took 1 Piercing DMG)" else ""
                    "Dealt ${event.amount} damage to ${event.targetCharacterId}$reactionPart.$swirlPart"
                }
                is GameEvent.CharacterSwitched -> "${event.userId} switched to character #${event.toIndex}."
                is GameEvent.PlayerDeclaredEnd -> "${event.userId} declared end of round."
                is GameEvent.RoundStarted -> "Round ${event.roundNumber} started."
                is GameEvent.MatchStarted -> "Match started between ${event.player1Id} and ${event.player2Id}."
                is GameEvent.EnergyGained -> "${event.characterId} gained ${event.amount} energy."
                is GameEvent.EnergyReset -> "${event.characterId} used all energy."
                is GameEvent.MatchEnded -> "Match ended! Winner: ${event.winnerId}."
                else -> event.toString()
            }
        }
    }

    fun deepCopy(): Match {
        return Match(
            id = id,
            players = players.map { it.deepCopy() },
            activePlayerIndex = activePlayerIndex,
            roundNumber = roundNumber,
            phase = phase,
            firstPlayerIndex = firstPlayerIndex,
            eventLog = eventLog.toMutableList()
        )
    }
}
