package com.logics.logics.controllers

import com.logics.logics.dto.GameRoomRequest
import com.logics.logics.entities.GameRoom
import com.logics.logics.services.GameRoomService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/rooms")
class GameRoomController(private val gameRoomService: GameRoomService) {

    private val logger = LoggerFactory.getLogger(GameRoomController::class.java)

    @PostMapping
    fun createRoom(
        @AuthenticationPrincipal userDetails: UserDetails,
        @RequestBody request: GameRoomRequest
    ): Mono<ResponseEntity<GameRoom>> {
        return gameRoomService.createRoom(
            userDetails.username,
            request.name ?: "Комната без названия",
            request.maxPlayers,
            request.category ?: "Без категории"
        ).map { ResponseEntity.ok(it) }
            .doOnError { e -> logger.error("Ошибка при создании комнаты: {}", e.message) }
            .onErrorResume { Mono.just(ResponseEntity.badRequest().build()) }
    }

    @GetMapping
    fun getAvailableRooms(): Flux<GameRoom> {
        return gameRoomService.getAvailableRooms()
    }

    @PostMapping("/{roomId}/join")
    fun joinRoom(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable roomId: Long
    ): Mono<ResponseEntity<GameRoom>> {
        return gameRoomService.joinRoom(roomId, userDetails.username)
            .map { ResponseEntity.ok(it) }
            .onErrorResume { Mono.just(ResponseEntity.badRequest().build()) }
    }

    @PostMapping("/{roomId}/leave")
    fun leaveRoom(
        @AuthenticationPrincipal userDetails: UserDetails,
        @PathVariable roomId: Long
    ): Mono<ResponseEntity<Void>> {
        return gameRoomService.leaveRoom(roomId, userDetails.username)
            .then(Mono.just(ResponseEntity.ok().build()))
    }

    @PostMapping("/{roomId}/start")
    fun startGame(@PathVariable roomId: Long): Mono<ResponseEntity<GameRoom>> {
        return gameRoomService.startGame(roomId)
            .map { ResponseEntity.ok(it) }
            .onErrorResume { Mono.just(ResponseEntity.badRequest().build()) }
    }

    @PostMapping("/{roomId}/disband")
    fun disbandRoom(@PathVariable roomId: Long): Mono<ResponseEntity<Void>> {
        return gameRoomService.disbandRoom(roomId)
            .then(Mono.just(ResponseEntity.ok().build<Void>()))
            .doOnSuccess { logger.info("Комната $roomId успешно распущена") }
            .onErrorResume { error ->
                logger.error("Ошибка при роспуске комнаты $roomId: ${error.message}")
                Mono.just(ResponseEntity.badRequest().build<Void>())
            }
    }
}