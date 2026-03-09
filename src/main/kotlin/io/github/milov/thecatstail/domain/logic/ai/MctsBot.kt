package io.github.milov.thecatstail.domain.logic.ai

import io.github.milov.thecatstail.domain.logic.*
import io.github.milov.thecatstail.domain.model.Match
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.random.Random

class MctsNode(
    val matchState: Match,
    val move: Move? = null,
    val parent: MctsNode? = null
) {
    val children = mutableListOf<MctsNode>()
    var wins = 0.0
    var visits = 0
    val untriedMoves = MoveGenerator.getLegalMoves(matchState).toMutableList()

    fun isFullyExpanded() = untriedMoves.isEmpty()
    fun isTerminal() = matchState.players.any { player -> player.characters.all { !it.isAlive } }
}

class MctsBot(private val iterations: Int = 100) {

    fun findBestMove(match: Match): Move? {
        val root = MctsNode(match.deepCopy())

        repeat(iterations) {
            var node = root
            
            // 1. Selection
            while (node.isFullyExpanded() && !node.isTerminal()) {
                node = bestChild(node)
            }

            // 2. Expansion
            if (!node.isTerminal() && node.untriedMoves.isNotEmpty()) {
                val move = node.untriedMoves.removeAt(Random.nextInt(node.untriedMoves.size))
                val nextState = node.matchState.deepCopy()
                MatchManager.applyMove(nextState, nextState.getActivePlayer().userId, move)
                val child = MctsNode(nextState, move, node)
                node.children.add(child)
                node = child
            }

            // 3. Simulation
            val result = simulateRandomPlayout(node.matchState.deepCopy())

            // 4. Backpropagation
            var backNode: MctsNode? = node
            while (backNode != null) {
                backNode.visits++
                // Simple score: if the player who made the move to reach this state won, it's a win
                // Actually, let's use the root's active player as the perspective
                if (result == root.matchState.getActivePlayer().userId) {
                    backNode.wins += 1.0
                } else if (result == "DRAW") {
                    backNode.wins += 0.5
                }
                backNode = backNode.parent
            }
        }

        return root.children.maxByOrNull { it.visits }?.move
    }

    private fun bestChild(node: MctsNode): MctsNode {
        val explorationParam = sqrt(2.0)
        return node.children.maxBy { child ->
            val uctValue = child.wins / child.visits + explorationParam * sqrt(ln(node.visits.toDouble()) / child.visits)
            uctValue
        }
    }

    private fun simulateRandomPlayout(match: Match): String {
        var currentMatch = match
        var limit = 200 // Increased limit
        var lastStateInfo = ""
        var stagnantCount = 0

        while (limit-- > 0) {
            val winner = getWinner(currentMatch)
            if (winner != null) return winner
            
            val moves = MoveGenerator.getLegalMoves(currentMatch)
            if (moves.isEmpty()) return "DRAW"
            
            val move = moves.random()
            val stateBefore = currentMatch.deepCopy()
            MatchManager.applyMove(currentMatch, currentMatch.getActivePlayer().userId, move)
            
            // Check if state actually changed
            val currentStateInfo = "${currentMatch.activePlayerIndex}-${currentMatch.roundNumber}-${currentMatch.players.map { p -> p.characters.map { c -> c.currentHp } }}-${currentMatch.players.map { p -> p.isFinishedActions }}"
            if (currentStateInfo == lastStateInfo) {
                stagnantCount++
                if (stagnantCount > 10) return "DRAW" // Stagnant state, force end
            } else {
                stagnantCount = 0
            }
            lastStateInfo = currentStateInfo
        }
        return "DRAW"
    }

    private fun getWinner(match: Match): String? {
        val p1 = match.players[0]
        val p2 = match.players[1]
        
        if (p1.characters.all { !it.isAlive }) return p2.userId
        if (p2.characters.all { !it.isAlive }) return p1.userId
        
        return null
    }
}
