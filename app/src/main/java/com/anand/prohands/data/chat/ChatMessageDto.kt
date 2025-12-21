package com.anand.prohands.data.chat

data class ChatMessageDto(
    val recipientId: String,
    val type: String = "TEXT", // Options: TEXT, IMAGE, VIDEO, VOICE
    val content: String, // Text or Cloudinary URL
    val metadata: Map<String, Any> = emptyMap(), // Optional: duration, size, dimensions
    val status: String = "SENT"
)
