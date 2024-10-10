package com.logics.logics.controllers;

import com.logics.logics.entities.User;
import com.logics.logics.services.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

@Controller
@RequestMapping("/")
public class HomeController {

  private final UserService userService;

  public HomeController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping
  public Mono<String> home(@AuthenticationPrincipal UserDetails userDetails, Model model) {
    return userService.getUserDetails(userDetails.getUsername())
        .doOnNext(user -> {
          model.addAttribute("username", user.getUsername());
          model.addAttribute("coins", user.getCoins());
        })
        .thenReturn("home");
  }
}
