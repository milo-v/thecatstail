package io.github.milov.thecatstail.domain.repository

import io.github.milov.thecatstail.domain.model.ActionCard
import io.github.milov.thecatstail.domain.model.Character
import io.github.milov.thecatstail.domain.model.Deck

interface DeckRepository {
  fun getById(id: String): Deck
  fun getAllCards(): List<ActionCard>
  fun getCardById(id: String): ActionCard?
  fun validateDeck(cards: List<ActionCard>, selectedCharacters: List<Character>): List<String>
}
