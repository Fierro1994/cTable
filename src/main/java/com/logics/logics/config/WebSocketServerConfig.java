package com.logics.logics.config;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import reactor.core.publisher.Hooks;

@Configuration
@Slf4j
public class WebSocketServerConfig {
  @Bean
  public HandlerMapping webSocketMapping(ChatWebSocketHandler webSocketHandler) {
    return new SimpleUrlHandlerMapping(Map.of("/chat", webSocketHandler), -1);
  }

}