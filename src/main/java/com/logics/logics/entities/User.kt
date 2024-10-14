package com.logics.logics.entities

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("users")
data class User(
    @Id
    var id: Long? = null,
    var username: String? = null,
    var password: String? = null,
    var coins: Int = 0,
    var status: String = "offline"

) {
    companion object {
        @JvmStatic
        fun builder() = Builder()
    }

    class Builder {
        private var id: Long? = null
        private var username: String? = null
        private var password: String? = null
        private var coins: Int = 0
        private var status: String = "offline"

        fun id(id: Long?) = apply { this.id = id }
        fun username(username: String?) = apply { this.username = username }
        fun password(password: String?) = apply { this.password = password }
        fun coins(coins: Int) = apply { this.coins = coins }
        fun status(status: String) = apply { this.status = status }
        fun build() = User(id, username, password, coins)
    }
}