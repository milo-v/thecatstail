package io.github.milov.thecatstail.domain.logic

import io.github.milov.thecatstail.domain.model.*

/**
 * Sealed hierarchy describing immediate effects that action cards apply when played.
 * Equipment cards attach to the active character via [Equip]; events run immediately;
 * support cards place a [SupportCard] into the support zone.
 *
 * ActionCard itself stays immutable — all mutable state from card effects lives on
 * Character.equipment, PlayerState.supportZone, or PlayerState.dicePool.
 */
sealed class CardEffect {
    /** Attach an equipment instance into a specific slot on the active character. */
    data class Equip(
        val slot: EquipSlot,
        val instance: EquipmentInstance
    ) : CardEffect()

    /** Heal the active character (or a specific character index) by [amount]. */
    data class Heal(val amount: Int, val allCharacters: Boolean = false) : CardEffect()

    /** Draw [amount] cards from the top of the deck. */
    data class DrawCards(val amount: Int) : CardEffect()

    /** Generate [amount] dice of [element] into the active player's dice pool. */
    data class GenerateDice(val element: Element, val amount: Int) : CardEffect()

    /** Place a support card into the support zone. */
    data class PlaceSupport(val support: SupportCard) : CardEffect()

    /** Restore energy to the active character. */
    data class GainEnergy(val amount: Int) : CardEffect()

    /** Composite: run several effects in order. */
    data class Composite(val effects: List<CardEffect>) : CardEffect()
}

enum class EquipSlot { WEAPON, ARTIFACT, TALENT }

object CardEffectRegistry {
    private val effects: Map<String, CardEffect> = buildMap {
        // -------------------- WEAPONS --------------------
        // Magic Guide — Catalyst weapon, +1 DMG
        put("311101", CardEffect.Equip(
            EquipSlot.WEAPON,
            EquipmentInstance(cardId = "311101", name = "Magic Guide", damageBonus = 1)
        ))
        // Sacrificial Fragments — Catalyst, +1 DMG
        put("311102", CardEffect.Equip(
            EquipSlot.WEAPON,
            EquipmentInstance(cardId = "311102", name = "Sacrificial Fragments", damageBonus = 1)
        ))
        // Sacrificial Sword — Sword weapon, +1 DMG
        put("311301", CardEffect.Equip(
            EquipSlot.WEAPON,
            EquipmentInstance(cardId = "311301", name = "Sacrificial Sword", damageBonus = 1)
        ))
        // Raven Bow — Bow weapon, +1 DMG
        put("311501", CardEffect.Equip(
            EquipSlot.WEAPON,
            EquipmentInstance(cardId = "311501", name = "Raven Bow", damageBonus = 1)
        ))

        // -------------------- ARTIFACTS --------------------
        // Witch's Scorching Hat — Pyro +1 DMG (approximate)
        put("312001", CardEffect.Equip(
            EquipSlot.ARTIFACT,
            EquipmentInstance(cardId = "312001", name = "Witch's Scorching Hat", damageBonus = 1)
        ))
        // Blizzard Strayer's Cryo Crown — Cryo +1 DMG (approximate)
        put("312002", CardEffect.Equip(
            EquipSlot.ARTIFACT,
            EquipmentInstance(cardId = "312002", name = "Broken Rime's Echo", damageBonus = 1)
        ))
        // Thunder Summoner's Crown — Electro +1 DMG (approximate)
        put("312003", CardEffect.Equip(
            EquipSlot.ARTIFACT,
            EquipmentInstance(cardId = "312003", name = "Thunder Summoner's Crown", damageBonus = 1)
        ))
        // Instructor's Cap — generic +1 DMG
        put("312004", CardEffect.Equip(
            EquipSlot.ARTIFACT,
            EquipmentInstance(cardId = "312004", name = "Instructor's Cap", damageBonus = 1)
        ))

        // -------------------- FOOD / HEAL --------------------
        // Mushroom Pizza — heal 1 HP
        put("333004", CardEffect.Heal(amount = 1))
        // Teyvat Fried Egg — revive with 1 HP (simplified to small heal)
        put("333003", CardEffect.Heal(amount = 1))
        // Sweet Madame — heal 1 HP
        put("333001", CardEffect.Heal(amount = 1))
        // Northern Smoked Chicken — draw 1 card (simplified)
        put("333002", CardEffect.DrawCards(amount = 1))

        // -------------------- ELEMENTAL RESONANCE --------------------
        // Fervent Flames — +1 Pyro die
        put("331601", CardEffect.GenerateDice(Element.PYRO, 1))
        // Soothing Water — +1 Hydro die
        put("331602", CardEffect.GenerateDice(Element.HYDRO, 1))
        // High Voltage — +1 Electro die
        put("331603", CardEffect.GenerateDice(Element.ELECTRO, 1))
        // Impetuous Winds — +1 Anemo die
        put("331604", CardEffect.GenerateDice(Element.ANEMO, 1))
        // Enduring Rock — +1 Geo die
        put("331605", CardEffect.GenerateDice(Element.GEO, 1))
        // Sprawling Greenery — +1 Dendro die
        put("331606", CardEffect.GenerateDice(Element.DENDRO, 1))
        // Shattering Ice — +1 Cryo die
        put("331607", CardEffect.GenerateDice(Element.CRYO, 1))

        // -------------------- GENERIC EVENTS --------------------
        // The Bestest Travel Companion — 2 UNALIGNED dice become 2 OMNI (simplified +1 OMNI)
        put("332001", CardEffect.GenerateDice(Element.OMNI, 1))
        // Changing Shifts — simplified draw 1
        put("332002", CardEffect.DrawCards(1))
        // Toss-Up — draw 1
        put("332003", CardEffect.DrawCards(1))
        // Strategize — draw 2
        put("332004", CardEffect.DrawCards(2))
        // Quick Knit — draw 1
        put("332007", CardEffect.DrawCards(1))

        // -------------------- COMPANIONS (Support) --------------------
        // Paimon — at round start, give 2 OMNI. Modeled as passive support with periodic trigger.
        put("322001", CardEffect.PlaceSupport(
            SupportCard(
                id = "322001",
                name = "Paimon",
                usages = 2,
                description = "When the Action Phase begins: Create Omni Element x2.",
                triggerType = SupportTrigger.ON_ROUND_START
            )
        ))
        // Liben — 3 uses, collects dice; simplified as passive support
        put("322002", CardEffect.PlaceSupport(
            SupportCard(
                id = "322002",
                name = "Liben",
                usages = 3,
                description = "Collects unused dice each end phase.",
                triggerType = SupportTrigger.ON_ROUND_END
            )
        ))

        // -------------------- LOCATIONS (Support) --------------------
        // Favonius Cathedral — heal active character each round
        put("321001", CardEffect.PlaceSupport(
            SupportCard(
                id = "321001",
                name = "Favonius Cathedral",
                usages = 2,
                description = "Heal your active character 1 HP each end phase.",
                triggerType = SupportTrigger.ON_ROUND_END
            )
        ))
        // Knights of Favonius Library — draw when a character uses a Burst
        put("321002", CardEffect.PlaceSupport(
            SupportCard(
                id = "321002",
                name = "Knights of Favonius Library",
                usages = null,
                description = "When played: Draw 1 card.",
                triggerType = SupportTrigger.ON_ACTION
            )
        ))
    }

    fun get(cardId: String): CardEffect? = effects[cardId]

    fun isRegistered(cardId: String): Boolean = effects.containsKey(cardId)

    /**
     * Applies a card's registered effect. Returns true if the card had a
     * registered effect and was processed, false otherwise.
     */
    fun apply(match: Match, player: PlayerState, card: ActionCard): Boolean {
        val effect = effects[card.id] ?: return false
        applyEffect(match, player, card, effect)
        return true
    }

    private fun applyEffect(match: Match, player: PlayerState, card: ActionCard, effect: CardEffect) {
        when (effect) {
            is CardEffect.Equip -> {
                val active = player.getActiveCharacter()
                val newEquip = when (effect.slot) {
                    EquipSlot.WEAPON -> active.equipment.withWeapon(effect.instance)
                    EquipSlot.ARTIFACT -> active.equipment.withArtifact(effect.instance)
                    EquipSlot.TALENT -> active.equipment.withTalent(effect.instance)
                }
                active.equipment = newEquip
            }
            is CardEffect.Heal -> {
                val targets = if (effect.allCharacters) player.characters.filter { it.isAlive }
                              else listOf(player.getActiveCharacter())
                for (t in targets) {
                    t.currentHp = minOf(t.maxHp, t.currentHp + effect.amount)
                }
            }
            is CardEffect.DrawCards -> {
                repeat(effect.amount) {
                    if (player.deck.isNotEmpty()) {
                        player.hand.add(player.deck.removeAt(0))
                    }
                }
            }
            is CardEffect.GenerateDice -> {
                player.dicePool[effect.element] =
                    (player.dicePool[effect.element] ?: 0) + effect.amount
            }
            is CardEffect.PlaceSupport -> {
                player.supportZone.add(effect.support.deepCopy())
                match.applyEvent(GameEvent.SupportCardPlayed(player.userId, card.name))
            }
            is CardEffect.GainEnergy -> {
                val active = player.getActiveCharacter()
                active.currentEnergy = minOf(active.maxEnergy, active.currentEnergy + effect.amount)
                match.applyEvent(GameEvent.EnergyGained(active.name, effect.amount))
            }
            is CardEffect.Composite -> {
                effect.effects.forEach { applyEffect(match, player, card, it) }
            }
        }
    }
}
