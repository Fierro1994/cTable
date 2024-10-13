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
    private val userRooms = ConcurrentHashMap<String, Long>()

    override fun handle(session: WebSocketSession): Mono<Void> {
        val username = session.handshakeInfo.uri.query.split("=").lastOrNull() ?: "unknown"
        sessions[username] = session

        return session.receive()
            .map { it.payloadAsText }
            .map { toEvent(it) }
            .doOnNext { event -> handleEvent(event, username) }
            .doFinally {
                sessions.remove(username)
                userRooms.remove(username)?.let { roomId ->
                    gameRoomService.leaveRoom(roomId, username)
                        .subscribe { updatedRoom ->
                            broadcastRoomUpdate(updatedRoom)
                        }
                }
            }
            .then()
    }


    private fun handleEvent(event: Event, username: String) {
        logger.info("Получено событие: {}", event)
        when (event.type) {
            EventType.ROOM_CREATED -> handleRoomCreated(event, username)
            EventType.ROOM_JOINED -> handleRoomJoined(event, username)
            EventType.ROOM_DISBANDED -> handleRoomDisbanded(event)
            EventType.ROOM_LIST_REQUEST -> handleRoomListRequest()
            EventType.ROOM_LEFT -> handleRoomLeft(event, username)
            else -> logger.warn("Неизвестный тип события: ${event.type}")
        }
    }

    private fun handleRoomCreated(event: Event, username: String) {
        val roomId = event.content?.toLongOrNull() ?: return
        userRooms[username] = roomId
        broadcastRoomUpdate()
    }

    private fun handleRoomDisbanded(event: Event) {
        val roomId = event.content?.toLongOrNull() ?: return
        gameRoomService.findById(roomId)
            .flatMap { room ->
                gameRoomService.disbandRoom(roomId)
                    .thenReturn(room)
            }
            .subscribe { room ->
                val disbandEvent = Event(EventType.ROOM_DISBANDED, roomId.toString(), "System")
                room.playerIds?.forEach { playerId ->
                    userRooms.remove(playerId)
                    sessions[playerId]?.let { session ->
                        session.send(Mono.just(session.textMessage(objectMapper.writeValueAsString(disbandEvent))))
                            .subscribe()
                    }
                }
                broadcastRoomUpdate()
            }
    }

    private fun handleRoomListRequest() {
        broadcastRoomUpdate()
    }


    private fun handleRoomJoined(event: Event, username: String) {
        val roomId = event.content?.toLongOrNull() ?: return
        gameRoomService.joinRoom(roomId, username)
            .doOnError { logger.error("Ошибка при присоединении к комнате $roomId", it) }
            .subscribe { room ->
                userRooms[username] = roomId
                broadcastRoomUpdate(room)
                broadcastRoomListUpdate()

                // Отправляем подтверждение присоединения пользователю
                val joinConfirmation = Event(EventType.ROOM_JOIN_CONFIRMATION, objectMapper.writeValueAsString(room), "System")
                sessions[username]?.send(Mono.just(sessions[username]!!.textMessage(objectMapper.writeValueAsString(joinConfirmation))))?.subscribe()
            }
    }

    private fun handleRoomLeft(event: Event, username: String) {
        val roomId = event.content?.toLongOrNull() ?: return
        userRooms.remove(username)
        gameRoomService.leaveRoom(roomId, username)
            .flatMap { updatedRoom ->
                if (updatedRoom.status == GameRoom.GameRoomStatus.DISBANDED) {
                    gameRoomService.disbandRoom(roomId).thenReturn(updatedRoom)
                } else {
                    Mono.just(updatedRoom)
                }
            }
            .subscribe { updatedRoom ->
                broadcastRoomUpdate(updatedRoom)
                broadcastRoomListUpdate()
                val confirmationEvent = Event(EventType.ROOM_LEFT_CONFIRMATION, "", "System")
                sessions[username]?.send(Mono.just(sessions[username]!!.textMessage(objectMapper.writeValueAsString(confirmationEvent))))?.subscribe()
            }
    }
    private fun broadcastRoomListUpdate() {
        gameRoomService.getAvailableRooms()
            .collectList()
            .subscribe { rooms ->
                val updateEvent = Event(EventType.ROOM_LIST_UPDATE, objectMapper.writeValueAsString(rooms), "System")
                broadcastMessage(updateEvent)
            }
    }

    private fun broadcastRoomUpdate(room: GameRoom? = null) {
        if (room != null) {
            val updateEvent = Event(EventType.ROOM_UPDATE, objectMapper.writeValueAsString(room), "System")
            broadcastToRoom(room, updateEvent)
        }

        gameRoomService.getAvailableRooms()
            .collectList()
            .subscribe { rooms ->
                val updateEvent = Event(EventType.ROOM_LIST_UPDATE, objectMapper.writeValueAsString(rooms), "System")
                broadcastMessage(updateEvent)
            }
    }

    private fun broadcastToRoom(room: GameRoom, event: Event) {
        val message = toString(event)
        room.playerIds?.forEach { playerId ->
            sessions[playerId]?.let { session ->
                if (session.isOpen) {
                    session.send(Mono.just(session.textMessage(message)))
                        .doOnError { logger.error("Ошибка отправки сообщения", it) }
                        .subscribe()
                }
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
