package com.anand.prohands.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.anand.prohands.data.chat.*
import com.anand.prohands.network.WebSocketClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ChatViewModel(
    private val repository: ChatRepository,
    private val currentUserId: String,
    private val recipientId: String,
    private val chatId: String
) : ViewModel() {

    // Raw messages from DB
    private val _messagesFlow = repository.getMessagesForChat(chatId)

    // UI State: Messages grouped by date with separators
    val uiState: StateFlow<ChatUiState> = _messagesFlow.map { messages ->
        val grouped = mutableListOf<ChatItem>()
        var lastDate = ""
        
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val displayFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())

        messages.forEach { message ->
            val date = Date(message.timestamp)
            val dateStr = dateFormat.format(date)
            
            if (dateStr != lastDate) {
                val displayDate = when {
                    isToday(date) -> "Today"
                    isYesterday(date) -> "Yesterday"
                    else -> displayFormat.format(date)
                }
                grouped.add(ChatItem.DateSeparator(displayDate))
                lastDate = dateStr
            }
            grouped.add(ChatItem.Message(message))
        }
        ChatUiState(items = grouped)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChatUiState())

    private val _recipientPresence = MutableStateFlow<PresenceDto?>(null)
    val recipientPresence: StateFlow<PresenceDto?> = _recipientPresence.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()
    
    // For voice recording UI state
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private var typingJob: Job? = null

    init {
        repository.currentUserId = currentUserId
        observeTyping()
        observePresence()
        
        viewModelScope.launch {
            repository.markMessagesAsRead(chatId, currentUserId)
        }
    }

    private fun observeTyping() {
        viewModelScope.launch {
            WebSocketClient.events.collect { event ->
                if (event is TypingStatusDto && event.chatId == chatId && event.userId == recipientId) {
                    _isTyping.value = event.isTyping
                    if (event.isTyping) {
                         typingJob?.cancel()
                         typingJob = launch {
                             delay(3000)
                             _isTyping.value = false
                         }
                    }
                }
            }
        }
    }

    private fun observePresence() {
        viewModelScope.launch {
            WebSocketClient.events.collect { event ->
                 if (event is PresenceDto && event.userId == recipientId) {
                     _recipientPresence.value = event
                 }
            }
        }
    }

    fun sendMessage(content: String, type: MessageType = MessageType.TEXT) {
        viewModelScope.launch {
            repository.sendMessage(chatId, content, currentUserId, recipientId)
        }
    }
    
    fun sendMediaMessage(uri: Uri, type: MessageType) {
        // In a real app, you would upload the file to Cloudinary here using 
        // repository.signMedia() and then send the returned URL.
        // For this demo, we'll just send the URI string.
        sendMessage(uri.toString(), type)
    }

    fun onUserTyping() {
        viewModelScope.launch {
             WebSocketClient.sendTypingStatus(TypingStatusDto(chatId, currentUserId, recipientId, true))
        }
    }
    
    fun setRecordingState(isRecording: Boolean) {
        _isRecording.value = isRecording
    }

    private fun isToday(date: Date): Boolean {
        val today = Calendar.getInstance()
        val d = Calendar.getInstance().apply { time = date }
        return today.get(Calendar.YEAR) == d.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == d.get(Calendar.DAY_OF_YEAR)
    }

    private fun isYesterday(date: Date): Boolean {
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val d = Calendar.getInstance().apply { time = date }
        return yesterday.get(Calendar.YEAR) == d.get(Calendar.YEAR) &&
                yesterday.get(Calendar.DAY_OF_YEAR) == d.get(Calendar.DAY_OF_YEAR)
    }
}

data class ChatUiState(
    val items: List<ChatItem> = emptyList()
)

sealed class ChatItem {
    data class Message(val message: MessageEntity) : ChatItem()
    data class DateSeparator(val date: String) : ChatItem()
}

class ChatViewModelFactory(
    private val repository: ChatRepository,
    private val currentUserId: String,
    private val recipientId: String,
    private val chatId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(repository, currentUserId, recipientId, chatId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
