package com.logics.logics.handlers

import com.fasterxml.jackson.databind.ObjectMapper
import com.logics.logics.entities.Event
import com.logics.logics.entities.EventType
import com.logics.logics.entities.GameState
import com.logics.logics.entities.ScoreUpdate
import com.logics.logics.services.GameRoomService
import com.logics.logics.services.GameService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.concurrent.ConcurrentHashMap

@Component
class GameWebSocketHandler(private val gameService: GameService, private val gameRoomService: GameRoomService) : WebSocketHandler {

    private val objectMapper = ObjectMapper()
    private val sessions = ConcurrentHashMap<String, WebSocketSession>()

    override fun handle(session: WebSocketSession): Mono<Void> {
        val username = session.handshakeInfo.uri.query?.split("=")?.lastOrNull() ?: "unknown"
        sessions[username] = session

        logger.info("Новое WebSocket соединение открыто для пользователя: $username")

        val input = session.receive()
            .doOnSubscribe { logger.info("Начало приема сообщений для пользователя: $username") }
            .map { it.payloadAsText }
            .map { toEvent(it) }
            .flatMap { event ->
                logger.info("Получено событие от $username: ${event.type}")
                handleEvent(event, username)
                    .onErrorResume { error ->
                        logger.error("Ошибка при обработке события от $username: ${error.message}", error)
                        Mono.empty()
                    }
            }
            .doOnComplete { logger.info("Завершен прием сообщений для пользователя: $username") }
            .doFinally {
                logger.info("WebSocket соединение закрыто для пользователя: $username")
                sessions.remove(username)
            }

        return session.send(input.map { session.textMessage("Событие обработано") })
            .doOnError { error -> logger.error("Ошибка при отправке ответа пользователю $username: ${error.message}", error) }
            .then()
    }

    private fun handleEvent(event: Event, username: String): Mono<Void> {
        return when (event.type) {
            EventType.START_GAME -> event.content?.let { startGame(it.toLong()) } ?: Mono.empty()
            EventType.GET_GAME_STATE -> event.content?.let { getGameState(it.toLong(), username) } ?: Mono.empty()
            EventType.UPDATE_SCORE -> updateScore(event.content, username)
            else -> {
                logger.warn("Неизвестный тип события: ${event.type}")
                Mono.empty()
            }
        }
    }

    private fun startGame(roomId: Long): Mono<Void> {
        return gameRoomService.findById(roomId)
            .flatMap { room ->
                val players = room.playerIds ?: emptyList()
                val (teamA, teamB) = divideIntoTeams(players)
                gameService.startGameWithTeams(roomId, teamA, teamB, room.category ?: "Неизвестная категория")
            }
            .flatMap { gameState ->
                val startEvent = Event(EventType.GAME_STARTED, objectMapper.writeValueAsString(gameState), "System")
                broadcastToPlayers(gameState.teamA + gameState.teamB, startEvent)
            }
            .doOnSuccess { logger.info("Игра успешно начата для комнаты $roomId") }
            .doOnError { error -> logger.error("Ошибка при начале игры для комнаты $roomId: ${error.message}", error) }
            .then()
    }
    private fun divideIntoTeams(players: List<String>): Pair<List<String>, List<String>> {
        val shuffledPlayers = players.shuffled()
        val mid = shuffledPlayers.size / 2
        return Pair(shuffledPlayers.take(mid), shuffledPlayers.drop(mid))
    }
    private fun getGameState(roomId: Long, username: String): Mono<Void> {
        return gameService.getGameState(roomId)
            .flatMap { gameState ->
                val stateEvent = Event(EventType.GAME_STATE, objectMapper.writeValueAsString(gameState), "System")
                sendToUser(username, stateEvent)
            }
            .doOnSuccess { logger.info("Состояние игры отправлено пользователю $username для комнаты $roomId") }
            .doOnError { error -> logger.error("Ошибка при получении состояния игры для комнаты $roomId: ${error.message}", error) }
            .then()
    }

    private fun updateScore(content: String?, username: String): Mono<Void> {
        if (content == null) return Mono.empty()
        val scoreUpdate = objectMapper.readValue(content, ScoreUpdate::class.java)
        return gameService.updateScore(scoreUpdate.roomId, scoreUpdate.teamAScore, scoreUpdate.teamBScore)
            .flatMap { gameState ->
                val scoreEvent = Event(EventType.SCORE_UPDATED, objectMapper.writeValueAsString(gameState), "System")
                broadcastToPlayers(gameState.teamA + gameState.teamB, scoreEvent)
            }
            .doOnSuccess { logger.info("Счет обновлен для комнаты ${scoreUpdate.roomId}") }
            .doOnError { error -> logger.error("Ошибка при обновлении счета для комнаты ${scoreUpdate.roomId}: ${error.message}", error) }
            .then()
    }

    private fun broadcastToPlayers(players: List<String>, event: Event): Mono<Void> {
        val message = objectMapper.writeValueAsString(event)
        return Flux.fromIterable(players)
            .flatMap { player -> sendToUser(player, message) }
            .then()
    }

    private fun sendToUser(username: String, event: Event): Mono<Void> {
        return sendToUser(username, objectMapper.writeValueAsString(event))
    }

    private fun sendToUser(username: String, message: String): Mono<Void> {
        return sessions[username]?.let { session ->
            if (session.isOpen) {
                session.send(Mono.just(session.textMessage(message)))
                    .doOnSuccess { logger.info("Сообщение успешно отправлено пользователю $username") }
                    .doOnError { error ->
                        logger.error("Ошибка отправки сообщения пользователю $username: ${error.message}", error)
                    }
                    .then()
            } else {
                logger.warn("Попытка отправить сообщение пользователю $username с закрытой сессией")
                Mono.empty()
            }
        } ?: run {
            logger.warn("Попытка отправить сообщение несуществующему пользователю $username")
            Mono.empty()
        }
    }

    private fun toEvent(message: String): Event {
        return try {
            objectMapper.readValue(message, Event::class.java)
        } catch (e: Exception) {
            logger.error("Ошибка при разборе события: {}", message, e)
            Event(EventType.ERROR, "Ошибка при обработке сообщения", "System")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GameWebSocketHandler::class.java)
    }
}
