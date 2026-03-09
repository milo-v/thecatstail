package io.github.milov.thecatstail.application.base

interface CommandBus {
  fun <C : Command> dispatch(command: C)
}
