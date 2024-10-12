package com.logics.logics.repositories;

import com.logics.logics.entities.GameRoom;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface GameRoomRepository extends ReactiveCrudRepository<GameRoom, Long> {
  Flux<GameRoom> findByStatus(GameRoom.GameRoomStatus status);
  Mono<GameRoom> findByIdAndStatus(Long id, GameRoom.GameRoomStatus status);
}

