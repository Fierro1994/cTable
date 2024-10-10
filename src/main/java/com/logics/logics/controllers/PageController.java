package com.logics.logics.controllers;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


@Controller
@RequestMapping("/auth")
public class PageController {

  @GetMapping("/login")
  public Mono<String> showLoginPage(ServerWebExchange exchange, Model model) {
    Mono<CsrfToken> csrfToken = exchange.getAttribute(CsrfToken.class.getName());
    if (csrfToken != null) {
      return csrfToken.doOnSuccess(token ->
          model.addAttribute("_csrf", token)).thenReturn("login");
    }
    return Mono.just("login");
  }

  @GetMapping("/register")
  public Mono<String> showRegistrationPage(ServerWebExchange exchange, Model model) {
    Mono<CsrfToken> csrfToken = exchange.getAttribute(CsrfToken.class.getName());
    if (csrfToken != null) {
      return csrfToken.doOnSuccess(token ->
          model.addAttribute("_csrf", token)).thenReturn("register");
    }
    return Mono.just("register");
  }
}
