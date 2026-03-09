package io.github.milov.thecatstail.infrastructure.bus

import io.github.milov.thecatstail.application.base.Query
import io.github.milov.thecatstail.application.base.QueryBus
import io.github.milov.thecatstail.application.base.QueryHandler
import org.springframework.stereotype.Component

@Component
class InMemQueryBus(private val handlers: List<QueryHandler<*, *>>) : QueryBus {
  @Suppress("UNCHECKED_CAST")
  override fun <R> query(query: Query<R>): R {
    val handler =
        handlers.firstNotNullOfOrNull { it as? QueryHandler<Query<R>, R> }
            ?: throw IllegalStateException(
                "No handler found for query type ${query::class.simpleName}"
            )
    return handler.handle(query)
  }
}
