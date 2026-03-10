package io.github.milov.thecatstail.infrastructure.repository

import io.github.milov.thecatstail.domain.model.*
import io.github.milov.thecatstail.domain.repository.DeckRepository
import org.springframework.stereotype.Repository

@Repository
class InMemDeckRepository : DeckRepository {
  private val cards = listOf(
      ActionCard("paimon", "Paimon", ActionCardType.SUPPORT, cost = listOf(DiceCost(Element.UNALIGNED, 3)), supportEffect = SupportCard("paimon", "Paimon", 2)),
      ActionCard("liben", "Liben", ActionCardType.SUPPORT, cost = listOf(DiceCost(Element.UNALIGNED, 0)), supportEffect = SupportCard("liben", "Liben", 1)),
      ActionCard("strategize", "Strategize", ActionCardType.EVENT, cost = listOf(DiceCost(Element.UNALIGNED, 1))),
      ActionCard("leave-it-to-me", "Leave It to Me!", ActionCardType.EVENT, cost = listOf(DiceCost(Element.UNALIGNED, 0)))
  )

  private val decks =
      mapOf(
          "deck-milo" to
              Deck(
                  "deck-milo",
                  "milo@example.com",
                  "Milo's Deck",
                  listOf("Fischl", "Mona", "Diluc"),
                  List(30) { cards[it % cards.size].id }),
          "deck-prince" to
              Deck(
                  "deck-prince",
                  "prince@example.com",
                  "Prince's Deck",
                  listOf("Kaeya", "Sucrose", "Noelle"),
                  List(30) { cards[it % cards.size].id }))

  override fun getById(id: String): Deck {
    return decks[id] ?: throw NoSuchElementException("Deck with id $id not found")
  }
}
