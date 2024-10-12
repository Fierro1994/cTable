package com.logics.logics.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.logics.logics.entities.Event
import com.logics.logics.entities.EventType
import com.logics.logics.entities.GameRoom
import com.logics.logics.services.GameRoomService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Component
class RoomWebSocketHandler(private val gameRoomService: GameRoomService) : WebSocketHandler {

    private val logger = LoggerFactory.getLogger(RoomWebSocketHandler::class.java)
    private val objectMapper = ObjectMapper()
    private val sessions = ConcurrentHashMap<String, WebSocketSession>()
    private val scheduler = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors())

    override fun handle(session: WebSocketSession): Mono<Void> {
        sessions[session.id] = session  // Добавляем сессию при подключении

        return session.receive()
            .map { it.payloadAsText }
            .map { toEvent(it) }
            .doOnNext { event -> handleEvent(event, session) }  // Вызов обработчика события
            .doFinally { sessions.remove(session.id) }  // Удаляем сессию при завершении
            .then()
    }
    private fun handleEvent(event: Event, session: WebSocketSession) {
        logger.info("Получено событие: {}", event)
        when (event.type) {
            EventType.ROOM_CREATED -> handleRoomCreated()
            EventType.ROOM_JOINED -> handleRoomJoined(event)
            EventType.ROOM_DISBANDED -> handleRoomDisbanded(event)
            EventType.ROOM_LIST_REQUEST -> handleRoomListRequest()
            else -> logger.warn("Неизвестный тип события: ${event.type}")
        }
    }
    private fun handleRoomCreated() {
        broadcastRoomUpdate()
    }

    private fun handleRoomDisbanded(event: Event) {
        val roomId = event.content?.toLongOrNull() ?: return
        gameRoomService.disbandRoom(roomId)
            .subscribe {
                broadcastRoomUpdate()
            }
    }

    private fun handleRoomListRequest() {
        broadcastRoomUpdate()
    }

    private fun broadcastRoomUpdate() {
        gameRoomService.getAvailableRooms()
            .collectList()
            .subscribe { rooms ->
                val updateEvent = Event(EventType.ROOM_LIST_UPDATE, objectMapper.writeValueAsString(rooms), "System")
                broadcastMessage(updateEvent)
            }
    }

    private fun handleRoomJoined(event: Event) {
        val roomId = event.content?.toLongOrNull() ?: return
        gameRoomService.findById(roomId)
            .doOnError { logger.error("Ошибка при поиске комнаты $roomId", it) }
            .subscribe { room ->
                try {
                    broadcastToRoom(room, Event(EventType.ROOM_UPDATE, objectMapper.writeValueAsString(room), "System"))
                    if (room.playerIds?.size == room.maxPlayers) {
                        startGameCountdown(room)
                    }
                } catch (e: Exception) {
                    logger.error("Ошибка при обработке присоединения к комнате", e)
                }
            }
    }

    private fun startGameCountdown(room: GameRoom) {
        val roomId = room.id ?: return
        scheduler.schedule({
            gameRoomService.startGame(roomId)
                .subscribe { updatedRoom ->
                    broadcastToRoom(updatedRoom, Event(EventType.GAME_STARTED, "Игра началась!", "System"))
                }
        }, 10, TimeUnit.SECONDS)
    }

    private fun broadcastToRoom(room: GameRoom, event: Event) {
        room.playerIds?.forEach { playerId ->
            sessions[playerId]?.let { session ->
                session.send(Mono.just(session.textMessage(toString(event)))).subscribe()
            }
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
            logger.error("Ошибка при разборе события комнаты: {}", message, e)
            Event(EventType.ERROR, "Ошибка при обработке сообщения комнаты", "System")
        }
    }

    private fun toString(event: Event): String {
        return objectMapper.writeValueAsString(event)
    }
}