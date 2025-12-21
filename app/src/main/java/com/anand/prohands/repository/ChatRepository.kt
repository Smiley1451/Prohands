package com.anand.prohands.repository

import android.util.Log
import com.anand.prohands.data.chat.ChatMessage
import com.anand.prohands.data.chat.ChatMessageDto
import com.anand.prohands.data.chat.MediaSignature
import com.anand.prohands.data.chat.UserPresence
import com.anand.prohands.network.ChatService
import com.anand.prohands.network.RetrofitClient
import com.google.gson.Gson
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.LifecycleEvent

object ChatRepository {

    private val chatService: ChatService = RetrofitClient.instance.create(ChatService::class.java)
    private var stompClient: StompClient? = null
    private val compositeDisposable = CompositeDisposable()
    private val gson = Gson()
    private var currentUserId: String? = null

    // Reactive streams for UI
    private val _incomingMessages = MutableSharedFlow<ChatMessage>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val incomingMessages: SharedFlow<ChatMessage> = _incomingMessages

    private val _typingStatus = MutableSharedFlow<String>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val typingStatus: SharedFlow<String> = _typingStatus

    private val _readReceipts = MutableSharedFlow<ChatMessage>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val readReceipts: SharedFlow<ChatMessage> = _readReceipts
    
    private val _connectionStatus = MutableSharedFlow<Boolean>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val connectionStatus: SharedFlow<Boolean> = _connectionStatus

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
        val userId = currentUserId ?: return
        val baseUrl = "wss://unsufferably-heimish-tashina.ngrok-free.dev/ws?userId=$userId"
        
        if (stompClient != null && stompClient!!.isConnected) return

        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, baseUrl).apply {
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
            // Messages
            val messagesDisp = client.topic("/user/queue/messages")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ topicMessage ->
                    try {
                        val message = gson.fromJson(topicMessage.payload, ChatMessage::class.java)
                        _incomingMessages.tryEmit(message)
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
        }
    }

    fun sendMessage(messageDto: ChatMessageDto) {
        val json = gson.toJson(messageDto)
        stompClient?.send("/app/chat.send", json)?.subscribe()
    }

    fun sendTyping(recipientId: String) {
        stompClient?.send("/app/chat.typing", recipientId)?.subscribe()
    }

    fun sendReadReceipt(messageId: String) {
        stompClient?.send("/app/chat.read", messageId)?.subscribe()
    }

    fun sendHeartbeat() {
        // Stomp client handles ping/pong automatically if configured, 
        // but explicit heartbeat to app/chat.heartbeat was requested
        stompClient?.send("/app/chat.heartbeat", "")?.subscribe()
    }

    fun disconnect() {
        compositeDisposable.clear()
        stompClient?.disconnect()
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
