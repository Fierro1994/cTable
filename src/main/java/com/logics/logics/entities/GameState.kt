package com.logics.logics.entities

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("game_states")
data class GameState(
    @Id
    var id: Long? = null,
    var roomId: Long,
    var teamA: List<String>,
    var teamB: List<String>,
    var category: String,
    var teamAScore: Int = 0,
    var teamBScore: Int = 0,
    var status: String = "IN_PROGRESS"
)