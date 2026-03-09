package io.github.milov.thecatstail.domain.model

data class PlayerState(
    val userId: String,
    val characters: List<Character>,
    var activeCharacterIndex: Int = 0,
    val hand: MutableList<ActionCard> = mutableListOf(),
    val deck: MutableList<ActionCard> = mutableListOf(),
    val dicePool: MutableMap<Element, Int> = mutableMapOf(),
    val supportZone: MutableList<ActionCard> = mutableListOf(), // Placeholder for now
    val summonsZone: MutableList<String> = mutableListOf(), // Placeholder for now
    var isFinishedActions: Boolean = false
) {
    fun getActiveCharacter(): Character = characters[activeCharacterIndex]

    fun deepCopy(): PlayerState {
        return PlayerState(
            userId = userId,
            characters = characters.map { it.deepCopy() },
            activeCharacterIndex = activeCharacterIndex,
            hand = hand.toMutableList(),
            deck = deck.toMutableList(),
            dicePool = dicePool.toMutableMap(),
            supportZone = supportZone.toMutableList(),
            summonsZone = summonsZone.toMutableList(),
            isFinishedActions = isFinishedActions
        )
    }
}
