package com.logics.logics.repositories

import com.logics.logics.entities.GameRoom
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface GameRoomRepository : ReactiveCrudRepository<GameRoom, Long> {
    fun findByStatus(status: GameRoom.GameRoomStatus): Flux<GameRoom>
    fun findByIdAndStatus(id: Long, status: GameRoom.GameRoomStatus): Mono<GameRoom>
}