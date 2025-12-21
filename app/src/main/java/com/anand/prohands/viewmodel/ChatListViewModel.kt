package com.anand.prohands.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.anand.prohands.data.chat.ChatRepository
import com.anand.prohands.data.chat.ConversationWithParticipants
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatListViewModel(
    private val repository: ChatRepository,
    private val currentUserId: String
) : ViewModel() {

    val conversations: StateFlow<List<ConversationWithParticipants>> = repository.getConversations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        repository.currentUserId = currentUserId
        syncConversations()
    }

    private fun syncConversations() {
        viewModelScope.launch {
            repository.syncData()
        }
    }
}

class ChatListViewModelFactory(
    private val repository: ChatRepository,
    private val currentUserId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatListViewModel(repository, currentUserId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
