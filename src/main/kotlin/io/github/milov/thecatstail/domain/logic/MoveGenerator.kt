package io.github.milov.thecatstail.domain.logic

import io.github.milov.thecatstail.domain.model.*
import io.github.milov.thecatstail.domain.model.Phase

object MoveGenerator {
    fun getLegalMoves(match: Match): List<Move> {
        // Auto-fast-forward the ROLL phase during MCTS simulation so that
        // simulated playouts never stall waiting for dice confirmation.
        if (match.phase == Phase.ROLL) {
            for (p in match.players) {
                MatchManager.confirmDice(match, p.userId)
            }
        }
        if (match.phase != Phase.ACTION) {
            return emptyList()
        }

        val player = match.getActivePlayer()
        if (player.isFinishedActions) {
            // If the active player is already finished, they shouldn't be able to do anything.
            // But we must return a move to allow the simulation to progress.
            return listOf(Move.DeclareEnd)
        }

        val moves = mutableListOf<Move>()
        
        // 0. Forced Switch Check
        if (match.isForcedSwitchRequired(player.userId)) {
            for (i in player.characters.indices) {
                if (player.characters[i].isAlive) {
                    moves.add(Move.SwitchCharacter(i))
                }
            }
            return moves
        }

        // 1. Skills
        val activeChar = player.getActiveCharacter()
        if (activeChar.isAlive) {
            for (skill in activeChar.skills) {
                // Passives are never playable
                if (skill.type == SkillType.PASSIVE) continue
                // Check cost
                if (CostValidator.canPay(player.dicePool, skill.cost)) {
                    // Check energy for Burst
                    if (skill.type == SkillType.ELEMENTAL_BURST) {
                        if (activeChar.currentEnergy >= activeChar.maxEnergy) {
                            moves.add(Move.UseSkill(skill.id))
                        }
                    } else {
                        moves.add(Move.UseSkill(skill.id))
                    }
                }
            }
        }

        // 2. Switch
        for (i in player.characters.indices) {
            if (i != player.activeCharacterIndex && player.characters[i].isAlive) {
                // Cost is 1 unaligned die (simplified)
                if (CostValidator.canPay(player.dicePool, listOf(DiceCost(Element.UNALIGNED, 1)))) {
                    moves.add(Move.SwitchCharacter(i))
                }
            }
        }

        // 3. Play Cards — only cards with a registered effect are considered legal.
        for (card in player.hand) {
            if (!CardEffectRegistry.isRegistered(card.id)) continue
            if (CostValidator.canPay(player.dicePool, card.cost)) {
                moves.add(Move.PlayCard(card.id))
            }
        }

        // 4. Tuning -- capped at max 1 move per card. Heuristic: pick the non-OMNI die
        //    element with the largest count that is NOT the active character's element.
        //    Falls back to any non-OMNI element if only the active element has dice.
        val activeElement = activeChar.element
        val tuningCandidate: Element? = player.dicePool.entries
            .filter { it.key != Element.OMNI && it.key != activeElement && it.value > 0 }
            .maxByOrNull { it.value }
            ?.key
            ?: player.dicePool.entries
                .filter { it.key != Element.OMNI && it.value > 0 }
                .maxByOrNull { it.value }
                ?.key
        if (tuningCandidate != null) {
            for (card in player.hand) {
                moves.add(Move.ElementalTuning(card.id, tuningCandidate))
            }
        }

        // 5. Declare End
        moves.add(Move.DeclareEnd)

        return moves
    }
}
