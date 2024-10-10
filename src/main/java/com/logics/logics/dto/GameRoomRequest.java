package com.logics.logics.dto;

import lombok.Data;

@Data
public class GameRoomRequest {
  private String name;
  private int maxPlayers;
  private String category;
}
