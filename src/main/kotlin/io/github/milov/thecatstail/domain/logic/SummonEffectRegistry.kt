package io.github.milov.thecatstail.domain.logic

import io.github.milov.thecatstail.domain.model.Element

/**
 * Dispatch-table describing what each summon does at the end phase.
 * Unregistered summons fall back to [defaultEffect] (1 Physical DMG).
 */
sealed class SummonEffect {
    /** Deal [amount] of [element] DMG to the opponent's active character. */
    data class DamageEffect(val element: Element, val amount: Int) : SummonEffect()

    /** Heal your active character by [amount]. */
    data class HealEffect(val amount: Int) : SummonEffect()

    /** Deal [amount] of [element] DMG to ALL opposing characters. */
    data class MultiTargetEffect(val element: Element, val amount: Int) : SummonEffect()

    /** A damage hit plus a heal on own side (e.g. Bake-Kurage). */
    data class DamageAndHeal(
        val element: Element,
        val damage: Int,
        val heal: Int
    ) : SummonEffect()
}

object SummonEffectRegistry {
    private val effects: Map<String, SummonEffect> = buildMap {
        // After-Sales Service Rounds (Dori) — Electro 1
        put("114101", SummonEffect.DamageEffect(Element.ELECTRO, 1))
        // Bake-Kurage (Kokomi) — Hydro 1 + heal 1
        put("112051", SummonEffect.DamageAndHeal(Element.HYDRO, 1, 1))
        // Autumn Whirlwind (Sucrose) — Anemo 1 (swirl conversion handled dynamically)
        put("115052", SummonEffect.DamageEffect(Element.ANEMO, 1))
        // Sesshou Sakura (Yae Miko) — Electro 1
        put("114071", SummonEffect.DamageEffect(Element.ELECTRO, 1))
        // Celestial Shower (Ganyu Burst) — Cryo multi-target 1
        put("111013", SummonEffect.MultiTargetEffect(Element.CRYO, 1))
        // Frostflake Seki no To — Cryo 1
        put("111012", SummonEffect.DamageEffect(Element.CRYO, 1))
        // Melody Loop (Barbara) — Hydro 1 + heal 1
        put("112011", SummonEffect.DamageAndHeal(Element.HYDRO, 1, 1))
        // Reflection (Mona) — Hydro 1
        put("112021", SummonEffect.DamageEffect(Element.HYDRO, 1))
        // Lightfall Sword (Eula) — Physical 2 (modeled as UNALIGNED)
        put("111052", SummonEffect.DamageEffect(Element.UNALIGNED, 2))
        // Guoba (Xiangling) — Pyro 1
        put("113011", SummonEffect.DamageEffect(Element.PYRO, 1))
        // Baron Bunny (Amber) — Pyro 2
        put("113041", SummonEffect.DamageEffect(Element.PYRO, 2))
        // Cuilein-Anbar (Klee) — Pyro 1
        put("113052", SummonEffect.DamageEffect(Element.PYRO, 1))
        // Fatui Cryo Cicin Mage ice lotus — Cryo 1
        put("111021", SummonEffect.DamageEffect(Element.CRYO, 1))
        // Dendro Sprout (Collei) — Dendro 1
        put("117011", SummonEffect.DamageEffect(Element.DENDRO, 1))
        // Oz (Fischl) — Electro 1
        put("114031", SummonEffect.DamageEffect(Element.ELECTRO, 1))
    }

    private val defaultEffect: SummonEffect = SummonEffect.DamageEffect(Element.UNALIGNED, 1)

    fun get(summonId: String): SummonEffect = effects[summonId] ?: defaultEffect

    fun isRegistered(summonId: String): Boolean = effects.containsKey(summonId)
}
