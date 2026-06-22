package io.github.milov.thecatstail.domain.logic

import io.github.milov.thecatstail.domain.model.Character
import io.github.milov.thecatstail.domain.model.CharacterStatus
import io.github.milov.thecatstail.domain.model.Element

/**
 * Processes per-character status triggers (shield absorption, damage bonus,
 * element infusion, freeze shatter) using the [StatusEffectRegistry].
 */
object StatusEngine {

    /**
     * Applies any shield statuses on [defender] to [damage]. Returns the
     * remaining damage after absorption. Depleted shields are removed from
     * the character's status list.
     */
    fun applyShieldAbsorption(defender: Character, damage: Int): Int {
        if (damage <= 0) return 0
        var remaining = damage
        val toRemove = mutableListOf<CharacterStatus>()
        for (status in defender.activeStatuses) {
            if (remaining <= 0) break
            val effect = StatusEffectRegistry.get(status.id)
            // Direct shieldValue on status object takes precedence over registry
            if (status.shieldValue > 0) {
                val absorbed = minOf(status.shieldValue, remaining)
                status.shieldValue -= absorbed
                remaining -= absorbed
                if (status.shieldValue <= 0) toRemove.add(status)
                continue
            }
            if (effect is StatusEffect.ShieldEffect) {
                val absorbed = minOf(effect.absorption, remaining)
                remaining -= absorbed
                toRemove.add(status)
            }
        }
        defender.activeStatuses.removeAll(toRemove)
        return remaining
    }

    /**
     * Returns the additional damage that should be added to an attack
     * with [attackElement] made by [attacker], based on its active statuses.
     */
    fun damageBonusFor(attacker: Character, attackElement: Element): Int {
        var bonus = 0
        for (status in attacker.activeStatuses) {
            val effect = StatusEffectRegistry.get(status.id)
            if (effect is StatusEffect.DamageBonusEffect && effect.element == attackElement) {
                bonus += effect.bonus
            }
        }
        return bonus
    }

    /**
     * Returns the overridden normal-attack element for [attacker] if any of
     * its active statuses infuses an element, or null if none do.
     */
    fun infusedElementFor(attacker: Character): Element? {
        for (status in attacker.activeStatuses) {
            // Direct elementOverride field takes precedence
            status.elementOverride?.let { return it }
            val effect = StatusEffectRegistry.get(status.id)
            if (effect is StatusEffect.ElementInfusionEffect) return effect.element
        }
        return null
    }

    /**
     * If the character is Frozen and hit by a shatter element, remove the
     * freeze (and return true). Called by damage resolution.
     */
    fun maybeShatterFreeze(defender: Character, attackElement: Element): Boolean {
        val frozen = defender.activeStatuses.find { it.id == "FROZEN" || it.name.equals("Frozen", true) }
            ?: return false
        val shatterElems = if (frozen.shatterElements.isNotEmpty()) {
            frozen.shatterElements
        } else {
            val effect = StatusEffectRegistry.get(frozen.id)
            if (effect is StatusEffect.FreezeEffect) effect.shatterOn else emptyList()
        }
        if (attackElement in shatterElems) {
            defender.activeStatuses.remove(frozen)
            return true
        }
        return false
    }
}
