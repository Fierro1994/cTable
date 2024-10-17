package com.logics.logics.handlers

import com.fasterxml.jackson.databind.ObjectMapper
import com.logics.logics.entities.Event
import com.logics.logics.entities.EventType
import com.logics.logics.services.GameService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono
import java.util.concurrent.ConcurrentHashMap

@Component
class GameWebSocketHandler(private val gameService: GameService) : WebSocketHandler {

    private val logger = LoggerFactory.getLogger(GameWebSocketHandler::class.java)
    private val objectMapper = ObjectMapper()
    private val sessions = ConcurrentHashMap<String, WebSocketSession>()

    override fun handle(session: WebSocketSession): Mono<Void> {
        val username = session.handshakeInfo.uri.query.split("=").lastOrNull() ?: "unknown"
        sessions[username] = session

        return session.receive()
            .map { it.payloadAsText }
            .map { toEvent(it) }
            .doOnNext { event -> handleEvent(event, username) }
            .doFinally { sessions.remove(username) }
            .then()
    }

    private fun handleEvent(event: Event, username: String) {
        when (event.type) {
            EventType.START_GAME -> event.content?.let { startGame(it.toLong()) }
            EventType.GET_GAME_STATE -> event.content?.let { sendGameState(it.toLong(), username) }
            else -> logger.warn("Неизвестный тип события: ${event.type}")
        }
    }

    private fun startGame(roomId: Long) {
        gameService.startGameWithTeams(roomId).subscribe { gameState ->
            val startEvent = Event(EventType.GAME_STARTED, objectMapper.writeValueAsString(gameState), "System")
            broadcastToPlayers(gameState.teamA + gameState.teamB, startEvent)
        }
    }

    private fun sendGameState(roomId: Long, username: String) {
        gameService.getGameState(roomId).subscribe { gameState ->
            val stateEvent = Event(EventType.GAME_STATE, objectMapper.writeValueAsString(gameState), "System")
            sessions[username]?.send(Mono.just(sessions[username]!!.textMessage(objectMapper.writeValueAsString(stateEvent))))?.subscribe()
        }
    }

    private fun broadcastToPlayers(players: List<String>, event: Event) {
        val message = objectMapper.writeValueAsString(event)
        players.forEach { player ->
            sessions[player]?.let { session ->
                if (session.isOpen) {
                    session.send(Mono.just(session.textMessage(message)))
                        .doOnError { logger.error("Ошибка отправки сообщения игроку $player", it) }
                        .subscribe()
                }
            }
        }
    }

    private fun toEvent(message: String): Event {
        return try {
            objectMapper.readValue(message, Event::class.java)
        } catch (e: Exception) {
            logger.error("Ошибка при разборе события игры: {}", message, e)
            Event(EventType.ERROR, "Ошибка при обработке сообщения игры", "System")
        }
    }
}