package io.github.milov.thecatstail.domain.logic

import io.github.milov.thecatstail.domain.model.Element

/**
 * Dispatch-table describing each character/combat status' effect.
 * Unregistered statuses default to [StatusEffect.NoOp].
 */
sealed class StatusEffect {
    object NoOp : StatusEffect()

    /** Absorbs up to [absorption] incoming damage, then expires. */
    data class ShieldEffect(val absorption: Int) : StatusEffect()

    /** Adds [bonus] DMG when this character attacks with [element]. */
    data class DamageBonusEffect(val element: Element, val bonus: Int) : StatusEffect()

    /** Converts this character's Normal Attack element to [element]. */
    data class ElementInfusionEffect(val element: Element) : StatusEffect()

    /** Reduces the dice cost of the next skill by [amount]. */
    data class CostReductionEffect(val amount: Int) : StatusEffect()

    /** Reduces all incoming damage by [percent]%. */
    data class DamageReductionEffect(val percent: Int) : StatusEffect()

    /** Prevents action next turn; shatters on the listed elements. */
    data class FreezeEffect(val shatterOn: List<Element>) : StatusEffect()
}

object StatusEffectRegistry {
    private val effects: Map<String, StatusEffect> = buildMap {
        // Crystallize Shield (Geo reaction)
        put("CRYSTALLIZE_SHIELD", StatusEffect.ShieldEffect(absorption = 1))
        // Frozen (Cryo + Hydro reaction)
        put("FROZEN", StatusEffect.FreezeEffect(shatterOn = listOf(Element.PYRO, Element.UNALIGNED)))
        // Cat-Claw Shield (Diona)
        put("112031", StatusEffect.ShieldEffect(absorption = 1))
        // Niwabi Enshou (Yoimiya) — Pyro infusion
        put("113081", StatusEffect.ElementInfusionEffect(Element.PYRO))
        // Pyro Infusion generic
        put("PYRO_INFUSION", StatusEffect.ElementInfusionEffect(Element.PYRO))
        // Hydro Infusion generic
        put("HYDRO_INFUSION", StatusEffect.ElementInfusionEffect(Element.HYDRO))
        // Cryo Infusion generic
        put("CRYO_INFUSION", StatusEffect.ElementInfusionEffect(Element.CRYO))
        // Electro Infusion generic
        put("ELECTRO_INFUSION", StatusEffect.ElementInfusionEffect(Element.ELECTRO))
        // Explosive Spark (Klee talent) — +1 DMG Pyro
        put("113053", StatusEffect.DamageBonusEffect(Element.PYRO, 1))
        // Icy Quill (Ayaka) — +1 DMG Cryo
        put("111021", StatusEffect.DamageBonusEffect(Element.CRYO, 1))
        // Vermillion Hereafter (Xiangling) — +1 DMG
        put("113012", StatusEffect.DamageBonusEffect(Element.PYRO, 1))
        // Shrine of Maya (Nahida) — +1 DMG Dendro
        put("117082", StatusEffect.DamageBonusEffect(Element.DENDRO, 1))
        // Pyronado (Xiangling burst) — +2 DMG Pyro
        put("113013", StatusEffect.DamageBonusEffect(Element.PYRO, 2))
        // Riptide / Hydro (Tartaglia) — +1 DMG Hydro
        put("112061", StatusEffect.DamageBonusEffect(Element.HYDRO, 1))
        // Inspiration Field (Bennett) — +2 DMG Pyro
        put("113071", StatusEffect.DamageBonusEffect(Element.PYRO, 2))
        // Jade Screen (Ningguang) — +1 DMG Geo
        put("116011", StatusEffect.DamageBonusEffect(Element.GEO, 1))
        // Crystal Shield (Zhongli)
        put("116031", StatusEffect.ShieldEffect(absorption = 2))
        // Silver And Melus (Fischl) — generic DMG bonus
        put("114032", StatusEffect.DamageBonusEffect(Element.ELECTRO, 1))
        // Electrocharged — Electro generic
        put("ELECTROCHARGED", StatusEffect.DamageBonusEffect(Element.ELECTRO, 1))
        // Generic damage reductions (25%)
        put("DAMAGE_REDUCTION_25", StatusEffect.DamageReductionEffect(percent = 25))
        // Cost reduction (e.g. Mushroom Pizza, Ei Talent)
        put("COST_REDUCTION_1", StatusEffect.CostReductionEffect(amount = 1))
    }

    fun get(statusId: String): StatusEffect = effects[statusId] ?: StatusEffect.NoOp

    fun isRegistered(statusId: String): Boolean = effects.containsKey(statusId)
}
