package io.github.milov.thecatstail.domain.repository

import io.github.milov.thecatstail.domain.model.Character

interface CharacterRepository {
  fun getById(id: String): Character
}
