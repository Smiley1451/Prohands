package com.anand.prohands.data.chat

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    @Transaction
    @Query("SELECT * FROM conversations ORDER BY lastMessageTimestamp DESC")
    fun getConversations(): Flow<List<ConversationWithParticipants>>

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getMessages(chatId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipant(participant: ParticipantEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipantCrossRef(crossRef: ConversationParticipantCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("UPDATE messages SET status = :status WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: MessageStatus)
    
    @Query("UPDATE messages SET status = :status WHERE chatId = :chatId AND status != 'READ' AND senderId != :currentUserId")
    suspend fun markMessagesAsRead(chatId: String, status: MessageStatus = MessageStatus.READ, currentUserId: String)

    @Query("UPDATE conversations SET lastMessage = :lastMessage, lastMessageTimestamp = :timestamp, unreadCounts = :unreadCounts WHERE chatId = :chatId")
    suspend fun updateConversationSnippet(chatId: String, lastMessage: String, timestamp: Long, unreadCounts: Map<String, Int>)

    @Query("UPDATE conversations SET unreadCounts = :unreadCounts WHERE chatId = :chatId")
    suspend fun updateUnreadCounts(chatId: String, unreadCounts: Map<String, Int>)
    
    @Query("UPDATE participants SET isOnline = :isOnline, lastSeen = :lastSeen WHERE userId = :userId")
    suspend fun updateUserPresence(userId: String, isOnline: Boolean, lastSeen: Long?)

    @Transaction
    suspend fun insertConversationWithParticipants(conversation: ConversationEntity, participants: List<ParticipantEntity>) {
        insertConversation(conversation)
        participants.forEach { participant ->
            insertParticipant(participant)
            insertParticipantCrossRef(ConversationParticipantCrossRef(conversation.chatId, participant.userId))
        }
    }
}
