package com.logics.logics.repositories;

import com.logics.logics.entities.User;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface UserRepository extends ReactiveCrudRepository<User, String> {
  Mono<User> findByUsername(String username);
}