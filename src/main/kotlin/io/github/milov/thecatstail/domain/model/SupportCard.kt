package io.github.milov.thecatstail.domain.model

data class SupportCard(
    val id: String,
    val name: String,
    var usages: Int? = null, // null means permanent until replaced or specific trigger
    val description: String = ""
) {
    fun deepCopy() = copy()
}
