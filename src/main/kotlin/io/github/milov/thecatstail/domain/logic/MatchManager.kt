package io.github.milov.thecatstail.domain.logic

import io.github.milov.thecatstail.domain.model.*
import io.github.milov.thecatstail.domain.model.Phase

object MatchManager {
    private const val INITIAL_HAND_SIZE = 5
    private const val DRAW_PER_ROUND = 2

    fun initMatch(match: Match) {
        // Initial draw of 5 cards for each player
        match.players.forEach { player ->
            repeat(INITIAL_HAND_SIZE) {
                if (player.deck.isNotEmpty()) {
                    player.hand.add(player.deck.removeAt(0))
                }
            }
            match.mulliganDone[player.userId] = false
        }
    }

    /**
     * Mulligan: the given card indices in hand are returned to the bottom of the deck,
     * and an equal number of new cards are drawn from the top.
     */
    fun performMulligan(match: Match, userId: String, cardIndices: List<Int>) {
        val player = match.players.find { it.userId == userId } ?: return
        if (match.mulliganDone[userId] == true) return

        val toReplace = cardIndices.distinct().sortedDescending().filter { it in player.hand.indices }
        val removed = mutableListOf<ActionCard>()
        for (idx in toReplace) {
            removed.add(player.hand.removeAt(idx))
        }
        // Put back on bottom
        player.deck.addAll(removed)
        // Draw replacements
        repeat(removed.size) {
            if (player.deck.isNotEmpty()) {
                player.hand.add(player.deck.removeAt(0))
            }
        }
        match.mulliganDone[userId] = true

        // If all players are done with mulligan, proceed to first round roll phase
        if (match.players.all { match.mulliganDone[it.userId] == true }) {
            match.roundNumber = 1
            startRound(match)
        }
    }

    fun confirmDice(match: Match, userId: String) {
        if (match.phase != Phase.ROLL) return
        val player = match.players.find { it.userId == userId } ?: return
        val rolled = match.rolledDice[userId] ?: return
        // Flush rolled dice into pool
        player.dicePool.clear()
        for (el in rolled) {
            player.dicePool[el] = (player.dicePool[el] ?: 0) + 1
        }
        match.rolledDice.remove(userId)

        // If both players have confirmed, transition to ACTION
        if (match.rolledDice.isEmpty()) {
            match.phase = Phase.ACTION
            match.activePlayerIndex = match.firstPlayerIndex
        }
    }

    fun rerollDice(match: Match, userId: String, indices: List<Int>) {
        if (match.phase != Phase.ROLL) return
        val current = match.rolledDice[userId] ?: return
        val newDice = DiceEngine.reroll(current, indices).toMutableList()
        match.rolledDice[userId] = newDice
        match.applyEvent(GameEvent.DiceRolled(userId, newDice))
    }

    fun applyMove(match: Match, userId: String, move: Move) {
        val player = match.players.find { it.userId == userId } ?: return
        if (player.isFinishedActions && move !is Move.SwitchCharacter) return

        // Forced switch check
        if (match.isForcedSwitchRequired(userId) && move !is Move.SwitchCharacter) {
            return
        }

        if (match.winner != null) return

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

        // Reaction side-effects
        applyReactionSideEffects(match, player, activeChar, defender, result)
        
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
        
        // Win condition check after skill damage
        if (checkAndApplyWinCondition(match)) return

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

        // 2. Apply effect via registry. Unregistered cards are still consumed but
        //    produce no effect (logged via event).
        val applied = CardEffectRegistry.apply(match, player, card)
        if (!applied) {
            // Fallback: legacy support card with embedded template
            if (card.type == ActionCardType.SUPPORT) {
                card.supportEffect?.let { supportTemplate ->
                    player.supportZone.add(supportTemplate.deepCopy())
                    match.applyEvent(GameEvent.SupportCardPlayed(player.userId, card.name))
                }
            }
            // No-op for unregistered event/equipment; card is still consumed.
        }

        // 3. Remove from hand
        player.hand.remove(card)

        // 4. Combat action (GCG_TAG_SLOWLY) ends the turn; fast actions do not.
        if (card.isCombatAction) {
            switchActivePlayer(match)
        }
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

            // 2. Process Summons via SummonEffectRegistry
            val it = player.summonsZone.iterator()
            while (it.hasNext()) {
                val summon = it.next()
                match.applyEvent(GameEvent.SummonTriggered(player.userId, summon.name))

                val effect = SummonEffectRegistry.get(summon.id)
                applySummonEffect(match, player, opponent, summon, effect)

                summon.usages -= 1
                if (summon.usages <= 0) {
                    match.applyEvent(GameEvent.SummonExpired(player.userId, summon.name))
                    it.remove()
                }

                if (checkAndApplyWinCondition(match)) return
            }
        }
    }

    /**
     * Applies the reaction side-effects carried in [DamageResult] after a
     * hit: Overloaded forced switch, Frozen status, Crystallize shield,
     * Burning/Bloom summon creation.
     */
    private fun applyReactionSideEffects(
        match: Match,
        attackerPlayer: PlayerState,
        attacker: Character?,
        defender: Character,
        result: DamageResult
    ) {
        // Frozen — apply FROZEN status to the defender
        if (result.createFrozenStatus && defender.isAlive) {
            val existing = defender.activeStatuses.find { it.id == "FROZEN" }
            if (existing == null) {
                defender.activeStatuses.add(
                    CharacterStatus(
                        id = "FROZEN",
                        name = "Frozen",
                        durationRounds = 1,
                        preventsAction = true,
                        shatterElements = listOf(Element.PYRO, Element.UNALIGNED),
                        description = "Prevents action; shatters on Pyro/Physical."
                    )
                )
                match.applyEvent(GameEvent.StatusApplied(defender.name, "Frozen"))
            }
        }

        // Crystallize — create shield status on the attacker
        if (result.createCrystallizeShield && attacker != null && attacker.isAlive) {
            val shield = attacker.activeStatuses.find { it.id == "CRYSTALLIZE_SHIELD" }
            if (shield == null) {
                attacker.activeStatuses.add(
                    CharacterStatus(
                        id = "CRYSTALLIZE_SHIELD",
                        name = "Crystallize Shield",
                        shieldValue = 1,
                        description = "Absorbs 1 damage."
                    )
                )
                match.applyEvent(GameEvent.StatusApplied(attacker.name, "Crystallize Shield"))
            } else {
                shield.shieldValue += 1
            }
        }

        // Burning — create Burning Flame summon on attacker's side
        if (result.createBurningSummon) {
            val existing = attackerPlayer.summonsZone.find { it.id == "BURNING_FLAME" }
            if (existing == null) {
                attackerPlayer.summonsZone.add(
                    Summon(
                        id = "BURNING_FLAME",
                        name = "Burning Flame",
                        element = Element.PYRO,
                        baseDamage = 1,
                        usages = 1,
                        description = "End Phase: Deal 1 Pyro DMG."
                    )
                )
                match.applyEvent(GameEvent.SummonCreated(attackerPlayer.userId, "Burning Flame"))
            } else {
                existing.usages = minOf(2, existing.usages + 1)
            }
        }

        // Bloom — create Bountiful Core summon on attacker's side
        if (result.createBloomSummon) {
            val existing = attackerPlayer.summonsZone.find { it.id == "BOUNTIFUL_CORE" }
            if (existing == null) {
                attackerPlayer.summonsZone.add(
                    Summon(
                        id = "BOUNTIFUL_CORE",
                        name = "Bountiful Core",
                        element = Element.DENDRO,
                        baseDamage = 2,
                        usages = 1,
                        description = "End Phase: Deal 2 Dendro DMG."
                    )
                )
                match.applyEvent(GameEvent.SummonCreated(attackerPlayer.userId, "Bountiful Core"))
            }
        }

        // Overloaded — force opponent to switch. The game loop already honours
        // `isForcedSwitchRequired` when the opponent's active character is dead;
        // here we simulate the forced-switch by marking the opponent's current
        // active as requiring switch via setting a transient status flag. For
        // simplicity in this prototype, immediately rotate the opponent's
        // active character to the next living one.
        if (result.forceOpponentSwitch) {
            val opponent = match.players.find { p -> p.characters.any { it === defender } }
            if (opponent != null) {
                val curIdx = opponent.activeCharacterIndex
                val n = opponent.characters.size
                for (offset in 1 until n) {
                    val next = (curIdx + offset) % n
                    if (opponent.characters[next].isAlive) {
                        opponent.activeCharacterIndex = next
                        match.applyEvent(
                            GameEvent.CharacterSwitched(
                                opponent.userId,
                                curIdx,
                                next,
                                opponent.characters[curIdx].name,
                                opponent.characters[next].name
                            )
                        )
                        break
                    }
                }
            }
        }

        // Win condition after all derived deaths (Electro-Charged pierce, etc.)
        checkAndApplyWinCondition(match)
    }

    private fun applySummonEffect(
        match: Match,
        player: PlayerState,
        opponent: PlayerState,
        summon: Summon,
        effect: SummonEffect
    ) {
        when (effect) {
            is SummonEffect.DamageEffect -> {
                val defender = opponent.getActiveCharacter()
                if (defender.isAlive) {
                    val dmg = DamageInfo(summon.id, defender.id, effect.amount, effect.element, isSkill = false)
                    val result = CombatEngine.applyDamage(null, defender, dmg, match)
                    match.applyEvent(GameEvent.DamageDealt(defender.name, result.finalDamage, result.reaction, result.isDead))
                    applyReactionSideEffects(match, player, null, defender, result)
                }
            }
            is SummonEffect.HealEffect -> {
                val active = player.getActiveCharacter()
                active.currentHp = minOf(active.maxHp, active.currentHp + effect.amount)
            }
            is SummonEffect.MultiTargetEffect -> {
                for (target in opponent.characters) {
                    if (!target.isAlive) continue
                    val dmg = DamageInfo(summon.id, target.id, effect.amount, effect.element, isSkill = false)
                    val result = CombatEngine.applyDamage(null, target, dmg, match)
                    match.applyEvent(GameEvent.DamageDealt(target.name, result.finalDamage, result.reaction, result.isDead))
                    applyReactionSideEffects(match, player, null, target, result)
                }
            }
            is SummonEffect.DamageAndHeal -> {
                val defender = opponent.getActiveCharacter()
                if (defender.isAlive) {
                    val dmg = DamageInfo(summon.id, defender.id, effect.damage, effect.element, isSkill = false)
                    val result = CombatEngine.applyDamage(null, defender, dmg, match)
                    match.applyEvent(GameEvent.DamageDealt(defender.name, result.finalDamage, result.reaction, result.isDead))
                    applyReactionSideEffects(match, player, null, defender, result)
                }
                val ally = player.getActiveCharacter()
                if (ally.isAlive) {
                    ally.currentHp = minOf(ally.maxHp, ally.currentHp + effect.heal)
                }
            }
        }
    }

    fun startRound(match: Match) {
        match.applyEvent(GameEvent.RoundStarted(match.roundNumber))
        match.phase = Phase.ROLL
        match.rolledDice.clear()
        match.players.forEach { player ->
            player.dicePool.clear()
            // Draw per-round cards
            repeat(DRAW_PER_ROUND) {
                if (player.deck.isNotEmpty()) {
                    player.hand.add(player.deck.removeAt(0))
                }
            }
            player.isFinishedActions = false
            val rolled = DiceEngine.roll(8).toMutableList()
            match.rolledDice[player.userId] = rolled
            match.applyEvent(GameEvent.DiceRolled(player.userId, rolled))
        }
    }

    /**
     * Checks if any player has all characters dead. If so, declares the opposing player
     * as the winner, sets match.winner, emits GameEvent.MatchEnded, and returns true.
     */
    fun checkAndApplyWinCondition(match: Match): Boolean {
        if (match.winner != null) return true
        val p1Dead = match.players[0].characters.all { !it.isAlive }
        val p2Dead = match.players[1].characters.all { !it.isAlive }
        if (!p1Dead && !p2Dead) return false
        val winnerId = when {
            p1Dead && p2Dead -> match.players[match.firstPlayerIndex].userId
            p1Dead -> match.players[1].userId
            else -> match.players[0].userId
        }
        match.winner = winnerId
        match.applyEvent(GameEvent.MatchEnded(winnerId))
        return true
    }

    fun endActionPhase(match: Match) {
        match.applyEvent(GameEvent.RoundEnded(match.roundNumber))
        match.phase = Phase.END

        processEndPhase(match)

        if (match.winner != null) return

        match.roundNumber += 1
        match.activePlayerIndex = match.firstPlayerIndex
        startRound(match)
    }

    private fun handleSwitchCharacter(match: Match, player: PlayerState, targetIndex: Int) {
        val oldIndex = player.activeCharacterIndex
        val fromName = player.characters[oldIndex].name
        val toName = player.characters[targetIndex].name
        
        val isForced = match.isForcedSwitchRequired(player.userId)
        
        if (!isForced) {
            payCost(player.dicePool, listOf(DiceCost(Element.UNALIGNED, 1)))
        }
        
        player.activeCharacterIndex = targetIndex
        match.applyEvent(GameEvent.CharacterSwitched(player.userId, oldIndex, targetIndex, fromName, toName))

        if (checkAndApplyWinCondition(match)) return

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
