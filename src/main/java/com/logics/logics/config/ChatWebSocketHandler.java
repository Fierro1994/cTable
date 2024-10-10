package com.logics.logics.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logics.logics.entities.Event;
import com.logics.logics.entities.EventType;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ChatWebSocketHandler implements WebSocketHandler {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

  @Override
  public Mono<Void> handle(WebSocketSession session) {
    return session.receive()
        .map(WebSocketMessage::getPayloadAsText)
        .map(this::toEvent)
        .doOnNext(event -> {
          log.info("Received chat event: {}", event);
          if (event.getSender() == null || event.getSender().equals("null")) {
            log.warn("Event has null sender: {}", event);
            return;
          }
          if (event.getType() == EventType.JOIN) {
            sessions.put(event.getSender(), session);
          }
          broadcastMessage(event);
        })
        .doOnComplete(() -> {
          String sender = getSenderFromSession(session);
          sessions.remove(sender);
          Event leaveEvent = new Event(EventType.LEAVE, sender + " покинул чат", sender);
          broadcastMessage(leaveEvent);
          log.info("Chat session completed for user: {}", sender);
        })
        .then();
  }

  private void broadcastMessage(Event event) {
    String message = toString(event);
    sessions.values().forEach(session ->
        session.send(Mono.just(session.textMessage(message))).subscribe());
  }

  private String getSenderFromSession(WebSocketSession session) {
    return sessions.entrySet().stream()
        .filter(entry -> entry.getValue().equals(session))
        .map(Map.Entry::getKey)
        .findFirst()
        .orElse("Unknown");
  }

  @SneakyThrows
  private Event toEvent(String message) {
    return objectMapper.readValue(message, Event.class);
  }

  @SneakyThrows
  private String toString(Event event) {
    return objectMapper.writeValueAsString(event);
  }
}