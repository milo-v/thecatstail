package io.github.milov.thecatstail.application.projections

data class LoadingScreenData(
    val playerName: String,
    val playerAvatar: String,
    val playerDeck: List<String>,
    val opponentName: String,
    val opponentAvatar: String,
    val opponentDeck: List<String>,
)
