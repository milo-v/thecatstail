package io.github.milov.thecatstail.infrastructure.repository

import io.github.milov.thecatstail.domain.model.Deck
import io.github.milov.thecatstail.domain.repository.DeckRepository
import org.springframework.stereotype.Repository

@Repository
class InMemDeckRepository : DeckRepository {
  private val decks =
      mapOf(
          "deck-milo" to
              Deck(
                  "deck-milo",
                  "milo@example.com",
                  "Milo's Deck",
                  listOf("Diluc", "Kaeya", "Sucrose"),
                  emptyList()),
          "deck-prince" to
              Deck(
                  "deck-prince",
                  "prince@example.com",
                  "Prince's Deck",
                  listOf("Fischl", "Ganyu", "Diona"),
                  emptyList()))

  override fun getById(id: String): Deck {
    return decks[id] ?: throw NoSuchElementException("Deck with id $id not found")
  }
}
