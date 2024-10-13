package com.logics.logics.entities

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("game_rooms")
data class GameRoom(
    @Id
    var id: Long? = null,
    var name: String? = null,
    var creatorId: String? = null,
    var maxPlayers: Int = 0,
    var category: String? = null,
    var playerIds: List<String>? = null,
    var status: GameRoomStatus? = null
) {
    enum class GameRoomStatus {
        WAITING, STARTING, IN_PROGRESS, FINISHED, DISBANDED
    }

    companion object {
        @JvmStatic
        fun builder() = Builder()
    }

    class Builder {
        private var id: Long? = null
        private var name: String? = null
        private var creatorId: String? = null
        private var maxPlayers: Int = 0
        private var category: String? = null
        private var playerIds: List<String>? = null
        private var status: GameRoomStatus? = null

        fun id(id: Long?) = apply { this.id = id }
        fun name(name: String?) = apply { this.name = name }
        fun creatorId(creatorId: String?) = apply { this.creatorId = creatorId }
        fun maxPlayers(maxPlayers: Int) = apply { this.maxPlayers = maxPlayers }
        fun category(category: String?) = apply { this.category = category }
        fun playerIds(playerIds: List<String>?) = apply { this.playerIds = playerIds }
        fun status(status: GameRoomStatus?) = apply { this.status = status }

        fun build() = GameRoom(id, name, creatorId, maxPlayers, category, playerIds, status)
    }
}