package com.logics.logics.controllers

import com.logics.logics.entities.CategoryList
import com.logics.logics.services.UserService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import reactor.core.publisher.Mono

@Controller
@RequestMapping("/")
class HomeController(private val userService: UserService) {

    @GetMapping
    fun home(@AuthenticationPrincipal userDetails: UserDetails, model: Model): Mono<String> {
        return userService.getUserDetails(userDetails.username)
            .doOnNext { user ->
                model.addAttribute("username", user.username)
                model.addAttribute("coins", user.coins)
                model.addAttribute("categories", CategoryList.values())
            }
            .thenReturn("home")
    }
}