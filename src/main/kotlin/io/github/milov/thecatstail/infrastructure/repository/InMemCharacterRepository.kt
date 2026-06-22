package io.github.milov.thecatstail.infrastructure.repository

import io.github.milov.thecatstail.domain.model.Character
import io.github.milov.thecatstail.domain.repository.CharacterRepository
import org.springframework.stereotype.Repository

@Repository
class InMemCharacterRepository(private val loader: TcgDataLoader) : CharacterRepository {
  private val characters = loader.loadCharacters().associateBy { it.id }

  override fun getById(id: String): Character {
    return characters[id]
        ?: throw NoSuchElementException("Character with id $id not found")
  }

  override fun getAll(): List<Character> = characters.values.toList()
}
