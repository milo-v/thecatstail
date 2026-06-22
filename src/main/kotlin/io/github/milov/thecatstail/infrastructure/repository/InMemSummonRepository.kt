package io.github.milov.thecatstail.infrastructure.repository

import io.github.milov.thecatstail.domain.model.Element
import org.springframework.stereotype.Repository

data class SummonMetadata(
    val id: String,
    val name: String,
    val element: Element,
    val usages: Int,
    val baseDamage: Int,
    val description: String
)

@Repository
class InMemSummonRepository(loader: TcgDataLoader) {
    private val byId: Map<String, SummonMetadata> = loader.loadSummons().associateBy { it.id }
    private val byName: Map<String, SummonMetadata> = byId.values.associateBy { it.name }

    fun getById(id: String): SummonMetadata? = byId[id]
    fun getByName(name: String): SummonMetadata? = byName[name]
    fun getAll(): List<SummonMetadata> = byId.values.toList()
}
