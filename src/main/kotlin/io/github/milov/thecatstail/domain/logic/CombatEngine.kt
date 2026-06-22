package io.github.milov.thecatstail.domain.logic

import io.github.milov.thecatstail.domain.model.Character
import io.github.milov.thecatstail.domain.model.Element
import io.github.milov.thecatstail.domain.model.Match
import io.github.milov.thecatstail.domain.model.SkillType

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
    val swirledElement: Element? = null,
    val allDeaths: List<String> = emptyList(),
    val forceOpponentSwitch: Boolean = false,
    val createFrozenStatus: Boolean = false,
    val createCrystallizeShield: Boolean = false,
    val createBurningSummon: Boolean = false,
    val createBloomSummon: Boolean = false
)

data class DamageSimulation(
    val targetDamage: Map<String, Int>, // characterId -> damage
    val reactions: Map<String, String>, // characterId -> reaction name
    val swirledElement: Element? = null,
    val canAfford: Boolean = true,
    val predictedElements: Map<String, List<Element>> = emptyMap(), // what elements will be on character after
    val predictedReactionElements: Map<String, List<Element>> = emptyMap() // which elements will be cleared
)

object CombatEngine {
    fun simulateDamage(attacker: Character, defender: Character, match: Match, skillId: String): DamageSimulation {
        val skill = attacker.skills.find { it.id == skillId } ?: return DamageSimulation(emptyMap(), emptyMap())
        
        var finalElement = skill.element
        var mainDamage = skill.baseDamage
        
        if (skill.type == SkillType.NORMAL_ATTACK) {
            attacker.activeStatuses.forEach { status ->
                status.elementOverride?.let { finalElement = it }
                mainDamage += status.damageBonus
            }
        }

        val reaction = ReactionManager.calculateReaction(defender.appliedElements, finalElement)
        val targetDamage = mutableMapOf<String, Int>()
        val reactions = mutableMapOf<String, String>()
        val predictedElements = mutableMapOf<String, List<Element>>()
        val predictedReactionElements = mutableMapOf<String, List<Element>>()
        
        var swirled: Element? = null
        
        if (reaction != null) {
            mainDamage += reaction.bonusDamage
            reactions[defender.id] = reaction.name
            predictedReactionElements[defender.id] = defender.appliedElements.toList()
            predictedElements[defender.id] = emptyList() // Reaction clears elements
            
            if (reaction.isPiercing) {
                val opponent = match.players.find { it.characters.contains(defender) }
                opponent?.characters?.forEach { 
                    if (it != defender && it.isAlive) {
                        targetDamage[it.id] = 1
                    }
                }
            }
            
            if (reaction.isSwirl) {
                swirled = reaction.swirledElement
                val opponent = match.players.find { it.characters.contains(defender) }
                opponent?.characters?.forEach { 
                    if (it != defender && it.isAlive) {
                        targetDamage[it.id] = (targetDamage[it.id] ?: 0) + 1
                        if (swirled != null && swirled !in it.appliedElements) {
                            predictedElements[it.id] = it.appliedElements + swirled
                        }
                    }
                }
            }
        } else {
            // No reaction
            if (finalElement != Element.UNALIGNED && finalElement != Element.OMNI) {
                if (finalElement !in defender.appliedElements) {
                    predictedElements[defender.id] = defender.appliedElements + finalElement
                }
            }
        }
        
        targetDamage[defender.id] = mainDamage
        
        return DamageSimulation(targetDamage, reactions, swirled, predictedElements = predictedElements, predictedReactionElements = predictedReactionElements)
    }

    fun applyDamage(attacker: Character?, defender: Character, damageInfo: DamageInfo, match: Match? = null): DamageResult {
        // 1. Calculate Reaction
        val reaction = ReactionManager.calculateReaction(defender.appliedElements, damageInfo.element)

        var totalDamage = damageInfo.baseDamage
        var reactionName: String? = null
        var piercingDamage = 0
        var swirledElement: Element? = null
        val allDeaths = mutableListOf<String>()

        if (reaction != null) {
            totalDamage += reaction.bonusDamage
            reactionName = reaction.name
            if (reaction.isPiercing && match != null) {
                piercingDamage = 1
                // Apply actual bench piercing damage (Electro-Charged)
                val defenderPlayer = match.players.find { it.characters.contains(defender) }
                if (defenderPlayer != null) {
                    defenderPlayer.characters.forEach { benchedChar ->
                        if (benchedChar != defender && benchedChar.isAlive) {
                            benchedChar.currentHp -= 1
                            if (benchedChar.currentHp <= 0) {
                                benchedChar.currentHp = 0
                                benchedChar.isAlive = false
                                allDeaths.add(benchedChar.id)
                            }
                        }
                    }
                }
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
                                allDeaths.add(benchedChar.id)
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
        
        // 2. Apply shield absorption via StatusEngine
        totalDamage = StatusEngine.applyShieldAbsorption(defender, totalDamage)

        // 3. Subtract HP
        defender.currentHp -= totalDamage
        if (defender.currentHp <= 0) {
            defender.currentHp = 0
            defender.isAlive = false
            if (defender.id !in allDeaths) allDeaths.add(defender.id)
        }

        return DamageResult(
            finalDamage = totalDamage,
            reaction = reactionName,
            isDead = !defender.isAlive,
            piercingDamage = piercingDamage,
            swirledElement = swirledElement,
            allDeaths = allDeaths.toList(),
            forceOpponentSwitch = reaction?.forceOpponentSwitch ?: false,
            createFrozenStatus = reaction?.isFrozen ?: false,
            createCrystallizeShield = reaction?.createsCrystallizeShield ?: false,
            createBurningSummon = reaction?.createsBurningSummon ?: false,
            createBloomSummon = reaction?.createsBloomSummon ?: false
        )
    }
}
