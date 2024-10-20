package com.logics.logics.config


import com.fasterxml.jackson.databind.ObjectMapper
import com.logics.logics.entities.ChatMessage
import com.logics.logics.services.GameService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.concurrent.ConcurrentHashMap

@Component
class GameChatWebSocketHandler(
    private val gameService: GameService,
    private val objectMapper: ObjectMapper
) : WebSocketHandler {

    private val sessions = ConcurrentHashMap<String, WebSocketSession>()
    private val logger = LoggerFactory.getLogger(GameChatWebSocketHandler::class.java)

    override fun handle(session: WebSocketSession): Mono<Void> {
        val roomId = session.handshakeInfo.uri.query?.split("&")
            ?.firstOrNull { it.startsWith("roomId") }
            ?.split("=")?.lastOrNull()?.toLongOrNull() ?: return Mono.empty()

        val username = session.handshakeInfo.uri.query?.split("&")
            ?.firstOrNull { it.startsWith("username") }
            ?.split("=")?.lastOrNull() ?: "unknown"
        sessions[username] = session

        val input = session.receive()
            .map { it.payloadAsText }
            .map { toChatMessage(it) }
            .flatMap { message ->
                handleChatMessage(message, roomId, username)
                    .doOnError { error ->
                        logger.error("Ошибка при обработке сообщения от $username: ${error.message}")
                    }
            }

        return session.send(input.map { session.textMessage("Сообщение доставлено") })
            .doOnError { error ->
                logger.error("Ошибка при отправке ответа пользователю $username: ${error.message}")
            }
            .doFinally {
                logger.info("Соединение WebSocket для $username закрыто")
                sessions.remove(username)
            }
    }

    private fun handleChatMessage(message: ChatMessage, roomId: Long, username: String): Mono<Void> {
        logger.info("Получено сообщение от $username: ${message.content}, isTeamMessage: ${message.isTeamMessage}")
        return if (message.isTeamMessage) {
            gameService.getTeamPlayers(roomId, username).flatMap { teamPlayers ->
                broadcastToPlayers(teamPlayers, message)
            }
        } else {
            gameService.getAllPlayers(roomId)
                .flatMap { allPlayers ->
                    broadcastToPlayers(allPlayers, message)
                }
        }
    }

    private fun broadcastToPlayers(players: List<String>, message: ChatMessage): Mono<Void> {
        val chatMessage = objectMapper.writeValueAsString(message)

        return Flux.fromIterable(players)
            .flatMap { player ->
                sessions[player]?.let { session ->
                    session.send(Mono.just(session.textMessage(chatMessage)))
                        .doOnError { error -> logger.error("Ошибка отправки сообщения пользователю $player: ${error.message}") }
                } ?: run {
                    logger.warn("Сессия для пользователя $player не найдена")
                    Mono.empty()
                }
            }
            .then()
    }

    private fun toChatMessage(message: String): ChatMessage {
        logger.info("Десериализация сообщения: $message")
        return objectMapper.readValue(message, ChatMessage::class.java)
    }
}