package com.logics.logics.handlers

import com.fasterxml.jackson.databind.ObjectMapper
import com.logics.logics.entities.Event
import com.logics.logics.entities.EventType
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
class GameWebSocketHandler(
    private val gameService: GameService,
    private val gameRoomService: GameRoomService
) : WebSocketHandler {

    private val objectMapper = ObjectMapper()
    private val sessions = ConcurrentHashMap<String, WebSocketSession>()
    private val logger = LoggerFactory.getLogger(GameWebSocketHandler::class.java)

    override fun handle(session: WebSocketSession): Mono<Void> {
        val username = session.handshakeInfo.uri.query?.split("=")?.lastOrNull() ?: "unknown"
        sessions[username] = session
        val input = session.receive()
            .map { it.payloadAsText }
            .map { toEvent(it) }
            .flatMap { event ->
                handleEvent(event, username)
                    .onErrorResume { error ->
                        logger.error("Ошибка при обработке события от $username: ${error.message}", error)
                        Mono.empty()
                    }
            }
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
            EventType.GET_GAME_STATE -> event.content?.let { getGameState(it.toLong()) } ?: Mono.empty()
            else -> {
                logger.warn("Неизвестный тип события: ${event.type}")
                Mono.empty()
            }
        }
    }
    private fun getGameState(roomId: Long): Mono<Void> {
        return gameService.getGameState(roomId).flatMap { gameState ->
            // Отправляем состояние игры всем игрокам
            val gameStateEvent = Event(EventType.GAME_STATE, objectMapper.writeValueAsString(gameState), "System")
            broadcastToRoom(gameState.teamA + gameState.teamB, gameStateEvent)
        }
    }
    private fun startGame(roomId: Long): Mono<Void> {
        return gameService.startGame(roomId)
            .flatMap { gameState ->
                val startEvent = Event(EventType.GAME_STARTED, objectMapper.writeValueAsString(gameState), "System")
                broadcastToRoom(gameState.teamA + gameState.teamB, startEvent)
            }
            .then()
    }

    private fun broadcastToRoom(players: List<String>, event: Event): Mono<Void> {
        val message = objectMapper.writeValueAsString(event)

        // Преобразуем список игроков в Flux и отправляем сообщение каждому
        return Flux.fromIterable(players)
            .flatMap { player -> sendToUser(player, message) }
            .then() // Возвращаем Mono<Void> после завершения всех операций
    }

    private fun sendToUser(username: String, message: String): Mono<Void> {
        return sessions[username]?.let { session ->
            if (session.isOpen) {
                session.send(Mono.just(session.textMessage(message)))
                    .then()
            } else {
                Mono.empty()
            }
        } ?: Mono.empty()
    }

    private fun toEvent(message: String): Event {
        return objectMapper.readValue(message, Event::class.java)
    }
}