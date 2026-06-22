package io.github.milov.thecatstail.domain.model

enum class SupportTrigger {
    ON_ROUND_START, ON_ROUND_END, ON_ACTION, PASSIVE
}

data class SupportCard(
    val id: String,
    val name: String,
    var usages: Int? = null, // null means permanent until replaced or specific trigger
    val description: String = "",
    val triggerType: SupportTrigger = SupportTrigger.PASSIVE
) {
    fun deepCopy() = copy()
}
