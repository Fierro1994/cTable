package com.logics.logics.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.logics.logics.entities.Event
import com.logics.logics.entities.EventType
import com.logics.logics.services.QuestionService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono

@Component
class QuestWebSocketHandler(
    private val questionService: QuestionService,
    private val objectMapper: ObjectMapper
) : WebSocketHandler {

    private val logger = LoggerFactory.getLogger(QuestWebSocketHandler::class.java)

    override fun handle(session: WebSocketSession): Mono<Void> {
        return session.receive()
            .map { it.payloadAsText }
            .map { objectMapper.readValue(it, Event::class.java) }
            .flatMap { event -> handleEvent(event, session) }
            .doOnError { error -> logger.error("Ошибка WebSocket: ${error.message}") }
            .then()
    }

    private fun handleEvent(event: Event, session: WebSocketSession): Mono<Void> {
        return when (event.type) {
            EventType.GET_QUESTION -> sendRandomQuestion(session)
            EventType.ANSWER -> {
                logger.info("Получен ответ: ${event.content}")
                Mono.empty()  // Здесь можно добавить логику для обработки ответов
            }
            else -> {
                logger.warn("Неизвестный тип события: ${event.type}")
                Mono.empty()
            }
        }
    }

    private fun sendRandomQuestion(session: WebSocketSession): Mono<Void> {
        return questionService.getRandomQuestion()
            .flatMap { question ->
                val questionEvent = Event(EventType.QUESTION, objectMapper.writeValueAsString(question), "System")
                val message = objectMapper.writeValueAsString(questionEvent)
                session.send(Mono.just(session.textMessage(message)))
            }
    }
}