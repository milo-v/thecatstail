package io.github.milov.thecatstail.domain.logic

import io.github.milov.thecatstail.domain.model.*
import io.github.milov.thecatstail.domain.model.Phase

object MoveGenerator {
    fun getLegalMoves(match: Match): List<Move> {
        if (match.phase != Phase.ACTION) {
            // For simplicity, MCTS only works on Action Phase for now
            // Roll phase is automated or has simple logic
            return listOf(Move.DeclareEnd)
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

        // 3. Declare End
        moves.add(Move.DeclareEnd)

        return moves
    }
}
