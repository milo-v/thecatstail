package io.github.milov.thecatstail.presentation

import io.github.milov.thecatstail.application.service.SinglePlayerMatchService
import io.github.milov.thecatstail.domain.logic.Move
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@RequestMapping("/game")
class GameController(private val matchService: SinglePlayerMatchService) {

    @RequestMapping("")
    fun game(model: Model): String {
        val userId = "milo" // Fixed for single player prototype
        val match = matchService.getMatch(userId) ?: matchService.startNewMatch(userId)
        model.addAttribute("match", match)
        model.addAttribute("userId", userId)
        return "game :: content"
    }

    @PostMapping("/action/skill")
    fun useSkill(@RequestParam skillId: String, model: Model): String {
        val userId = "milo"
        val match = matchService.handleAction(userId, Move.UseSkill(skillId))
        model.addAttribute("match", match)
        model.addAttribute("userId", userId)
        return "game :: content"
    }

    @PostMapping("/action/switch")
    fun switchCharacter(@RequestParam targetIndex: Int, model: Model): String {
        val userId = "milo"
        val match = matchService.handleAction(userId, Move.SwitchCharacter(targetIndex))
        model.addAttribute("match", match)
        model.addAttribute("userId", userId)
        return "game :: content"
    }

    @PostMapping("/action/end")
    fun declareEnd(model: Model): String {
        val userId = "milo"
        val match = matchService.handleAction(userId, Move.DeclareEnd)
        model.addAttribute("match", match)
        model.addAttribute("userId", userId)
        return "game :: content"
    }
}
