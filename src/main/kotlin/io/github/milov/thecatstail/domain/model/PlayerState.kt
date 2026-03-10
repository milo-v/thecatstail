package io.github.milov.thecatstail.domain.model

data class PlayerState(
    val userId: String,
    val characters: List<Character>,
    var activeCharacterIndex: Int = 0,
    val hand: MutableList<ActionCard> = mutableListOf(),
    val deck: MutableList<ActionCard> = mutableListOf(),
    val dicePool: MutableMap<Element, Int> = mutableMapOf(),
    val supportZone: MutableList<SupportCard> = mutableListOf(),
    val summonsZone: MutableList<Summon> = mutableListOf(),
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
            supportZone = supportZone.map { it.deepCopy() }.toMutableList(),
            summonsZone = summonsZone.map { it.deepCopy() }.toMutableList(),
            isFinishedActions = isFinishedActions
        )
    }
}
