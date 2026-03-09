package io.github.milov.thecatstail.infrastructure.repository

import io.github.milov.thecatstail.domain.model.User
import io.github.milov.thecatstail.domain.repository.UserRepository
import org.springframework.stereotype.Repository

@Repository
class InMemUserRepository : UserRepository {
  private val users =
      mapOf(
          "milo@example.com" to
              User(
                  "milo@example.com",
                  "Milo",
                  "https://api.dicebear.com/9.x/adventurer/svg?seed=Milo"),
          "prince@example.com" to
              User(
                  "prince@example.com",
                  "Prince",
                  "https://api.dicebear.com/9.x/adventurer/svg?seed=Prince"))

  override fun getById(id: String): User {
    return users[id] ?: throw NoSuchElementException("User with id $id not found")
  }
}
