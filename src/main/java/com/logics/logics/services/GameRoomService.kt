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
    private fun cleanId(id: String): String {

        val cleaned = id.replace(Regex("[{}\"\\\\/]"), "").trim()
        return if (cleaned == id) cleaned else cleanId(cleaned)
    }
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
        val cleanPlayerId = cleanId(playerId)
        return findById(roomId)
            .flatMap { room ->
                if (cleanPlayerId == room.creatorId) {
                    log.info("Игрок $cleanPlayerId уже является создателем комнаты $roomId, не добавляем повторно.")
                    return@flatMap Mono.just(room)
                }

                val playerIds = room.playerIds?.toMutableSet() ?: mutableSetOf()
                if (!playerIds.contains(cleanPlayerId)) {
                    if (playerIds.size < room.maxPlayers) {
                        playerIds.add(cleanPlayerId)
                        val updatedRoom = room.copy(
                            playerIds = playerIds.toList(),
                            status = if (playerIds.size == room.maxPlayers) GameRoom.GameRoomStatus.STARTING else room.status
                        )
                        return@flatMap gameRoomRepository.save(updatedRoom)
                            .doOnSuccess { savedRoom ->
                                log.info("Игрок $cleanPlayerId успешно добавлен в комнату $roomId. Текущие игроки: ${savedRoom.playerIds}")
                            }
                    } else {
                        return@flatMap Mono.error(RuntimeException("Комната заполнена"))
                    }
                }

                Mono.just(room)
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
            .map { room ->
                room.copy(
                    creatorId = cleanId(room.creatorId ?: ""),
                    playerIds = room.playerIds?.map { cleanId(it) }?.distinct()
                )
            }
            .doOnNext { gameRoom -> log.info("Найдена игровая комната: $gameRoom") }
            .doOnError { error -> log.error("Ошибка при поиске игровой комнаты: ${error.message}") }
    }
}