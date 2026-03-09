package io.github.milov.thecatstail.domain.model

import io.github.milov.thecatstail.domain.base.DomainEntity

class User(email: String, val displayName: String, val avatarUrl: String) : DomainEntity(email)
