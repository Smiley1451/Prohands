package com.anand.prohands.network

import android.annotation.SuppressLint
import android.util.Log
import com.anand.prohands.data.chat.*
import com.google.gson.Gson
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.LifecycleEvent

@SuppressLint("CheckResult")
object WebSocketClient {

    private var stompClient: StompClient? = null
    private val gson = Gson()
    private val compositeDisposable = CompositeDisposable()

    private val _events = MutableSharedFlow<Any>(replay = 1)
    val events = _events

    private val scope = CoroutineScope(Dispatchers.IO)

    fun connect(userId: String) {
        if (stompClient?.isConnected == true) return
        
        // Dispose any previous subscriptions before creating a new connection
        compositeDisposable.clear()

        val httpUrl = RetrofitClient.BASE_URL
        val wsUrl = httpUrl.replace("https://", "wss://").replace("http://", "ws://")
        val stompUrl = "${wsUrl}ws/websocket?userId=$userId"

        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, stompUrl)

        val lifecycleDisposable = stompClient!!.lifecycle().subscribe { lifecycleEvent ->
            when (lifecycleEvent.type) {
                LifecycleEvent.Type.OPENED -> {
                    Log.d("WebSocketClient", "Stomp connection opened")
                    subscribeToTopics()
                }
                LifecycleEvent.Type.ERROR -> {
                    Log.e("WebSocketClient", "Connection error", lifecycleEvent.exception)
                }
                LifecycleEvent.Type.CLOSED -> {
                    Log.d("WebSocketClient", "Stomp connection closed")
                    // Clear disposables on close to prevent memory leaks and redundant subscriptions
                    compositeDisposable.clear()
                }
                LifecycleEvent.Type.FAILED_SERVER_HEARTBEAT -> {
                    Log.e("WebSocketClient", "Failed server heartbeat")
                }
            }
        }
        compositeDisposable.add(lifecycleDisposable)
        
        // Configure heartbeats (in milliseconds). The library handles sending pings automatically.
        stompClient?.withServerHeartbeat(30000)?.withClientHeartbeat(30000)
        stompClient?.connect()
    }

    private fun subscribeToTopics() {
        stompClient?.let { client ->
            val topicDisposables = listOf(
                client.topic("/user/queue/conversations").subscribe({
                    handleSubscription(it.payload, ConversationUpdateDto::class.java)
                }, ::handleError),
                client.topic("/user/queue/messages").subscribe({
                    handleSubscription(it.payload, MessageDto::class.java)
                }, ::handleError),
                client.topic("/user/queue/typing").subscribe({
                    handleSubscription(it.payload, TypingStatusDto::class.java)
                }, ::handleError),
                client.topic("/user/queue/status-updates").subscribe({
                    handleSubscription(it.payload, MessageStatusUpdateDto::class.java)
                }, ::handleError),
                client.topic("/user/queue/read-receipt").subscribe({
                    handleSubscription(it.payload, ReadReceiptDto::class.java)
                }, ::handleError),
                client.topic("/user/queue/presence").subscribe({
                    handleSubscription(it.payload, PresenceDto::class.java)
                }, ::handleError)
            )
            compositeDisposable.addAll(*topicDisposables.toTypedArray())
        }
    }

    private fun <T> handleSubscription(payload: String, dtoClass: Class<T>) {
        try {
            val event = gson.fromJson(payload, dtoClass)
            event?.let { scope.launch { _events.emit(it) } }
        } catch (e: Exception) {
            Log.e("WebSocketClient", "GSON parse error for ${dtoClass.simpleName}", e)
        }
    }

    private fun handleError(throwable: Throwable) {
        Log.e("WebSocketClient", "Subscription error", throwable)
    }

    fun sendMessage(message: MessageDto) {
        val json = gson.toJson(message)
        stompClient?.send("/app/chat.send", json)?.let {
            compositeDisposable.add(it.subscribe())
        }
    }

    fun sendTypingStatus(typingStatus: TypingStatusDto) {
        val json = gson.toJson(typingStatus)
        stompClient?.send("/app/chat.typing", json)?.let {
            compositeDisposable.add(it.subscribe())
        }
    }

    fun sendReadReceipt(readReceipt: ReadReceiptDto) {
        val json = gson.toJson(readReceipt)
        stompClient?.send("/app/chat.read", json)?.let {
            compositeDisposable.add(it.subscribe())
        }
    }
    
    fun sendDeliveryConfirmation(confirmation: DeliveryConfirmationDto) {
        val json = gson.toJson(confirmation)
        stompClient?.send("/app/chat.delivered", json)?.let {
            compositeDisposable.add(it.subscribe())
        }
    }

    fun disconnect() {
        // This will close the connection and trigger the onClosed event, 
        // which in turn clears the compositeDisposable.
        stompClient?.disconnect()
        stompClient = null
    }
}
