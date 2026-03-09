package io.github.milov.thecatstail.application.query.handler

import io.github.milov.thecatstail.application.base.QueryHandler
import io.github.milov.thecatstail.application.projections.LoadingScreenData
import io.github.milov.thecatstail.application.query.GetLoadingScreenData
import io.github.milov.thecatstail.domain.repository.DeckRepository
import io.github.milov.thecatstail.domain.repository.UserRepository
import org.springframework.stereotype.Component

@Component
class GetLoadingScreenDataHandler(
    private val userRepository: UserRepository,
    private val deckRepository: DeckRepository,
) : QueryHandler<GetLoadingScreenData, LoadingScreenData> {
  override fun handle(query: GetLoadingScreenData): LoadingScreenData {
    val player = userRepository.getById("milo@example.com")
    val opponent = userRepository.getById("prince@example.com")

    val playerDeck = deckRepository.getById("deck-milo")
    val opponentDeck = deckRepository.getById("deck-prince")

    return LoadingScreenData(
        playerName = player.displayName,
        playerAvatar = player.avatarUrl,
        playerDeck = playerDeck.characters,
        opponentName = opponent.displayName,
        opponentAvatar = opponent.avatarUrl,
        opponentDeck = opponentDeck.characters,
    )
  }
}
