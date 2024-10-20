package com.logics.logics.entities

data class ChatMessage(
    val type: String,
    val content: String,
    val isTeamMessage: Boolean,
    val sender: String
)