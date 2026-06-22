package io.github.milov.thecatstail.domain.model

/**
 * An equipment card attached to a character. Immutable — equipment effects live
 * in CardEffectRegistry; this record just tracks which card is attached and any
 * static stat bonuses (e.g. damage +1).
 */
data class EquipmentInstance(
    val cardId: String,
    val name: String,
    val damageBonus: Int = 0,
    val statBonus: Map<String, Int> = emptyMap()
)

data class CharacterEquipment(
    val weapon: EquipmentInstance? = null,
    val artifact: EquipmentInstance? = null,
    val talent: EquipmentInstance? = null
) {
    fun deepCopy(): CharacterEquipment = copy()

    fun withWeapon(w: EquipmentInstance) = copy(weapon = w)
    fun withArtifact(a: EquipmentInstance) = copy(artifact = a)
    fun withTalent(t: EquipmentInstance) = copy(talent = t)

    fun totalDamageBonus(): Int =
        (weapon?.damageBonus ?: 0) + (artifact?.damageBonus ?: 0) + (talent?.damageBonus ?: 0)
}
