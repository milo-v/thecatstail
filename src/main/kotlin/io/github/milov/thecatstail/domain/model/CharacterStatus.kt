package io.github.milov.thecatstail.domain.model

data class CharacterStatus(
    val id: String,
    val name: String,
    var usages: Int? = null,
    var durationRounds: Int? = null,
    val elementOverride: Element? = null,
    val damageBonus: Int = 0,
    var shieldValue: Int = 0,
    val damageReduction: Int = 0,
    var preventsAction: Boolean = false,
    val shatterElements: List<Element> = emptyList(),
    val description: String = ""
) {
    fun deepCopy() = copy()
}
