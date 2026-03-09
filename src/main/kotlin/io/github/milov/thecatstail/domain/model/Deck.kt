package io.github.milov.thecatstail.domain.model

import io.github.milov.thecatstail.domain.base.DomainEntity

class Deck(
    id: String,
    val userId: String,
    val name: String,
    val characters: List<String>,
    val actionCards: List<String>,
) : DomainEntity(id)
