package com.logics.logics.controllers

import org.springframework.security.web.csrf.CsrfToken
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Controller
@RequestMapping("/auth")
class PageController {

    @GetMapping("/login")
    fun showLoginPage(exchange: ServerWebExchange, model: Model): Mono<String> {
        val csrfToken = exchange.getAttribute<Mono<CsrfToken>>(CsrfToken::class.java.name)
        return csrfToken?.doOnSuccess { token ->
            model.addAttribute("_csrf", token)
        }?.thenReturn("login") ?: Mono.just("login")
    }

    @GetMapping("/register")
    fun showRegistrationPage(exchange: ServerWebExchange, model: Model): Mono<String> {
        val csrfToken = exchange.getAttribute<Mono<CsrfToken>>(CsrfToken::class.java.name)
        return csrfToken?.doOnSuccess { token ->
            model.addAttribute("_csrf", token)
        }?.thenReturn("register") ?: Mono.just("register")
    }
}