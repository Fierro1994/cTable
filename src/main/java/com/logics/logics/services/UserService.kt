package com.logics.logics.services

import com.logics.logics.entities.User
import com.logics.logics.repositories.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {

    fun register(username: String, password: String): Mono<User> {
        return userRepository.findByUsername(username)
            .flatMap { Mono.error<User>(RuntimeException("User already exists")) }
            .switchIfEmpty(
                Mono.defer {
                    val newUser = User.builder()
                        .username(username)
                        .password(passwordEncoder.encode(password))
                        .coins(0)
                        .build()
                    userRepository.save(newUser)
                }
            )
            .cast(User::class.java)
    }

    fun authenticate(username: String, password: String): Mono<User> {
        return userRepository.findByUsername(username)
            .flatMap { user ->
                if (passwordEncoder.matches(password, user.password)) {
                    Mono.just(user)
                } else {
                    Mono.error(RuntimeException("Invalid credentials"))
                }
            }
    }

    fun getUserDetails(username: String): Mono<User> {
        return userRepository.findByUsername(username)
    }
}