package io.github.milov.thecatstail.presentation

import io.github.milov.thecatstail.application.service.SinglePlayerMatchService
import io.github.milov.thecatstail.domain.logic.CombatEngine
import io.github.milov.thecatstail.domain.logic.Move
import io.github.milov.thecatstail.domain.model.Element
import io.github.milov.thecatstail.domain.repository.CharacterRepository
import io.github.milov.thecatstail.domain.repository.DeckRepository
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.mvc.support.RedirectAttributes

@Controller
@RequestMapping("/game")
class GameController(
    private val matchService: SinglePlayerMatchService,
    private val characterRepository: CharacterRepository,
    private val deckRepository: DeckRepository
) {

    @RequestMapping("")
    fun game(
        model: Model,
        @RequestHeader(value = "HX-Request", required = false) hxRequest: String?
    ): String {
        val userId = "milo" // Fixed for single player prototype
        val match = matchService.getMatch(userId) ?: return "redirect:/game/setup"
        model.addAttribute("match", match)
        model.addAttribute("userId", userId)
        return if (hxRequest != null) "game :: content" else "game_page"
    }

    @GetMapping("/setup")
    fun setup(model: Model): String {
        model.addAttribute("characters", characterRepository.getAll().sortedBy { it.name })
        model.addAttribute("cards", deckRepository.getAllCards().sortedBy { it.name })
        return "setup"
    }

    @PostMapping("/setup")
    fun startWithSelection(
        @RequestParam(required = false) characterIds: List<String>?,
        @RequestParam(required = false) cardIds: List<String>?,
        @RequestParam(required = false, defaultValue = "false") quickStart: Boolean,
        model: Model,
        redirectAttributes: RedirectAttributes
    ): String {
        val userId = "milo"
        val selectedCharIds = characterIds ?: emptyList()

        if (selectedCharIds.size != 3) {
            redirectAttributes.addFlashAttribute(
                "errors",
                listOf("You must select exactly 3 characters (selected ${selectedCharIds.size}).")
            )
            return "redirect:/game/setup"
        }

        val selectedCharacters = selectedCharIds.map { characterRepository.getById(it) }

        val finalDeckIds: List<String> =
            if (quickStart || cardIds.isNullOrEmpty()) {
                buildPresetDeck()
            } else {
                cardIds
            }

        val cards = finalDeckIds.mapNotNull { deckRepository.getCardById(it) }
        val errors = deckRepository.validateDeck(cards, selectedCharacters)
        if (errors.isNotEmpty()) {
            redirectAttributes.addFlashAttribute("errors", errors)
            return "redirect:/game/setup"
        }

        matchService.startMatch(userId, selectedCharIds, finalDeckIds)
        return "redirect:/game"
    }

    @PostMapping("/new")
    fun newGame(): String {
        return "redirect:/game/setup"
    }

    private fun buildPresetDeck(): List<String> {
        val safe = deckRepository.getAllCards().filter { !it.isTalent }
        if (safe.isEmpty()) return emptyList()
        return List(30) { safe[it % safe.size].id }
    }

    @PostMapping("/action/preview")
    fun previewSkill(@RequestParam skillId: String, model: Model): String {
        val userId = "milo"
        val match = matchService.getMatch(userId) ?: throw IllegalStateException("Match not found")
        
        val attacker = match.getActivePlayer().getActiveCharacter()
        val defender = match.getInactivePlayer().getActiveCharacter()
        val preview = CombatEngine.simulateDamage(attacker, defender, match, skillId)
        
        model.addAttribute("match", match)
        model.addAttribute("userId", userId)
        model.addAttribute("preview", preview)
        model.addAttribute("previewSkillId", skillId)
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

    @PostMapping("/action/switch-preview")
    fun previewSwitch(@RequestParam targetIndex: Int, model: Model): String {
        val userId = "milo"
        val match = matchService.getMatch(userId) ?: throw IllegalStateException("Match not found")
        
        model.addAttribute("match", match)
        model.addAttribute("userId", userId)
        model.addAttribute("previewSwitchIndex", targetIndex)
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

    @PostMapping("/action/play")
    fun playCard(@RequestParam cardId: String, model: Model): String {
        val userId = "milo"
        val match = matchService.handleAction(userId, Move.PlayCard(cardId))
        model.addAttribute("match", match)
        model.addAttribute("userId", userId)
        return "game :: content"
    }

    @PostMapping("/action/tuning")
    fun tuneCard(@RequestParam cardId: String, @RequestParam element: Element, model: Model): String {
        val userId = "milo"
        val current = matchService.getMatch(userId) ?: throw IllegalStateException("Match not found")
        val pool = current.players.firstOrNull { it.userId == userId }?.dicePool ?: emptyMap()
        // Validation: cannot tune away OMNI dice and chosen element must exist in pool
        if (element == Element.OMNI || (pool[element] ?: 0) <= 0) {
            model.addAttribute("match", current)
            model.addAttribute("userId", userId)
            model.addAttribute("tuningError", "Invalid tuning element: $element")
            return "game :: content"
        }
        val match = matchService.handleAction(userId, Move.ElementalTuning(cardId, element))
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

    @PostMapping("/action/mulligan")
    fun mulligan(
        @RequestParam(required = false) indices: List<Int>?,
        model: Model
    ): String {
        val userId = "milo"
        val match = matchService.performHumanMulligan(userId, indices ?: emptyList())
        model.addAttribute("match", match)
        model.addAttribute("userId", userId)
        return "game :: content"
    }

    @PostMapping("/action/reroll")
    fun reroll(
        @RequestParam(required = false) indices: List<Int>?,
        model: Model
    ): String {
        val userId = "milo"
        val match = matchService.rerollHumanDice(userId, indices ?: emptyList())
        model.addAttribute("match", match)
        model.addAttribute("userId", userId)
        return "game :: content"
    }

    @PostMapping("/action/confirm-dice")
    fun confirmDice(model: Model): String {
        val userId = "milo"
        val match = matchService.confirmHumanDice(userId)
        model.addAttribute("match", match)
        model.addAttribute("userId", userId)
        return "game :: content"
    }

    @PostMapping("/rematch")
    fun rematch(model: Model): String {
        val userId = "milo"
        matchService.endMatch(userId)
        return "redirect:/game/setup"
    }
}
