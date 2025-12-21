package com.anand.prohands.data.chat

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import androidx.room.TypeConverters
import com.anand.prohands.data.chat.converters.MapConverter

@Entity(tableName = "conversations")
@TypeConverters(MapConverter::class)
data class ConversationEntity(
    @PrimaryKey val chatId: String,
    val lastMessage: String?,
    val lastMessageTimestamp: Long?,
    val unreadCounts: Map<String, Int> = emptyMap(),
    val participants: List<String> = emptyList(),
    val updatedAt: Long
)

@Entity(tableName = "messages")
@TypeConverters(MapConverter::class)
data class MessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val senderId: String,
    val content: String,
    val timestamp: Long,
    val status: MessageStatus,
    val type: MessageType = MessageType.TEXT,
    val metadata: Map<String, String>? = null
)

@Entity(tableName = "participants")
data class ParticipantEntity(
    @PrimaryKey val userId: String,
    val name: String,
    val profilePictureUrl: String?,
    val isOnline: Boolean = false,
    val lastSeen: Long? = null
)

data class ConversationWithParticipants(
    @Embedded val conversation: ConversationEntity,
    @Relation(
        parentColumn = "chatId",
        entityColumn = "userId",
        associateBy = Junction(ConversationParticipantCrossRef::class)
    )
    val participants: List<ParticipantEntity>
)

@Entity(primaryKeys = ["chatId", "userId"])
data class ConversationParticipantCrossRef(
    val chatId: String,
    val userId: String
)

enum class MessageStatus {
    PENDING, SENT, DELIVERED, READ, FAILED, DELETED
}

enum class MessageType {
    TEXT, IMAGE, VIDEO, VOICE, SYSTEM
}
