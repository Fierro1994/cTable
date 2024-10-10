package com.logics.logics.controllers;


import com.logics.logics.entities.GameRoom;
import com.logics.logics.services.GameRoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/rooms")
public class GameRoomController {

  private final GameRoomService gameRoomService;

  public GameRoomController(GameRoomService gameRoomService) {
    this.gameRoomService = gameRoomService;
  }

  @PostMapping
  public Mono<ResponseEntity<GameRoom>> createRoom(@AuthenticationPrincipal UserDetails userDetails,
      @RequestParam String name,
      @RequestParam int maxPlayers,
      @RequestParam String category) {
    return gameRoomService.createRoom(userDetails.getUsername(), name, maxPlayers, category)
        .map(ResponseEntity::ok);
  }

  @GetMapping
  public Flux<GameRoom> getAvailableRooms() {
    return gameRoomService.getAvailableRooms();
  }

  @PostMapping("/{roomId}/join")
  public Mono<ResponseEntity<GameRoom>> joinRoom(@AuthenticationPrincipal UserDetails userDetails,
      @PathVariable String roomId) {
    return gameRoomService.joinRoom(roomId, userDetails.getUsername())
        .map(ResponseEntity::ok)
        .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().build()));
  }

  @PostMapping("/{roomId}/leave")
  public Mono<ResponseEntity<Void>> leaveRoom(@AuthenticationPrincipal UserDetails userDetails,
      @PathVariable String roomId) {
    return gameRoomService.leaveRoom(roomId, userDetails.getUsername())
        .then(Mono.just(ResponseEntity.ok().<Void>build()));
  }

  @PostMapping("/{roomId}/start")
  public Mono<ResponseEntity<GameRoom>> startGame(@PathVariable String roomId) {
    return gameRoomService.startGame(roomId)
        .map(ResponseEntity::ok)
        .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().build()));
  }

  @PostMapping("/{roomId}/disband")
  public Mono<ResponseEntity<Void>> disbandRoom(@PathVariable String roomId) {
    return gameRoomService.disbandRoom(roomId)
        .then(Mono.just(ResponseEntity.ok().<Void>build()));
  }
}

