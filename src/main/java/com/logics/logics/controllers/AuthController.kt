package com.logics.logics.controllers

import com.logics.logics.entities.User
import com.logics.logics.services.UserService
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/auth")
class AuthController(private val userService: UserService) {

    @PostMapping("/register")
    fun register(@ModelAttribute user: User): Mono<String> {
        return if (user.username != null && user.password != null) {
            userService.register(user.username!!, user.password!!)
                .map { "User registered successfully" }
                .onErrorResume { Mono.just(it.message ?: "Unknown error occurred") }
        } else {
            Mono.just("Username or password cannot be null")
        }
    }
}