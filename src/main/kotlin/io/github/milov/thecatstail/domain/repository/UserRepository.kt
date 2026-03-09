package io.github.milov.thecatstail.domain.repository

import io.github.milov.thecatstail.domain.model.User

interface UserRepository {
  fun getById(id: String): User
}
