package io.github.milov.thecatstail.domain.model

data class CharacterStatus(
    val id: String,
    val name: String,
    var usages: Int? = null,
    var durationRounds: Int? = null,
    val elementOverride: Element? = null,
    val damageBonus: Int = 0,
    val description: String = ""
) {
    fun deepCopy() = copy()
}
