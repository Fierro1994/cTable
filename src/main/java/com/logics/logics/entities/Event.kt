package com.logics.logics.entities

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class Event @JsonCreator constructor(
    @JsonProperty("type") var type: EventType?,
    @JsonProperty("content") var content: String?,
    @JsonProperty("sender") var sender: String?
) {
    constructor(sender: String, content: String, type: EventType) : this(type, content, sender)

    companion object {
        @JvmStatic
        fun builder() = Builder()
    }

    class Builder {
        private var type: EventType? = null
        private var content: String? = null
        private var sender: String? = null

        fun type(type: EventType) = apply { this.type = type }
        fun content(content: String) = apply { this.content = content }
        fun sender(sender: String) = apply { this.sender = sender }

        fun build() = Event(
            type ?: throw IllegalStateException("type must not be null"),
            content ?: throw IllegalStateException("content must not be null"),
            sender ?: throw IllegalStateException("sender must not be null")
        )
    }
}