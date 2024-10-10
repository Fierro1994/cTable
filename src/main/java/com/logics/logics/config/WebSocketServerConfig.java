package com.logics.logics.config;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;

@Configuration
@Slf4j
public class WebSocketServerConfig {

  @Bean
  public HandlerMapping webSocketMapping(ChatWebSocketHandler chatWebSocketHandler,
      RoomWebSocketHandler roomWebSocketHandler) {
    return new SimpleUrlHandlerMapping(Map.of(
        "/chat", chatWebSocketHandler,
        "/rooms", roomWebSocketHandler
    ), -1);
  }
}