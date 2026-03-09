package io.github.milov.thecatstail.presentation

import io.github.milov.thecatstail.application.base.QueryBus
import io.github.milov.thecatstail.application.query.GetLoadingScreenData
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("")
class MainController(private val queryBus: QueryBus) {
  @RequestMapping("/")
  fun home(): String {
    return "home"
  }

  @RequestMapping("/loading")
  fun loading(model: Model): String {
    val data = queryBus.query(GetLoadingScreenData())

    model.addAttribute("playerName", data.playerName)
    model.addAttribute("playerAvatar", data.playerAvatar)
    model.addAttribute("playerDeck", data.playerDeck)

    model.addAttribute("opponentName", data.opponentName)
    model.addAttribute("opponentAvatar", data.opponentAvatar)
    model.addAttribute("opponentDeck", data.opponentDeck)

    return "loading :: content"
  }
}
