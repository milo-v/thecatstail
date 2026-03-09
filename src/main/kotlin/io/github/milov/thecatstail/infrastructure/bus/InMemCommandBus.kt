package io.github.milov.thecatstail.infrastructure.bus

import io.github.milov.thecatstail.application.base.Command
import io.github.milov.thecatstail.application.base.CommandBus
import io.github.milov.thecatstail.application.base.CommandHandler

class InMemCommandBus(private val handlers: List<CommandHandler<*>>) : CommandBus {
  @Suppress("UNCHECKED_CAST")
  override fun <C : Command> dispatch(command: C) {
    val handler =
        handlers.firstNotNullOfOrNull { it as? CommandHandler<C> }
            ?: throw IllegalStateException(
                "No handler found for command type ${command::class.simpleName}"
            )
    return handler.handle(command)
  }
}
