package io.github.milov.thecatstail.infrastructure.repository

import org.springframework.stereotype.Repository

data class StatusMetadata(
    val id: String,
    val name: String,
    val statusType: String, // "Character Status", "Combat Status", "Team Status"
    val description: String
)

@Repository
class InMemStatusRepository(loader: TcgDataLoader) {
    private val byId: Map<String, StatusMetadata> = loader.loadStatuses().associateBy { it.id }
    private val byName: Map<String, StatusMetadata> = byId.values.associateBy { it.name }

    fun getById(id: String): StatusMetadata? = byId[id]
    fun getByName(name: String): StatusMetadata? = byName[name]
    fun getAll(): List<StatusMetadata> = byId.values.toList()
}
