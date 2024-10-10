package com.logics.logics.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logics.logics.entities.Event;
import com.logics.logics.entities.EventType;
import com.logics.logics.entities.GameRoom;
import com.logics.logics.services.GameRoomService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RoomWebSocketHandler implements WebSocketHandler {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
  private final GameRoomService gameRoomService;
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

  public RoomWebSocketHandler(GameRoomService gameRoomService) {
    this.gameRoomService = gameRoomService;
  }

  @Override
  public Mono<Void> handle(WebSocketSession session) {
    return session.receive()
        .map(WebSocketMessage::getPayloadAsText)
        .map(this::toEvent)
        .doOnNext(event -> {
          log.info("Received room event: {}", event);
          if (event.getSender() == null || event.getSender().equals("null")) {
            log.warn("Event has null sender: {}", event);
            return;
          }
          if (event.getType() == EventType.ROOM_CREATED) {
            handleRoomCreated(event);
          } else if (event.getType() == EventType.ROOM_JOINED) {
            handleRoomJoined(event);
          } else if (event.getType() == EventType.GAME_STARTED) {
            handleGameStarted(event);
          }
        })
        .doOnComplete(() -> {
          String sender = getSenderFromSession(session);
          sessions.remove(sender);
          Event leaveEvent = new Event(EventType.LEAVE, sender + " покинул комнату", sender);
          broadcastMessage(leaveEvent);
          log.info("WebSocket session for room completed for user: {}", sender);
        })
        .then();
  }

  private void handleRoomCreated(Event event) {
    gameRoomService.getAvailableRooms()
        .collectList()
        .subscribe(rooms -> {
          try {
            broadcastRoomUpdate(rooms);
          } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
          }
        });
  }

  private void handleRoomJoined(Event event) {
    String roomId = event.getContent();
    gameRoomService.findById(roomId)
        .subscribe(room -> {
          try {
            broadcastToRoom(room, new Event(EventType.ROOM_UPDATE, objectMapper.writeValueAsString(room), "System"));
          } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
          }
          if (room.getPlayerIds().size() == room.getMaxPlayers()) {
            startGameCountdown(room);
          }
        });
  }

  private void handleGameStarted(Event event) {
    String roomId = event.getContent();
    gameRoomService.startGame(roomId)
        .subscribe(room -> broadcastToRoom(room, new Event(EventType.GAME_STARTED, "Игра началась!", "System")));
  }

  private void startGameCountdown(GameRoom room) {
    scheduler.schedule(() -> {
      gameRoomService.startGame(room.getId())
          .subscribe(updatedRoom -> broadcastToRoom(updatedRoom, new Event(EventType.GAME_STARTED, "Игра началась!", "System")));
    }, 10, TimeUnit.SECONDS);
  }

  private void broadcastToRoom(GameRoom room, Event event) {
    room.getPlayerIds().forEach(playerId -> {
      WebSocketSession session = sessions.get(playerId);
      if (session != null) {
        session.send(Mono.just(session.textMessage(toString(event)))).subscribe();
      }
    });
  }

  private void broadcastRoomUpdate(List<GameRoom> rooms) throws JsonProcessingException {
    Event updateEvent = new Event(EventType.ROOM_LIST_UPDATE, objectMapper.writeValueAsString(rooms), "System");
    broadcastMessage(updateEvent);
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
    try {
      Event event = objectMapper.readValue(message, Event.class);
      if (event.getSender() == null || event.getSender().isEmpty()) {
        log.warn("Received event with null or empty sender: {}", message);
      }
      if (event.getContent() == null || event.getContent().isEmpty()) {
        log.warn("Received event with null or empty content: {}", message);
        if (event.getType() == EventType.JOIN) {
          event.setContent(event.getSender() + " присоединился к комнате");
        }
      }
      return event;
    } catch (Exception e) {
      log.error("Error parsing room event: {}", message, e);
      return new Event(EventType.ERROR, "Error processing room message", "System");
    }
  }

  @SneakyThrows
  private String toString(Event event) {
    return objectMapper.writeValueAsString(event);
  }
}