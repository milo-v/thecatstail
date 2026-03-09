package io.github.milov.thecatstail.domain.repository

import io.github.milov.thecatstail.domain.model.Deck

interface DeckRepository {
  fun getById(id: String): Deck
}
