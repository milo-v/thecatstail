package io.github.milov.thecatstail.application.service

import io.github.milov.thecatstail.domain.logic.*
import io.github.milov.thecatstail.domain.logic.ai.MctsBot
import io.github.milov.thecatstail.domain.model.*
import io.github.milov.thecatstail.domain.repository.CharacterRepository
import io.github.milov.thecatstail.domain.repository.DeckRepository
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class SinglePlayerMatchService(
    private val characterRepository: CharacterRepository,
    private val deckRepository: DeckRepository
) {
    private val matches = ConcurrentHashMap<String, Match>()
    private val bot = MctsBot(timeBudgetMs = 500L)

    private val botCharacterIds = listOf("Mona", "Collei", "Noelle")
    private val defaultPlayerCharacterIds = listOf("Diluc", "Kaeya", "Sucrose")

    fun startNewMatch(userId: String): Match {
        return startMatch(userId, defaultPlayerCharacterIds, buildPresetDeckIds())
    }

    fun startMatch(
        userId: String,
        playerCharacterIds: List<String>,
        playerDeckCardIds: List<String>
    ): Match {
        val playerCharacters = playerCharacterIds.map { characterRepository.getById(it).deepCopy() }
        val botCharacters = botCharacterIds.map { characterRepository.getById(it).deepCopy() }

        val humanPlayer = PlayerState(userId, playerCharacters)
        humanPlayer.deck.addAll(
            playerDeckCardIds.mapNotNull { deckRepository.getCardById(it) }.toMutableList()
        )

        val botPlayer = PlayerState("BOT", botCharacters)
        botPlayer.deck.addAll(buildPresetDeckIds().mapNotNull { deckRepository.getCardById(it) })

        val match = Match("match-$userId", listOf(humanPlayer, botPlayer))
        match.applyEvent(GameEvent.MatchStarted(userId, "BOT"))
        // Initial 5-card draw + mulligan tracking
        MatchManager.initMatch(match)
        // Bot performs a random mulligan immediately
        performBotMulligan(match)

        matches[userId] = match
        return match
    }

    private fun performBotMulligan(match: Match) {
        val botPlayer = match.players.find { it.userId == "BOT" } ?: return
        val handSize = botPlayer.hand.size
        val replaceCount = if (handSize == 0) 0 else kotlin.random.Random.nextInt(0, minOf(3, handSize + 1))
        val indices = (0 until handSize).shuffled().take(replaceCount)
        MatchManager.performMulligan(match, "BOT", indices)
    }

    fun performHumanMulligan(userId: String, indices: List<Int>): Match {
        val match = matches[userId] ?: throw IllegalStateException("Match not found")
        MatchManager.performMulligan(match, userId, indices)
        autoConfirmBotDice(match)
        return match
    }

    private fun autoConfirmBotDice(match: Match) {
        if (match.phase == Phase.ROLL && "BOT" in match.rolledDice.keys) {
            MatchManager.confirmDice(match, "BOT")
        }
    }

    fun rerollHumanDice(userId: String, indices: List<Int>): Match {
        val match = matches[userId] ?: throw IllegalStateException("Match not found")
        MatchManager.rerollDice(match, userId, indices)
        return match
    }

    fun confirmHumanDice(userId: String): Match {
        val match = matches[userId] ?: throw IllegalStateException("Match not found")
        autoConfirmBotDice(match)
        MatchManager.confirmDice(match, userId)
        drainBotTurns(match)
        return match
    }

    fun endMatch(userId: String) {
        matches.remove(userId)
    }

    private fun buildPresetDeckIds(): List<String> {
        val all = deckRepository.getAllCards()
        if (all.isEmpty()) return emptyList()
        // Use non-talent cards to avoid validation conflicts
        val safe = all.filter { !it.isTalent }.ifEmpty { all }
        return List(30) { safe[it % safe.size].id }
    }

    fun getMatch(userId: String): Match? = matches[userId]

    fun handleAction(userId: String, move: Move): Match {
        val match = matches[userId] ?: throw IllegalStateException("Match not found")

        if (match.winner != null) return match

        // Ensure bot dice are confirmed in case player acts while bot is still in ROLL
        autoConfirmBotDice(match)

        // 1. Human Move (only if we're in ACTION phase on the human's turn)
        if (match.phase == Phase.ACTION && match.getActivePlayer().userId == userId) {
            MatchManager.applyMove(match, userId, move)
        }

        // 2. Bot Turn(s)
        drainBotTurns(match)

        return match
    }

    private fun drainBotTurns(match: Match) {
        var safety = 500
        while (match.winner == null &&
            match.phase == Phase.ACTION &&
            match.getActivePlayer().userId == "BOT"
        ) {
            if (safety-- <= 0) break
            val botMove = bot.findBestMove(match)
            if (botMove != null) {
                MatchManager.applyMove(match, "BOT", botMove)
            } else {
                MatchManager.applyMove(match, "BOT", Move.DeclareEnd)
            }
            // After any move, a new round may begin (ROLL). Auto-confirm bot dice.
            autoConfirmBotDice(match)
        }
    }
}
