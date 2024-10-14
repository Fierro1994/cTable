
package com.logics.logics.entities

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDateTime

@Table("game_rooms")
data class GameRoom(
    @Id
    var id: Long? = null,
    var name: String,
    var creatorId: String? = null,
    var maxPlayers: Int,
    var category: String,
    var playerIds: List<String>? = null,
    var status: GameRoomStatus = GameRoomStatus.WAITING,
    @CreatedDate
    var createdAt: Timestamp? = null
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
        private var status: GameRoomStatus = GameRoomStatus.WAITING
        // Убрали createdAt из Builder, так как оно будет устанавливаться автоматически

        fun id(id: Long?) = apply { this.id = id }
        fun name(name: String) = apply { this.name = name }
        fun creatorId(creatorId: String?) = apply { this.creatorId = creatorId }
        fun maxPlayers(maxPlayers: Int) = apply { this.maxPlayers = maxPlayers }
        fun category(category: String) = apply { this.category = category }
        fun playerIds(playerIds: List<String>?) = apply { this.playerIds = playerIds }
        fun status(status: GameRoomStatus) = apply { this.status = status }
        // Убрали метод createdAt, так как оно будет устанавливаться автоматически

        fun build(): GameRoom {
            requireNotNull(name) { "Name must not be null" }
            requireNotNull(category) { "Category must not be null" }
            require(maxPlayers > 0) { "MaxPlayers must be greater than 0" }

            return GameRoom(
                id = id,
                name = name!!,
                creatorId = creatorId,
                maxPlayers = maxPlayers,
                category = category!!,
                playerIds = playerIds,
                status = status
                // createdAt не устанавливается здесь, оно будет установлено автоматически
            )
        }
    }
}