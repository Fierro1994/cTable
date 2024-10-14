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

        return gameRoomRepository.save(newRoom)
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
                    } else {
                        return@flatMap Mono.error(RuntimeException("Комната заполнена"))
                    }
                }

                Mono.just(room)
            }
            .doOnError { log.error("Ошибка при присоединении к комнате", it) }
    }
    fun cleanId(id: String): String {
        return id.replace(Regex("[{}\"\\\\/]"), "").trim() // Убираем фигурные скобки и любые другие нежелательные символы
    }

    fun leaveRoom(roomId: Long, playerId: String): Mono<GameRoom> {
        return gameRoomRepository.findById(roomId)
            .flatMap { room ->
                // Очищаем все идентификаторы игроков
                val playerIds: MutableList<String> = room.playerIds?.map { cleanId(it) }?.toMutableList() ?: mutableListOf()

                log.info("Текущие игроки до удаления: $playerIds")

                // Удаление игрока из списка
                if (playerIds.contains(cleanId(playerId))) {
                    playerIds.remove(cleanId(playerId))
                    log.info("Игрок $playerId удален из комнаты $roomId.")
                } else {
                    log.warn("Игрок $playerId не найден в списке игроков комнаты $roomId.")
                }

                // Обновляем статус комнаты, если список пуст
                val updatedRoom = if (playerIds.isEmpty()) {
                    room.copy(
                        playerIds = playerIds,
                        status = GameRoom.GameRoomStatus.DISBANDED // Если нет игроков, комната распускается
                    )
                } else {
                    room.copy(
                        playerIds = playerIds,
                        status = GameRoom.GameRoomStatus.WAITING // Комната остается в ожидании, если есть игроки
                    )
                }

                // Сохраняем изменения в базе данных
                gameRoomRepository.save(updatedRoom)
            }
            .doOnSuccess { updatedRoom ->
                log.info("Player $playerId успешно вышел из комнаты $roomId. Обновленные данные комнаты: $updatedRoom")
            }
            .doOnError { error ->
                log.error("Ошибка при выходе игрока $playerId из комнаты $roomId: ${error.message}")
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
            .doOnError { error -> log.error("Ошибка при поиске игровой комнаты: ${error.message}") }
    }
}