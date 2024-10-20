package com.logics.logics.controllers

import com.logics.logics.services.GameService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import reactor.core.publisher.Mono

@Controller
@RequestMapping("/game")
class GameController(private val gameService: GameService) {

    @GetMapping
    fun gamePage(@AuthenticationPrincipal userDetails: UserDetails, @RequestParam roomId: Long, model: Model): Mono<String> {
        return gameService.getGameState(roomId)
            .doOnNext { gameState ->
                model.addAttribute("username", userDetails.username)
                model.addAttribute("teamA", gameState.teamA)
                model.addAttribute("teamB", gameState.teamB)
                model.addAttribute("teamAScore", gameState.teamAScore)
                model.addAttribute("teamBScore", gameState.teamBScore)
                model.addAttribute("category", gameState.category)
            }
            .thenReturn("game")
    }
}