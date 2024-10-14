package com.logics.logics.repositories

import com.logics.logics.entities.User
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface UserRepository : ReactiveCrudRepository<User, String> {
    fun findByUsername(username: String): Mono<User>
    fun findByUsernameContainingIgnoreCase(username: String): Flux<User>

}