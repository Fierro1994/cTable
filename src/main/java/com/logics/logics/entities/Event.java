package com.logics.logics.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Event {
  private EventType type;
  private String content;
  private String sender;
  public Event(String sender, String content, EventType type) {
    this.sender = sender;
    this.content = content;
    this.type = type;
  }
}

