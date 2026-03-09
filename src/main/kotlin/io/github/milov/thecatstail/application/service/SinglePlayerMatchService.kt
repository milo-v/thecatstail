package io.github.milov.thecatstail.application.service

import io.github.milov.thecatstail.domain.logic.*
import io.github.milov.thecatstail.domain.logic.ai.MctsBot
import io.github.milov.thecatstail.domain.model.*
import io.github.milov.thecatstail.domain.repository.CharacterRepository
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class SinglePlayerMatchService(private val characterRepository: CharacterRepository) {
    private val matches = ConcurrentHashMap<String, Match>()
    private val bot = MctsBot(iterations = 500)

    fun startNewMatch(userId: String): Match {
        val playerCharacters = listOf("Diluc", "Kaeya", "Sucrose").map { characterRepository.getById(it).deepCopy() }
        val botCharacters = listOf("Mona", "Collei", "Noelle").map { characterRepository.getById(it).deepCopy() }

        val humanPlayer = PlayerState(userId, playerCharacters)
        val botPlayer = PlayerState("BOT", botCharacters)

        val match = Match("match-$userId", listOf(humanPlayer, botPlayer))
        match.applyEvent(GameEvent.MatchStarted(userId, "BOT"))
        MatchManager.startRound(match)
        
        matches[userId] = match
        return match
    }

    fun getMatch(userId: String): Match? = matches[userId]

    fun handleAction(userId: String, move: Move): Match {
        val match = matches[userId] ?: throw IllegalStateException("Match not found")
        
        // 1. Human Move
        if (match.getActivePlayer().userId == userId) {
            MatchManager.applyMove(match, userId, move)
        }

        // 2. Bot Turn(s)
        while (match.getActivePlayer().userId == "BOT") {
            val botMove = bot.findBestMove(match)
            if (botMove != null) {
                MatchManager.applyMove(match, "BOT", botMove)
            } else {
                // Should not happen if DeclareEnd is always available
                MatchManager.applyMove(match, "BOT", Move.DeclareEnd)
            }
        }

        return match
    }
}
