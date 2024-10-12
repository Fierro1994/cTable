package com.logics.logics.dto

data class GameRoomRequest(
    var name: String? = null,
    var maxPlayers: Int = 0,
    var category: String? = null
)