package com.anand.prohands.data.chat

import android.util.Log
import com.anand.prohands.network.ChatService
import com.anand.prohands.network.WebSocketClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class ChatRepository(
    private val chatDao: ChatDao, 
    private val chatService: ChatService
) {

    var currentUserId: String? = null
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    init {
        repositoryScope.launch {
            WebSocketClient.events.collect { event ->
                handleWebSocketEvent(event)
            }
        }
    }

    // --- Flows for UI --- //
    fun getConversations(): Flow<List<ConversationWithParticipants>> = chatDao.getConversations()

    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>> = chatDao.getMessages(chatId)

    // --- Network and DB Operations --- //

    suspend fun syncData() {
        val lastTimestamp = chatDao.getConversations().firstOrNull()?.firstOrNull()?.conversation?.lastMessageTimestamp ?: 0L
        val since = if (lastTimestamp == 0L) {
            "1970-01-01T00:00:00Z"
        } else {
            Instant.ofEpochMilli(lastTimestamp).atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT)
        }
        try {
            val response = chatService.sync(since)
            if (response.isSuccessful) {
                val syncData = response.body()
                syncData?.messages?.forEach { messageDto ->
                    chatDao.insertMessage(messageDto.toEntity())
                }
                syncData?.statusUpdates?.forEach { statusUpdate ->
                    chatDao.updateMessageStatus(statusUpdate.messageId, statusUpdate.status)
                }
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Sync failed", e)
        }
    }

    suspend fun sendMessage(chatId: String, content: String, senderId: String, recipientId: String, type: MessageType = MessageType.TEXT) {
        val tempId = "temp_${System.currentTimeMillis()}"
        val messageEntity = MessageEntity(
            id = tempId,
            chatId = chatId,
            senderId = senderId,
            content = content,
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.PENDING,
            type = type
        )
        chatDao.insertMessage(messageEntity)

        // Create DTO with SENT status and recipientId for the backend
        val dto = messageEntity.toDto().copy(
            status = MessageStatus.SENT,
            recipientId = recipientId
        )
        WebSocketClient.sendMessage(dto)
    }
    
    suspend fun markMessagesAsRead(chatId: String, currentUserId: String) {
        chatDao.markMessagesAsRead(chatId, currentUserId = currentUserId)
        val conversation = chatDao.getConversations().firstOrNull()?.find { it.conversation.chatId == chatId }
        conversation?.let {
            val newMap = it.conversation.unreadCounts.toMutableMap()
            newMap[currentUserId] = 0
            chatDao.updateUnreadCounts(chatId, newMap)
        }
    }

    private suspend fun handleWebSocketEvent(event: Any) {
        when (event) {
            is ConversationUpdateDto -> {
                 chatDao.updateConversationSnippet(
                    event.chatId,
                    event.lastMessage ?: "",
                    event.lastMessageTimestamp ?: 0L,
                    event.unreadCounts
                )
            }
            is MessageDto -> {
                chatDao.insertMessage(event.toEntity())
                if (event.senderId != currentUserId) {
                     WebSocketClient.sendDeliveryConfirmation(DeliveryConfirmationDto(event.id))
                }
            }
            is MessageStatusUpdateDto -> {
                chatDao.updateMessageStatus(event.messageId, event.status)
            }
            is ReadReceiptDto -> {
                chatDao.updateMessageStatus(event.messageId, MessageStatus.READ)
            }
            is PresenceDto -> {
                chatDao.updateUserPresence(event.userId, event.isOnline, event.lastSeen)
            }
        }
    }
    
    private fun MessageDto.toEntity() = MessageEntity(id, chatId, senderId, content, timestamp, status, type, metadata)
    private fun MessageEntity.toDto() = MessageDto(id, chatId, senderId, null, content, timestamp, status, type, metadata)

}
