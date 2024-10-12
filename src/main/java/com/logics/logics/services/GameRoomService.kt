package com.logics.logics.services;

import com.logics.logics.entities.GameRoom;
import com.logics.logics.repositories.GameRoomRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.UUID;

@Service
@Slf4j
public class GameRoomService {

  private final GameRoomRepository gameRoomRepository;

  public GameRoomService(GameRoomRepository gameRoomRepository) {
    this.gameRoomRepository = gameRoomRepository;
  }

  public Mono<GameRoom> createRoom(String creatorId, String name, int maxPlayers, String category) {
    GameRoom newRoom = GameRoom.builder()
        .name(name)
        .creatorId(creatorId)
        .maxPlayers(maxPlayers)
        .category(category)
        .playerIds(new ArrayList<>())
        .status(GameRoom.GameRoomStatus.WAITING)
        .build();
    log.info("Attempting to create new room: {}", newRoom);
    return gameRoomRepository.save(newRoom)
        .doOnSuccess(room -> log.info("Room created successfully: {}", room))
        .doOnError(error -> log.error("Error creating room: {}", error.getMessage()));
  }

  public Flux<GameRoom> getAvailableRooms() {
    return gameRoomRepository.findByStatus(GameRoom.GameRoomStatus.WAITING);
  }

  public Mono<GameRoom> joinRoom(Long roomId, String playerId) {
    return gameRoomRepository.findByIdAndStatus(roomId, GameRoom.GameRoomStatus.WAITING)
        .flatMap(room -> {
          if (room.getPlayerIds().size() < room.getMaxPlayers()) {
            room.getPlayerIds().add(playerId);
            if (room.getPlayerIds().size() == room.getMaxPlayers()) {
              room.setStatus(GameRoom.GameRoomStatus.STARTING);
            }
            return gameRoomRepository.save(room);
          } else {
            return Mono.error(new RuntimeException("Room is full"));
          }
        });
  }

  public Mono<GameRoom> leaveRoom(Long roomId, String playerId) {
    return gameRoomRepository.findById(roomId)
        .flatMap(room -> {
          room.getPlayerIds().remove(playerId);
          if (room.getPlayerIds().isEmpty()) {
            return gameRoomRepository.delete(room).then(Mono.empty());
          } else {
            return gameRoomRepository.save(room);
          }
        });
  }

  public Mono<GameRoom> startGame(Long roomId) {
    return gameRoomRepository.findById(roomId)
        .flatMap(room -> {
          room.setStatus(GameRoom.GameRoomStatus.IN_PROGRESS);
          return gameRoomRepository.save(room);
        });
  }

  public Mono<GameRoom> disbandRoom(Long roomId) {
    return gameRoomRepository.findById(roomId)
        .flatMap(room -> gameRoomRepository.delete(room).then(Mono.empty()));
  }

  public Mono<GameRoom> findById(Long roomId) {
    return gameRoomRepository.findById(roomId);
  }
}