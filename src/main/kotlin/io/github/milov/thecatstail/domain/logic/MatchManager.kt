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
            is Move.PlayCard -> handlePlayCard(match, player, move.cardId)
            is Move.ElementalTuning -> handleTuning(match, player, move.cardId, move.targetDieElement)
            is Move.DeclareEnd -> declareEnd(match, userId)
        }
    }

    private fun handleUseSkill(match: Match, player: PlayerState, skillId: String) {
        val activeChar = player.getActiveCharacter()
        val skill = activeChar.skills.find { it.id == skillId } ?: return
        
        match.applyEvent(GameEvent.SkillUsed(player.userId, activeChar.name, skill.id, skill.name))
        
        // 1. Pay cost
        payCost(player.dicePool, skill.cost)
        
        // 2. Apply damage
        val defender = match.getInactivePlayer().getActiveCharacter()
        
        // Elemental Override & Bonus Damage from Status
        var finalElement = skill.element
        var extraDamage = 0
        if (skill.type == SkillType.NORMAL_ATTACK) {
            activeChar.activeStatuses.forEach { status ->
                status.elementOverride?.let { finalElement = it }
                extraDamage += status.damageBonus
            }
        }
        
        val damageInfo = DamageInfo(activeChar.id, defender.id, skill.baseDamage + extraDamage, finalElement)
        val result = CombatEngine.applyDamage(activeChar, defender, damageInfo, match)
        
        match.applyEvent(GameEvent.DamageDealt(defender.name, result.finalDamage, result.reaction, result.isDead))
        
        // 3. Summon Effect
        skill.summonEffect?.let { summonTemplate ->
            val existing = player.summonsZone.find { it.id == summonTemplate.id }
            if (existing != null) {
                existing.usages = summonTemplate.usages // Reset usages or stack? Let's reset for now
            } else {
                player.summonsZone.add(summonTemplate.deepCopy())
                match.applyEvent(GameEvent.SummonCreated(player.userId, summonTemplate.name))
            }
        }

        // 4. Status Effect
        skill.applyStatus?.let { statusTemplate ->
            val existing = activeChar.activeStatuses.find { it.id == statusTemplate.id }
            if (existing != null) {
                existing.durationRounds = statusTemplate.durationRounds
                existing.usages = statusTemplate.usages
            } else {
                activeChar.activeStatuses.add(statusTemplate.deepCopy())
                match.applyEvent(GameEvent.StatusApplied(activeChar.name, statusTemplate.name))
            }
        }

        // 5. Generate Energy
        if (skill.type != SkillType.ELEMENTAL_BURST) {
            activeChar.currentEnergy = minOf(activeChar.currentEnergy + skill.energyGenerated, activeChar.maxEnergy)
            match.applyEvent(GameEvent.EnergyGained(activeChar.name, skill.energyGenerated))
        } else {
            activeChar.currentEnergy = 0
            match.applyEvent(GameEvent.EnergyReset(activeChar.name))
        }
        
        // 6. Combat action ends turn IF not forced switch on opponent
        if (!match.isForcedSwitchRequired(match.getInactivePlayer().userId)) {
            switchActivePlayer(match)
        } else {
            // Wait for opponent to switch
            match.activePlayerIndex = match.players.indexOfFirst { it.userId == match.getInactivePlayer().userId }
        }
    }

    private fun handlePlayCard(match: Match, player: PlayerState, cardId: String) {
        val card = player.hand.find { it.id == cardId } ?: return
        
        // 1. Pay cost
        payCost(player.dicePool, card.cost)
        
        // 2. Apply effect
        when (card.type) {
            ActionCardType.SUPPORT -> {
                card.supportEffect?.let { supportTemplate ->
                    player.supportZone.add(supportTemplate.deepCopy())
                    match.applyEvent(GameEvent.SupportCardPlayed(player.userId, card.name))
                }
            }
            ActionCardType.EVENT -> {
                // TODO: Event effects
            }
            ActionCardType.EQUIPMENT -> {
                // TODO: Equipment effects
            }
        }
        
        // 3. Remove from hand
        player.hand.remove(card)
        
        // 4. Playing a card is usually a fast action, so we don't switch player? 
        // In the original game, some are fast, some are combat. Let's assume fast for now.
    }

    private fun handleTuning(match: Match, player: PlayerState, cardId: String, targetDieElement: Element) {
        val card = player.hand.find { it.id == cardId } ?: return
        
        // 1. Remove 1 die of target element
        if (player.dicePool.getOrDefault(targetDieElement, 0) > 0) {
            player.dicePool[targetDieElement] = player.dicePool[targetDieElement]!! - 1
            
            // 2. Add 1 die of active character element
            val activeCharElement = player.getActiveCharacter().element
            player.dicePool[activeCharElement] = player.dicePool.getOrDefault(activeCharElement, 0) + 1
            
            // 3. Remove card from hand
            player.hand.remove(card)
            
            match.applyEvent(GameEvent.TuningDone(player.userId, card.name, activeCharElement))
        }
    }

    private fun processEndPhase(match: Match) {
        // Triggers summons for both players starting from first player
        val order = if (match.activePlayerIndex == 0) listOf(0, 1) else listOf(1, 0)
        
        for (idx in order) {
            val player = match.players[idx]
            val opponent = match.players[(idx + 1) % 2]
            
            // 1. Process Statuses expiration
            player.characters.forEach { char ->
                val statusIt = char.activeStatuses.iterator()
                while (statusIt.hasNext()) {
                    val status = statusIt.next()
                    status.durationRounds?.let {
                        val newDuration = it - 1
                        status.durationRounds = newDuration
                        if (newDuration <= 0) {
                            match.applyEvent(GameEvent.StatusExpired(char.name, status.name))
                            statusIt.remove()
                        }
                    }
                }
            }

            // 2. Process Summons
            val it = player.summonsZone.iterator()
            while (it.hasNext()) {
                val summon = it.next()
                match.applyEvent(GameEvent.SummonTriggered(player.userId, summon.name))
                
                // Deal damage
                val defender = opponent.getActiveCharacter()
                if (defender.isAlive) {
                    val damageInfo = DamageInfo(summon.id, defender.id, summon.baseDamage, summon.element, isSkill = false)
                    val result = CombatEngine.applyDamage(null, defender, damageInfo, match)
                    match.applyEvent(GameEvent.DamageDealt(defender.name, result.finalDamage, result.reaction, result.isDead))
                }
                
                summon.usages -= 1
                if (summon.usages <= 0) {
                    match.applyEvent(GameEvent.SummonExpired(player.userId, summon.name))
                    it.remove()
                }
            }
        }
    }

    fun startRound(match: Match) {
        match.applyEvent(GameEvent.RoundStarted(match.roundNumber))
        match.phase = Phase.ACTION 
        match.players.forEach { player ->
            player.dicePool.clear()
            // player.dicePool[Element.OMNI] = 8 // In real game, dice are rolled
            // For now, give 8 omni but also draw cards
            repeat(2) {
                if (player.deck.isNotEmpty()) {
                    player.hand.add(player.deck.removeAt(0))
                }
            }
            player.dicePool[Element.OMNI] = 8
            player.isFinishedActions = false
            match.applyEvent(GameEvent.DiceRolled(player.userId, List(8) { Element.OMNI }))
        }
    }

    fun endActionPhase(match: Match) {
        match.applyEvent(GameEvent.RoundEnded(match.roundNumber))
        match.phase = Phase.END
        
        processEndPhase(match)
        
        match.roundNumber += 1
        match.activePlayerIndex = match.firstPlayerIndex
        startRound(match)
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
        // If both finished, end Action Phase
        if (match.players.all { it.isFinishedActions }) {
            endActionPhase(match)
            return
        }

        val otherIndex = (match.activePlayerIndex + 1) % 2
        // Only switch if the other player is NOT finished
        if (!match.players[otherIndex].isFinishedActions) {
            match.activePlayerIndex = otherIndex
        }
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
