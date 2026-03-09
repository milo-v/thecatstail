package io.github.milov.thecatstail.domain.logic

import io.github.milov.thecatstail.domain.model.Element
import kotlin.random.Random

object DiceEngine {
    private val playableElements = listOf(
        Element.PYRO, Element.HYDRO, Element.ANEMO, 
        Element.ELECTRO, Element.DENDRO, Element.CRYO, 
        Element.GEO, Element.OMNI
    )

    fun roll(count: Int = 8): List<Element> {
        return List(count) { playableElements[Random.nextInt(playableElements.size)] }
    }

    fun reroll(currentDice: List<Element>, indexesToReroll: List<Int>): List<Element> {
        val newDice = currentDice.toMutableList()
        for (index in indexesToReroll) {
            if (index in newDice.indices) {
                newDice[index] = playableElements[Random.nextInt(playableElements.size)]
            }
        }
        return newDice
    }

    fun elementalTuning(dicePool: MutableMap<Element, Int>, discardedCardElement: Element, targetElement: Element) {
        // In the actual game, it changes to the element of the ACTIVE character.
        // For simplicity now, let's just implement the logic of swapping 1 die for another of target element.
        // But the player must have a die to tune.
        // Let's assume the user picks a die element to tune FROM.
    }
}
