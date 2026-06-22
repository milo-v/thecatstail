package io.github.milov.thecatstail.domain.model

data class Summon(
    val id: String,
    val name: String,
    var element: Element,
    var baseDamage: Int,
    var usages: Int,
    val healAmount: Int = 0,
    val isMultiTarget: Boolean = false,
    val description: String = "",
    val imageUrl: String = ""
) {
    fun deepCopy() = copy()
}
