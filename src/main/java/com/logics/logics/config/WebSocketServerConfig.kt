package com.logics.logics.config

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping

@Configuration
open class WebSocketServerConfig {

    private val logger = LoggerFactory.getLogger(WebSocketServerConfig::class.java)

    @Bean
    open fun webSocketMapping(
        chatWebSocketHandler: ChatWebSocketHandler,
        roomWebSocketHandler: RoomWebSocketHandler
    ): HandlerMapping {
        logger.info("Configuring WebSocket mappings")
        return SimpleUrlHandlerMapping(
            mapOf(
                "/chat" to chatWebSocketHandler,
                "/rooms" to roomWebSocketHandler
            ),
            -1
        )
    }
}