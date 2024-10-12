package com.logics.logics.services

import com.logics.logics.entities.GameRoom
import com.logics.logics.repositories.GameRoomRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class GameRoomService(private val gameRoomRepository: GameRoomRepository) {

    private val log = LoggerFactory.getLogger(GameRoomService::class.java)

    fun createRoom(creatorId: String, name: String, maxPlayers: Int, category: String): Mono<GameRoom> {
        val newRoom = GameRoom(
            name = name,
            creatorId = creatorId,
            maxPlayers = maxPlayers,
            category = category,
            playerIds = mutableListOf(creatorId),
            status = GameRoom.GameRoomStatus.WAITING
        )
        log.info("Попытка создания новой комнаты: {}", newRoom)
        return gameRoomRepository.save(newRoom)
            .doOnSuccess { room -> log.info("Комната успешно создана: {}", room) }
            .doOnError { error -> log.error("Ошибка при создании комнаты: {}", error.message) }
    }

    fun getAvailableRooms(): Flux<GameRoom> {
        return gameRoomRepository.findByStatus(GameRoom.GameRoomStatus.WAITING)
    }

    fun joinRoom(roomId: Long, playerId: String): Mono<GameRoom> {
        return gameRoomRepository.findByIdAndStatus(roomId, GameRoom.GameRoomStatus.WAITING)
            .flatMap { room ->
                val playerIds = room.playerIds?.toMutableList() ?: mutableListOf()
                if (playerIds.size < room.maxPlayers && !playerIds.contains(playerId)) {
                    playerIds.add(playerId)
                    room.playerIds = playerIds
                    if (playerIds.size == room.maxPlayers) {
                        room.status = GameRoom.GameRoomStatus.STARTING
                    }
                    gameRoomRepository.save(room)
                } else {
                    Mono.error(RuntimeException("Невозможно присоединиться к комнате"))
                }
            }
            .doOnError { log.error("Ошибка при присоединении к комнате", it) }
    }

    fun leaveRoom(roomId: Long, playerId: String): Mono<GameRoom> {
        return gameRoomRepository.findById(roomId)
            .flatMap { room ->
                val playerIds = room.playerIds?.toMutableList() ?: mutableListOf()
                playerIds.remove(playerId)
                room.playerIds = playerIds
                if (playerIds.isEmpty()) {
                    gameRoomRepository.delete(room).then(Mono.empty())
                } else {
                    gameRoomRepository.save(room)
                }
            }
    }

    fun startGame(roomId: Long): Mono<GameRoom> {
        return gameRoomRepository.findById(roomId)
            .flatMap { room ->
                room.status = GameRoom.GameRoomStatus.IN_PROGRESS
                gameRoomRepository.save(room)
            }
    }

    fun disbandRoom(roomId: Long): Mono<Void> {
        return gameRoomRepository.findById(roomId)
            .flatMap { room ->
                gameRoomRepository.delete(room)
            }
            .then()
            .doOnSuccess { log.info("Комната $roomId успешно распущена") }
            .doOnError { error -> log.error("Ошибка при роспуске комнаты $roomId: ${error.message}") }
    }

    fun findById(roomId: Long): Mono<GameRoom> {
        return gameRoomRepository.findById(roomId)
    }
}