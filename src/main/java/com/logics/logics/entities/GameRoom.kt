package com.logics.logics.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("game_rooms")
public class GameRoom {
  @Id
  private Long id;
  private String name;
  private String creatorId;
  private int maxPlayers;
  private String category;
  private List<String> playerIds;
  private GameRoomStatus status;

  public enum GameRoomStatus {
    WAITING, STARTING, IN_PROGRESS, FINISHED
  }
}
