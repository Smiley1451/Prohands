package com.anand.prohands.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.anand.prohands.data.chat.ChatItem
import com.anand.prohands.data.chat.ChatMessage
import com.anand.prohands.data.chat.ChatMessageDto
import com.anand.prohands.data.chat.UserPresence
import com.anand.prohands.repository.ChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class ChatViewModel(
    private val currentUserId: String,
    private val recipientId: String
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    
    // Transform messages to ChatItems with Date Separators
    val chatItems: StateFlow<List<ChatItem>> = _messages
        .map { messages ->
            val items = mutableListOf<ChatItem>()
            var lastDate = ""
            
            val sortedMessages = messages.sortedBy { it.timestamp }
            
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val displayFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
            
            sortedMessages.forEach { message ->
                val dateStr = try {
                     if (message.timestamp.length >= 10) message.timestamp.substring(0, 10) else ""
                } catch (e: Exception) { "" }
                
                if (dateStr.isNotEmpty() && dateStr != lastDate) {
                    val displayDate = try {
                        val date = dateFormat.parse(dateStr)
                        if (isToday(date)) "Today" 
                        else if (isYesterday(date)) "Yesterday"
                        else displayFormat.format(date!!)
                    } catch (e: Exception) { dateStr }
                    
                    items.add(ChatItem.DateSeparator(displayDate))
                    lastDate = dateStr
                }
                items.add(ChatItem.Message(message))
            }
            items.toList()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _typing = MutableStateFlow(false)
    val typing: StateFlow<Boolean> = _typing.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _recipientPresence = MutableStateFlow<UserPresence?>(null)
    val recipientPresence: StateFlow<UserPresence?> = _recipientPresence.asStateFlow()

    private var typingJob: Job? = null

    init {
        ChatRepository.initialize(currentUserId)
        
        loadHistory()
        observeIncomingMessages()
        observeTyping()
        observeConnection()
        fetchRecipientPresence()
    }

    private fun isToday(date: Date?): Boolean {
        if (date == null) return false
        val today = Date()
        val fmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        return fmt.format(date) == fmt.format(today)
    }
    
    private fun isYesterday(date: Date?): Boolean {
        if (date == null) return false
        val today = Date()
        val cal = java.util.Calendar.getInstance()
        cal.time = today
        cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
        val yesterday = cal.time
        val fmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        return fmt.format(date) == fmt.format(yesterday)
    }

    private fun observeConnection() {
        viewModelScope.launch {
            ChatRepository.connectionStatus.collectLatest { connected ->
                _isConnected.value = connected
            }
        }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            val chatId = getChatId(currentUserId, recipientId)
            val result = ChatRepository.getChatHistory(chatId)
            result.onSuccess { history ->
                _messages.value = history
            }
        }
    }

    private fun observeIncomingMessages() {
        viewModelScope.launch {
            ChatRepository.incomingMessages.collect { message ->
                if (message.chatId == getChatId(currentUserId, recipientId)) {
                    _messages.value = _messages.value + message
                    if (message.senderId == recipientId) {
                        ChatRepository.sendReadReceipt(message.messageId)
                    }
                }
            }
        }
    }

    private fun observeTyping() {
        viewModelScope.launch {
            ChatRepository.typingStatus.collect { senderId ->
                if (senderId == recipientId) {
                    _typing.value = true
                    typingJob?.cancel()
                    typingJob = launch {
                        delay(3000)
                        _typing.value = false
                    }
                }
            }
        }
    }

    fun sendMessage(content: String, type: String = "TEXT") {
        val messageDto = ChatMessageDto(
            recipientId = recipientId,
            type = type,
            content = content
        )
        ChatRepository.sendMessage(messageDto)
        
        val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())

        val optimisticMessage = ChatMessage(
            messageId = System.currentTimeMillis().toString(),
            chatId = getChatId(currentUserId, recipientId),
            senderId = currentUserId,
            recipientId = recipientId,
            type = type,
            content = content,
            status = "SENT",
            timestamp = now
        )
         _messages.value = _messages.value + optimisticMessage
    }

    fun sendTyping() {
        ChatRepository.sendTyping(recipientId)
    }
    
    private fun fetchRecipientPresence() {
        viewModelScope.launch {
            val result = ChatRepository.getUserPresence(recipientId)
            result.onSuccess { presence ->
                _recipientPresence.value = presence
            }
        }
    }
    
    private fun getChatId(user1: String, user2: String): String {
        return if (user1 < user2) "${user1}_${user2}" else "${user2}_${user1}"
    }
}

class ChatViewModelFactory(
    private val currentUserId: String,
    private val recipientId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(currentUserId, recipientId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
