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


    @GetMapping("/{roomId}")
    fun getRoomById(@PathVariable roomId: Long): Mono<ResponseEntity<GameRoom>> {
        return gameRoomService.findById(roomId)
            .map { room -> ResponseEntity.ok(room) }
            .defaultIfEmpty(ResponseEntity.notFound().build())  // Возвращаем 404, если комната не найдена
    }

  }