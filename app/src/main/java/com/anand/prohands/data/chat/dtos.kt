package com.anand.prohands.data.chat

import com.google.gson.annotations.SerializedName

// From WebSocket /user/queue/conversations
data class ConversationUpdateDto(
    val chatId: String,
    val lastMessage: String?,
    val lastMessageTimestamp: Long?,
    val unreadCounts: Map<String, Int>,
    val participants: List<ParticipantDto>,
    val updatedAt: Long
)

// For initial load GET /api/chat/conversations
data class ConversationDto(
    val chatId: String,
    val participants: List<ParticipantDto>,
    val lastMessage: String?,
    val lastMessageTimestamp: Long?,
    val unreadCounts: Map<String, Int>,
    val updatedAt: Long
)

data class ParticipantDto(
    val userId: String,
    val name: String,
    val profilePictureUrl: String?
)

// For WebSocket /user/queue/messages and REST sync
data class MessageDto(
    val id: String,
    val chatId: String,
    val senderId: String,
    val recipientId: String? = null, // Can be null in incoming DTOs
    val content: String,
    val timestamp: Long,
    val status: MessageStatus,
    val type: MessageType = MessageType.TEXT,
    val metadata: Map<String, String>? = null
)

// For /user/queue/typing
data class TypingStatusDto(
    val chatId: String,
    val userId: String,
    val recipientId: String? = null,
    val isTyping: Boolean
)

// For /user/queue/status-updates
data class MessageStatusUpdateDto(
    val messageId: String,
    val chatId: String,
    val status: MessageStatus
)

// For /user/queue/read-receipt
data class ReadReceiptDto(
    val messageId: String,
    val readerId: String // The user who read the messages
)

// For STOMP /app/chat.delivered
data class DeliveryConfirmationDto(
    val messageId: String
)

// For /user/queue/presence
data class PresenceDto(
    val userId: String,
    val isOnline: Boolean,
    val lastSeen: Long?
)

// For POST /api/chat/media/sign
data class MediaSignRequest(
    val filename: String,
    val contentType: String
)

data class MediaSignResponse(
    val uploadUrl: String,
    val publicId: String,
    val apiKey: String,
    val signature: String,
    val timestamp: Long
)

// For GET /api/chat/sync
data class SyncResponse(
    val messages: List<MessageDto>,
    val statusUpdates: List<MessageStatusUpdateDto>
)
