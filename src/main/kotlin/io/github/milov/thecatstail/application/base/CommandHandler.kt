package io.github.milov.thecatstail.application.base

interface CommandHandler<C : Command> {
  fun handle(command: C)
}
