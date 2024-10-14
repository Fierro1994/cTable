package com.logics.logics.config;

import com.fasterxml.jackson.databind.ObjectMapper
import com.logics.logics.entities.Event
import com.logics.logics.entities.EventType
import com.logics.logics.entities.User
import com.logics.logics.services.UserService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono
import java.util.concurrent.ConcurrentHashMap

@Component
class PlayerWebSocketHandler(private val userService: UserService) : WebSocketHandler {

    private val logger = LoggerFactory.getLogger(PlayerWebSocketHandler::class.java)
    private val objectMapper = ObjectMapper()
    private val sessions = ConcurrentHashMap<String, WebSocketSession>()

    override fun handle(session: WebSocketSession): Mono<Void> {
        val username = session.handshakeInfo.uri.query.split("=").lastOrNull() ?: "unknown"
        sessions[username] = session
        userService.updateUserStatus(username, "online")
            .subscribe {
                broadcastPlayerListUpdate()
            }
        return session.receive()
            .map { it.payloadAsText }
            .map { toEvent(it) }
            .doOnNext { event -> handleEvent(event, username) }
            .doFinally {
                sessions.remove(username)
                userService.updateUserStatus(username, "offline")
                    .subscribe {
                        broadcastPlayerListUpdate()
                    }
            }
            .then()
    }

    private fun handleEvent(event: Event, username: String) {
        when (event.type) {
            EventType.PLAYER_LIST_REQUEST -> handlePlayerListRequest()
            EventType.PLAYER_SEARCH -> handlePlayerSearch(event.content ?: "")
            EventType.PLAYER_STATUS_CHANGE -> handlePlayerStatusChange(username, event.content ?: "offline")
            else -> logger.warn("Неизвестный тип события: ${event.type}")
        }
    }

    private fun handlePlayerListRequest() {
        broadcastPlayerListUpdate()
    }

    private fun handlePlayerSearch(searchQuery: String) {
        userService.searchUsers(searchQuery)
            .collectList()
            .subscribe { users ->
                val searchEvent = Event(EventType.PLAYER_SEARCH_RESULT, objectMapper.writeValueAsString(users), "System")
                broadcastMessage(searchEvent)
            }
    }

    private fun handlePlayerStatusChange(username: String, status: String) {
        userService.updateUserStatus(username, status)
            .subscribe {
                broadcastPlayerListUpdate()
            }
    }

    private fun broadcastPlayerListUpdate() {
        userService.getAllUsers()
            .collectList()
            .subscribe { users ->
                val playerListEvent = Event(EventType.PLAYER_LIST_UPDATE, objectMapper.writeValueAsString(users), "System")
                broadcastMessage(playerListEvent)
            }
    }

    private fun broadcastMessage(event: Event) {
        val message = toString(event)
        sessions.values.forEach { session ->
            if (session.isOpen) {
                session.send(Mono.just(session.textMessage(message)))
                    .doOnError { logger.error("Ошибка отправки сообщения", it) }
                    .subscribe()
            }
        }
    }

    private fun toEvent(message: String): Event {
        return try {
            objectMapper.readValue(message, Event::class.java)
        } catch (e: Exception) {
            logger.error("Ошибка при разборе события игрока: {}", message, e)
            Event(EventType.ERROR, "Ошибка при обработке сообщения игрока", "System")
        }
    }

    private fun toString(event: Event): String {
        return objectMapper.writeValueAsString(event)
    }
}
