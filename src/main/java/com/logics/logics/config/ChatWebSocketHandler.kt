package com.logics.logics.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.logics.logics.entities.Event
import com.logics.logics.entities.EventType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono
import java.util.concurrent.ConcurrentHashMap


@Component
open class ChatWebSocketHandler : WebSocketHandler {

    private val logger = LoggerFactory.getLogger(ChatWebSocketHandler::class.java)
    private val objectMapper = ObjectMapper()
    private val sessions = ConcurrentHashMap<String, WebSocketSession>()

    override fun handle(session: WebSocketSession): Mono<Void> {
        return session.receive()
            .map { it.payloadAsText }
            .map { toEvent(it) }
            .doOnNext { event ->
                logger.info("Received chat event: {}", event)
                val sender = event.sender
                if (sender.isNullOrBlank()) {
                    logger.warn("Event has null or blank sender: {}", event)
                    return@doOnNext
                }
                if (event.type == EventType.JOIN) {
                    sessions[sender] = session
                }
                broadcastMessage(event)
            }
            .doOnComplete {
                val sender = getSenderFromSession(session)
                sessions.remove(sender)
                val leaveEvent = Event(EventType.LEAVE, "$sender покинул чат", sender)
                broadcastMessage(leaveEvent)
                logger.info("Chat session completed for user: {}", sender)
            }
            .then()
    }

    private fun broadcastMessage(event: Event) {
        val message = toString(event)
        sessions.values.forEach { session ->
            session.send(Mono.just(session.textMessage(message))).subscribe()
        }
    }

    private fun getSenderFromSession(session: WebSocketSession): String {
        return sessions.entries.firstOrNull { it.value == session }?.key ?: "Unknown"
    }

    private fun toEvent(message: String): Event {
        return try {
            objectMapper.readValue(message, Event::class.java)
        } catch (e: Exception) {
            logger.error("Error parsing chat event: {}", message, e)
            Event(EventType.ERROR, "Error processing chat message", "System")
        }
    }

    private fun toString(event: Event): String {
        return objectMapper.writeValueAsString(event)
    }
}