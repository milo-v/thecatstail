package io.github.milov.thecatstail.domain.logic

import io.github.milov.thecatstail.domain.model.Character
import io.github.milov.thecatstail.domain.model.Element
import io.github.milov.thecatstail.domain.model.Match

data class DamageInfo(
    val attackerId: String,
    val defenderId: String,
    val baseDamage: Int,
    val element: Element,
    val isSkill: Boolean = true
)

data class DamageResult(
    val finalDamage: Int,
    val reaction: String? = null,
    val isDead: Boolean = false,
    val piercingDamage: Int = 0,
    val swirledElement: Element? = null
)

object CombatEngine {
    fun applyDamage(attacker: Character, defender: Character, damageInfo: DamageInfo, match: Match? = null): DamageResult {
        // 1. Calculate Reaction
        val reaction = ReactionManager.calculateReaction(defender.appliedElements, damageInfo.element)
        
        var totalDamage = damageInfo.baseDamage
        var reactionName: String? = null
        var piercingDamage = 0
        var swirledElement: Element? = null
        
        if (reaction != null) {
            totalDamage += reaction.bonusDamage
            reactionName = reaction.name
            if (reaction.isPiercing) {
                piercingDamage = 1 
            }
            if (reaction.isSwirl && match != null) {
                swirledElement = reaction.swirledElement
                // Apply Swirl effect: 1 damage and element to bench
                val defenderPlayer = match.players.find { it.characters.contains(defender) }
                if (defenderPlayer != null) {
                    defenderPlayer.characters.forEach { benchedChar ->
                        if (benchedChar != defender && benchedChar.isAlive) {
                            // 1 Piercing damage
                            benchedChar.currentHp -= 1
                            if (benchedChar.currentHp <= 0) {
                                benchedChar.currentHp = 0
                                benchedChar.isAlive = false
                            }
                            // Apply swirled element
                            if (swirledElement != null && swirledElement !in benchedChar.appliedElements) {
                                benchedChar.appliedElements.add(swirledElement)
                            }
                        }
                    }
                }
            }
            // Clear current elements if a reaction happened (simplification)
            defender.appliedElements.clear()
        } else if (damageInfo.element != Element.UNALIGNED && damageInfo.element != Element.OMNI) {
            // Apply element if no reaction
            if (damageInfo.element !in defender.appliedElements) {
                defender.appliedElements.add(damageInfo.element)
            }
        }
        
        // 2. Subtract HP
        defender.currentHp -= totalDamage
        if (defender.currentHp <= 0) {
            defender.currentHp = 0
            defender.isAlive = false
        }
        
        return DamageResult(
            finalDamage = totalDamage,
            reaction = reactionName,
            isDead = !defender.isAlive,
            piercingDamage = piercingDamage,
            swirledElement = swirledElement
        )
    }
}
