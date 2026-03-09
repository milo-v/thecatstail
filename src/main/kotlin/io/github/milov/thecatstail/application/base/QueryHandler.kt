package io.github.milov.thecatstail.application.base

interface QueryHandler<Q : Query<R>, R> {
  fun handle(query: Q): R
}
