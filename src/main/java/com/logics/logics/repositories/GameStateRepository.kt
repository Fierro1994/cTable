package com.logics.logics.repositories

import com.logics.logics.entities.GameState
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Mono

interface GameStateRepository : ReactiveCrudRepository<GameState, Long> {
    fun findByRoomId(roomId: Long): Mono<GameState>
}