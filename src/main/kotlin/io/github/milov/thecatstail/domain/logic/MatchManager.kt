package io.github.milov.thecatstail.domain.logic

import io.github.milov.thecatstail.domain.model.*
import io.github.milov.thecatstail.domain.model.Phase

object MatchManager {
    fun applyMove(match: Match, userId: String, move: Move) {
        val player = match.players.find { it.userId == userId } ?: return
        if (player.isFinishedActions && move !is Move.SwitchCharacter) return

        // Forced switch check
        if (match.isForcedSwitchRequired(userId) && move !is Move.SwitchCharacter) {
            return
        }

        when (move) {
            is Move.UseSkill -> handleUseSkill(match, player, move.skillId)
            is Move.SwitchCharacter -> handleSwitchCharacter(match, player, move.targetIndex)
            is Move.DeclareEnd -> declareEnd(match, userId)
            else -> {}
        }
    }

    private fun handleUseSkill(match: Match, player: PlayerState, skillId: String) {
        val activeChar = player.getActiveCharacter()
        val skill = activeChar.skills.find { it.id == skillId } ?: return
        
        match.applyEvent(GameEvent.SkillUsed(player.userId, activeChar.name, skill.id, skill.name))
        
        // 1. Pay cost
        payCost(player.dicePool, skill.cost)
        
        // 2. Apply damage
        val defender = match.players.find { it.userId != player.userId }?.getActiveCharacter() ?: return
        val damageInfo = DamageInfo(activeChar.id, defender.id, skill.baseDamage, skill.element)
        val result = CombatEngine.applyDamage(activeChar, defender, damageInfo, match)
        
        match.applyEvent(GameEvent.DamageDealt(defender.name, result.finalDamage, result.reaction, result.isDead))
        
        // 3. Generate Energy
        if (skill.type != SkillType.ELEMENTAL_BURST) {
            activeChar.currentEnergy = minOf(activeChar.currentEnergy + skill.energyGenerated, activeChar.maxEnergy)
            match.applyEvent(GameEvent.EnergyGained(activeChar.name, skill.energyGenerated))
        } else {
            activeChar.currentEnergy = 0
            match.applyEvent(GameEvent.EnergyReset(activeChar.name))
        }
        
        // 4. Combat action ends turn IF not forced switch on opponent
        if (!match.isForcedSwitchRequired(match.getInactivePlayer().userId)) {
            switchActivePlayer(match)
        } else {
            // Wait for opponent to switch
            match.activePlayerIndex = match.players.indexOfFirst { it.userId == match.getInactivePlayer().userId }
        }
    }

    private fun handleSwitchCharacter(match: Match, player: PlayerState, targetIndex: Int) {
        val oldIndex = player.activeCharacterIndex
        
        val isForced = match.isForcedSwitchRequired(player.userId)
        
        if (!isForced) {
            payCost(player.dicePool, listOf(DiceCost(Element.UNALIGNED, 1)))
        }
        
        player.activeCharacterIndex = targetIndex
        match.applyEvent(GameEvent.CharacterSwitched(player.userId, oldIndex, targetIndex))
        
        switchActivePlayer(match)
    }

    private fun payCost(dicePool: MutableMap<Element, Int>, cost: List<DiceCost>) {
        for (c in cost) {
            var remaining = c.amount
            if (c.element != Element.UNALIGNED) {
                // Use specific
                val available = dicePool.getOrDefault(c.element, 0)
                val used = minOf(available, remaining)
                dicePool[c.element] = available - used
                remaining -= used
                
                // Use Omni
                if (remaining > 0) {
                    val omni = dicePool.getOrDefault(Element.OMNI, 0)
                    val omniUsed = minOf(omni, remaining)
                    dicePool[Element.OMNI] = omni - omniUsed
                    remaining -= omniUsed
                }
            } else {
                // Use non-omni first
                val elements = dicePool.keys.filter { it != Element.OMNI }
                for (el in elements) {
                    val available = dicePool.getOrDefault(el, 0)
                    val used = minOf(available, remaining)
                    dicePool[el] = available - used
                    remaining -= used
                    if (remaining == 0) break
                }
                // Use Omni
                if (remaining > 0) {
                    val omni = dicePool.getOrDefault(Element.OMNI, 0)
                    val omniUsed = minOf(omni, remaining)
                    dicePool[Element.OMNI] = omni - omniUsed
                }
            }
        }
    }

    private fun switchActivePlayer(match: Match) {
        println("Switching active player. Current: ${match.activePlayerIndex}, Finished: ${match.players.map { it.isFinishedActions }}")
        // If both finished, end Action Phase
        if (match.players.all { it.isFinishedActions }) {
            println("Both players finished. Ending Action Phase.")
            endActionPhase(match)
            return
        }

        val otherIndex = (match.activePlayerIndex + 1) % 2
        // Only switch if the other player is NOT finished
        if (!match.players[otherIndex].isFinishedActions) {
            match.activePlayerIndex = otherIndex
            println("Switched to player index: ${match.activePlayerIndex} (${match.players[match.activePlayerIndex].userId})")
        } else {
            println("Other player finished. Keeping player index: ${match.activePlayerIndex}")
            // Keep current player active because the other is finished
            // and we know not all are finished from the check above
        }
    }

    fun startRound(match: Match) {
        match.applyEvent(GameEvent.RoundStarted(match.roundNumber))
        match.phase = Phase.ACTION 
        match.players.forEach { player ->
            player.dicePool.clear()
            player.dicePool[Element.OMNI] = 8
            player.isFinishedActions = false
            match.applyEvent(GameEvent.DiceRolled(player.userId, List(8) { Element.OMNI }))
        }
    }

    fun endActionPhase(match: Match) {
        match.applyEvent(GameEvent.RoundEnded(match.roundNumber))
        match.phase = Phase.END
        match.roundNumber += 1
        match.activePlayerIndex = match.firstPlayerIndex
        startRound(match)
    }

    fun declareEnd(match: Match, userId: String) {
        val player = match.players.find { it.userId == userId } ?: return
        if (player.isFinishedActions) return
        
        player.isFinishedActions = true
        match.applyEvent(GameEvent.PlayerDeclaredEnd(userId))
        
        if (match.players.none { it.isFinishedActions && it.userId != userId }) {
            match.firstPlayerIndex = match.players.indexOf(player)
        }
        
        if (match.players.all { it.isFinishedActions }) {
            endActionPhase(match)
        } else {
            switchActivePlayer(match)
        }
    }
}
