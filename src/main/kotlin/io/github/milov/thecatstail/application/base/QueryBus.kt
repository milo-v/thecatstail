package io.github.milov.thecatstail.application.base

interface QueryBus {
  fun <R> query(query: Query<R>): R
}
