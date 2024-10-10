package com.logics.logics.controllers;

import com.logics.logics.entities.User;
import com.logics.logics.services.UserService;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/auth")
public class AuthController {

  private final UserService userService;

  public AuthController(UserService userService) {
    this.userService = userService;
  }

  @PostMapping("/register")
  public Mono<String> register(@ModelAttribute User user) {
    return userService.register(user.getUsername(), user.getPassword())
        .map(registeredUser -> "User registered successfully")
        .onErrorResume(e -> Mono.just(e.getMessage()));
  }
}
