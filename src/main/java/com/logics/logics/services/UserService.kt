package com.logics.logics.services;

import com.logics.logics.entities.User;
import com.logics.logics.repositories.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  public Mono<User> register(String username, String password) {
    return userRepository.findByUsername(username)
        .flatMap(existingUser -> Mono.error(new RuntimeException("User already exists")))
        .switchIfEmpty(
            Mono.defer(() -> {
              User newUser = User.builder()
                  .username(username)
                  .password(passwordEncoder.encode(password))
                  .coins(0)
                  .build();
              return userRepository.save(newUser);
            })
        )
        .cast(User.class);
  }

  public Mono<User> authenticate(String username, String password) {
    return userRepository.findByUsername(username)
        .flatMap(user -> {
          if (passwordEncoder.matches(password, user.getPassword())) {
            return Mono.just(user);
          } else {
            return Mono.error(new RuntimeException("Invalid credentials"));
          }
        });
  }

  public Mono<User> getUserDetails(String username) {
    return userRepository.findByUsername(username);
  }
}
