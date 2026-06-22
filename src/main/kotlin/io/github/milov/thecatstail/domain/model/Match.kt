package io.github.milov.thecatstail.domain.model

import io.github.milov.thecatstail.domain.base.DomainEntity

class Match(
    id: String,
    val players: List<PlayerState>,
    var activePlayerIndex: Int = 0,
    var roundNumber: Int = 1,
    var phase: Phase = Phase.ROLL,
    var firstPlayerIndex: Int = 0,
    val eventLog: MutableList<GameEvent> = mutableListOf(),
    var winner: String? = null,
    val rolledDice: MutableMap<String, MutableList<Element>> = mutableMapOf(),
    var mulliganDone: MutableMap<String, Boolean> = mutableMapOf()
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

    fun generateTextLog(): List<String> = generateLogEntries().map { it.text }

    fun generateLogEntries(): List<LogEntry> {
        return eventLog.map { event ->
            when (event) {
                is GameEvent.SkillUsed -> LogEntry(
                    "${event.userId}'s ${event.characterId} used ${event.skillName}!",
                    "skill"
                )
                is GameEvent.DamageDealt -> {
                    val reactionPart = event.reaction?.let { " ($it!)" } ?: ""
                    val swirlPart = if (event.reaction == "Swirl") " (All benched characters took 1 Piercing DMG)" else ""
                    val category = if (event.reaction != null) "reaction" else "damage"
                    LogEntry("Dealt ${event.amount} damage to ${event.targetCharacterId}$reactionPart.$swirlPart", category)
                }
                is GameEvent.CharacterSwitched -> LogEntry("${event.userId} switched to ${event.toName}.", "switch")
                is GameEvent.PlayerDeclaredEnd -> LogEntry("${event.userId} declared end of round.", "info")
                is GameEvent.RoundStarted -> LogEntry("Round ${event.roundNumber} started.", "info")
                is GameEvent.MatchStarted -> LogEntry("Match started between ${event.player1Id} and ${event.player2Id}.", "info")
                is GameEvent.EnergyGained -> LogEntry("${event.characterId} gained ${event.amount} energy.", "energy")
                is GameEvent.EnergyReset -> LogEntry("${event.characterId} used all energy.", "energy")
                is GameEvent.MatchEnded -> LogEntry("Match ended! Winner: ${event.winnerId}.", "info")
                is GameEvent.SummonCreated -> LogEntry("${event.userId} summoned ${event.summonName}!", "energy")
                is GameEvent.SummonTriggered -> LogEntry("${event.userId}'s ${event.summonName} triggered.", "info")
                is GameEvent.SummonExpired -> LogEntry("${event.userId}'s ${event.summonName} expired.", "info")
                is GameEvent.SupportCardPlayed -> LogEntry("${event.userId} played ${event.cardName}.", "energy")
                is GameEvent.TuningDone -> LogEntry("${event.userId} tuned ${event.cardName} to ${event.targetElement}.", "info")
                is GameEvent.StatusApplied -> LogEntry("${event.characterId} gained ${event.statusName}.", "energy")
                is GameEvent.StatusExpired -> LogEntry("${event.characterId}'s ${event.statusName} expired.", "info")
                else -> LogEntry(event.toString(), "info")
            }
        }
    }

    data class LogEntry(val text: String, val category: String)

    fun deepCopy(forMcts: Boolean = false): Match {
        return Match(
            id = id,
            players = players.map { it.deepCopy() },
            activePlayerIndex = activePlayerIndex,
            roundNumber = roundNumber,
            phase = phase,
            firstPlayerIndex = firstPlayerIndex,
            eventLog = if (forMcts) mutableListOf() else eventLog.toMutableList(),
            winner = winner,
            rolledDice = rolledDice.mapValues { it.value.toMutableList() }.toMutableMap(),
            mulliganDone = mulliganDone.toMutableMap()
        )
    }
}
