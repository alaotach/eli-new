package com.alaotach.eli5.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<Message>,
    val stream: Boolean = false
)

@Serializable
data class StreamResponse(
    val choices: List<StreamChoice>
)

@Serializable
data class StreamChoice(
    val delta: Delta
)

@Serializable
data class Delta(
    val content: String? = null
)

@Serializable
data class Message(
    val role: String,
    val content: String
)

@Serializable
data class ChatResponse(
    val choices: List<Choice>
)

@Serializable
data class Choice(
    val message: Message
)
