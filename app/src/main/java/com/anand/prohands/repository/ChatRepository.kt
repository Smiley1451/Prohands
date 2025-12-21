package com.anand.prohands.repository

import android.util.Log
import com.anand.prohands.ProHandsApplication
import com.anand.prohands.data.chat.ChatMessage
import com.anand.prohands.data.chat.ChatMessageDto
import com.anand.prohands.data.chat.MediaSignature
import com.anand.prohands.data.chat.UserPresence
import com.anand.prohands.data.local.RecentChatEntity
import com.anand.prohands.network.ChatService
import com.anand.prohands.network.RetrofitClient
import com.google.gson.Gson
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.LifecycleEvent

object ChatRepository {

    private val chatService: ChatService = RetrofitClient.instance.create(ChatService::class.java)
    private var stompClient: StompClient? = null
    private val compositeDisposable = CompositeDisposable()
    private val gson = Gson()
    private var currentUserId: String? = null
    
    // DAO
    private val recentChatDao = ProHandsApplication.instance.database.recentChatDao()
    
    // Coroutine Scope for DB operations
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Reactive streams for UI
    private val _incomingMessages = MutableSharedFlow<ChatMessage>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val incomingMessages: SharedFlow<ChatMessage> = _incomingMessages

    private val _typingStatus = MutableSharedFlow<String>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val typingStatus: SharedFlow<String> = _typingStatus

    private val _readReceipts = MutableSharedFlow<ChatMessage>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val readReceipts: SharedFlow<ChatMessage> = _readReceipts
    
    private val _connectionStatus = MutableSharedFlow<Boolean>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val connectionStatus: SharedFlow<Boolean> = _connectionStatus
    
    // Expose recent chats
    val recentChats = recentChatDao.getAllRecentChats()

    fun initialize(userId: String) {
        if (currentUserId != userId) {
            disconnect()
            currentUserId = userId
            connect()
        } else if (stompClient == null || !stompClient!!.isConnected) {
            connect()
        }
    }

    private fun connect() {
        val userId = currentUserId
        if (userId.isNullOrEmpty()) {
            Log.e("ChatRepository", "Cannot connect: userId is null or empty.")
            return
        }
        
        // Construct WebSocket URL from Retrofit Base URL
        val httpUrl = RetrofitClient.BASE_URL
        val wsUrl = httpUrl.replace("https://", "wss://").replace("http://", "ws://")
        val stompUrl = "${wsUrl}ws?userId=$userId"
        
        Log.d("ChatRepository", "Attempting to connect to: $stompUrl")
        
        if (stompClient != null && stompClient!!.isConnected) return

        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, stompUrl).apply {
            withClientHeartbeat(30000)
            withServerHeartbeat(30000)
        }

        val lifecycleDisposable = stompClient!!.lifecycle()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { lifecycleEvent ->
                when (lifecycleEvent.type) {
                    LifecycleEvent.Type.OPENED -> {
                        Log.d("ChatRepository", "Stomp connection opened")
                        _connectionStatus.tryEmit(true)
                        subscribeToTopics()
                    }
                    LifecycleEvent.Type.ERROR -> {
                        Log.e("ChatRepository", "Stomp connection error", lifecycleEvent.exception)
                        _connectionStatus.tryEmit(false)
                    }
                    LifecycleEvent.Type.CLOSED -> {
                        Log.d("ChatRepository", "Stomp connection closed")
                        _connectionStatus.tryEmit(false)
                    }
                    else -> {}
                }
            }
        compositeDisposable.add(lifecycleDisposable)

        stompClient!!.connect()
    }

    private fun subscribeToTopics() {
        stompClient?.let { client ->
            try {
                // Messages
                val messagesDisp = client.topic("/user/queue/messages")
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ topicMessage ->
                        try {
                            val message = gson.fromJson(topicMessage.payload, ChatMessage::class.java)
                            _incomingMessages.tryEmit(message)
                            saveIncomingMessageToRecent(message)
                        } catch (e: Exception) {
                            Log.e("ChatRepository", "Error parsing message", e)
                        }
                    }, { e -> Log.e("ChatRepository", "Error on message subscription", e) })
                compositeDisposable.add(messagesDisp)

                // Typing
                val typingDisp = client.topic("/user/queue/typing")
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ topicMessage ->
                        _typingStatus.tryEmit(topicMessage.payload)
                    }, { e -> Log.e("ChatRepository", "Error on typing subscription", e) })
                compositeDisposable.add(typingDisp)

                // Read Receipts
                val readDisp = client.topic("/user/queue/read-receipt")
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ topicMessage ->
                        try {
                            val message = gson.fromJson(topicMessage.payload, ChatMessage::class.java)
                            _readReceipts.tryEmit(message)
                        } catch (e: Exception) {
                            Log.e("ChatRepository", "Error parsing read receipt", e)
                        }
                    }, { e -> Log.e("ChatRepository", "Error on read receipt subscription", e) })
                compositeDisposable.add(readDisp)
            } catch (e: IllegalStateException) {
                 Log.e("ChatRepository", "Failed to subscribe to topics, connection likely closed", e)
            }
        }
    }
    
    private fun saveIncomingMessageToRecent(message: ChatMessage) {
        repositoryScope.launch {
            try {
                // Determine the other user (if I am sender, it's recipient. If I am recipient, it's sender)
                val otherUserId = if (message.senderId == currentUserId) message.recipientId else message.senderId
                
                // Try to get existing chat to preserve name/pic
                val existingChat = recentChatDao.getRecentChat(message.chatId)
                
                val unreadIncrement = if (message.senderId != currentUserId) 1 else 0
                
                val recentChat = RecentChatEntity(
                    chatId = message.chatId,
                    recipientId = otherUserId,
                    recipientName = existingChat?.recipientName, // Keep existing name or null
                    profilePictureUrl = existingChat?.profilePictureUrl, // Keep existing pic or null
                    lastMessage = if (message.type == "IMAGE") "📷 Image" else message.content,
                    timestamp = System.currentTimeMillis(), // Or parse message.timestamp
                    unreadCount = (existingChat?.unreadCount ?: 0) + unreadIncrement
                )
                recentChatDao.insertOrUpdate(recentChat)
            } catch (e: Exception) {
                Log.e("ChatRepository", "Error saving recent chat", e)
            }
        }
    }

    fun sendMessage(messageDto: ChatMessageDto) {
        val json = gson.toJson(messageDto)
        stompClient?.send("/app/chat.send", json)?.subscribe(
            {
                 // Optimistically update recent chats
                 saveOptimisticMessageToRecent(messageDto)
            }, 
            { e -> Log.e("ChatRepository", "Error sending message", e) }
        )
    }
    
    private fun saveOptimisticMessageToRecent(messageDto: ChatMessageDto) {
        val userId = currentUserId ?: return
        repositoryScope.launch {
            try {
                val chatId = if (userId < messageDto.recipientId) "${userId}_${messageDto.recipientId}" else "${messageDto.recipientId}_${userId}"
                val existingChat = recentChatDao.getRecentChat(chatId)
                
                val recentChat = RecentChatEntity(
                    chatId = chatId,
                    recipientId = messageDto.recipientId,
                    recipientName = existingChat?.recipientName,
                    profilePictureUrl = existingChat?.profilePictureUrl,
                    lastMessage = if (messageDto.type == "IMAGE") "📷 Image" else messageDto.content,
                    timestamp = System.currentTimeMillis(),
                    unreadCount = existingChat?.unreadCount ?: 0 // Don't increment unread for own messages
                )
                recentChatDao.insertOrUpdate(recentChat)
            } catch (e: Exception) {
                Log.e("ChatRepository", "Error saving optimistic recent chat", e)
            }
        }
    }
    
    fun updateChatProfile(chatId: String, name: String, profileUrl: String?) {
        repositoryScope.launch {
            val existingChat = recentChatDao.getRecentChat(chatId)
            if (existingChat != null) {
                recentChatDao.insertOrUpdate(existingChat.copy(recipientName = name, profilePictureUrl = profileUrl))
            }
        }
    }

    fun sendTyping(recipientId: String) {
        stompClient?.send("/app/chat.typing", recipientId)?.subscribe(
            {}, 
            { e -> Log.e("ChatRepository", "Error sending typing status", e) }
        )
    }

    fun sendReadReceipt(messageId: String) {
        stompClient?.send("/app/chat.read", messageId)?.subscribe(
            {}, 
            { e -> Log.e("ChatRepository", "Error sending read receipt", e) }
        )
    }

    fun sendHeartbeat() {
        // Stomp client handles ping/pong automatically if configured, 
        // but explicit heartbeat to app/chat.heartbeat was requested
        stompClient?.send("/app/chat.heartbeat", "")?.subscribe(
            {}, 
            { e -> Log.e("ChatRepository", "Error sending heartbeat", e) }
        )
    }

    fun disconnect() {
        compositeDisposable.clear()
        try {
            stompClient?.disconnect()
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error disconnecting StompClient", e)
        }
        stompClient = null
        // Do not clear currentUserId here if we want to reconnect easily, 
        // but if it's a full logout, the caller should handle that.
    }
    
    fun clearSession() {
        disconnect()
        currentUserId = null
    }

    // REST API Calls
    suspend fun getChatHistory(chatId: String, page: Int = 0, size: Int = 50): Result<List<ChatMessage>> {
        return try {
            val response = chatService.getChatHistory(chatId, page, size)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Error fetching chat history: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserPresence(userId: String): Result<UserPresence> {
        return try {
            val response = chatService.getUserPresence(userId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error fetching user presence"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMediaSignature(userId: String): Result<MediaSignature> {
         return try {
            val response = chatService.getMediaSignature(userId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error fetching media signature"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
