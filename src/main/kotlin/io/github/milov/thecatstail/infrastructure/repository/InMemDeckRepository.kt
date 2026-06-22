package io.github.milov.thecatstail.infrastructure.repository

import io.github.milov.thecatstail.domain.model.ActionCard
import io.github.milov.thecatstail.domain.model.Character
import io.github.milov.thecatstail.domain.model.Deck
import io.github.milov.thecatstail.domain.repository.DeckRepository
import org.springframework.stereotype.Repository

@Repository
class InMemDeckRepository(private val loader: TcgDataLoader) : DeckRepository {
  private val cards = loader.loadActionCards()
  private val cardsById = cards.associateBy { it.id }

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

  override fun getAllCards(): List<ActionCard> = cards

  override fun getCardById(id: String): ActionCard? = cardsById[id]

  override fun validateDeck(
      cards: List<ActionCard>,
      selectedCharacters: List<Character>
  ): List<String> {
    val errors = mutableListOf<String>()
    if (cards.size != 30) {
      errors.add("Deck must contain exactly 30 cards (currently ${cards.size}).")
    }

    val counts = cards.groupingBy { it.id }.eachCount()
    val characterNames = selectedCharacters.map { it.name }.toSet()

    for ((cardId, count) in counts) {
      val card = cardsById[cardId] ?: continue
      if (card.isTalent) {
        if (count > 1) {
          errors.add("Talent card '${card.name}' may only appear once (found $count).")
        }
        val requiredChar = card.talentCharacterId
        if (requiredChar != null && requiredChar !in characterNames) {
          errors.add("Talent card '${card.name}' requires $requiredChar in your team.")
        }
      } else {
        if (count > 2) {
          errors.add("Card '${card.name}' may only appear up to 2 times (found $count).")
        }
      }
    }
    return errors
  }
}
