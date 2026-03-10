package io.github.milov.thecatstail.domain.model

data class Summon(
    val id: String,
    val name: String,
    val element: Element,
    val baseDamage: Int,
    var usages: Int,
    val description: String = ""
) {
    fun deepCopy() = copy()
}
