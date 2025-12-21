package com.anand.prohands.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.anand.prohands.data.SearchResultDto
import com.anand.prohands.data.local.RecentChatEntity
import com.anand.prohands.ui.components.AppHeader
import com.anand.prohands.ui.theme.ProColors
import com.anand.prohands.viewmodel.ChatListViewModel
import com.anand.prohands.viewmodel.ChatListViewModelFactory
import com.anand.prohands.viewmodel.SearchViewModel
import com.anand.prohands.viewmodel.SearchViewModelFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatListScreen(
    navController: NavController,
    currentUserId: String,
    chatListViewModel: ChatListViewModel = viewModel(factory = ChatListViewModelFactory()),
    searchViewModel: SearchViewModel = viewModel(factory = SearchViewModelFactory())
) {
    val recentChats by chatListViewModel.recentChats.collectAsState()
    
    val searchQuery by searchViewModel.searchQuery.collectAsState()
    val searchResults by searchViewModel.searchResults.collectAsState()
    val isSearching by searchViewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            AppHeader(
                title = "Chats",
                onBackClick = { navController.popBackStack() }
            )
        },
        containerColor = ProColors.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchViewModel.onSearchQueryChanged(it) },
                label = { Text("Search users...", color = ProColors.TextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = ProColors.Primary) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ProColors.Primary,
                    unfocusedBorderColor = ProColors.TextSecondary,
                    cursorColor = ProColors.Primary
                ),
                singleLine = true
            )

            if (searchQuery.isNotEmpty()) {
                // Show Search Results
                if (isSearching) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ProColors.Primary)
                    }
                } else if (searchResults.isEmpty()) {
                     Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No users found.", color = ProColors.TextSecondary)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(searchResults) { result ->
                            SearchResultItem(
                                searchResult = result, 
                                onClick = {
                                    result.profile.userId?.let { userId ->
                                        // Navigate to messages
                                        navController.navigate("messages/$userId")
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                // Show Recent Chats
                if (recentChats.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No recent chats.", color = ProColors.TextSecondary)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(recentChats) { chat ->
                            RecentChatItem(
                                chat = chat,
                                onClick = {
                                    navController.navigate("messages/${chat.recipientId}")
                                }
                            )
                            HorizontalDivider(color = ProColors.Surface, thickness = 1.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecentChatItem(
    chat: RecentChatEntity,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Profile Picture
        val painter = if (!chat.profilePictureUrl.isNullOrEmpty()) {
            rememberAsyncImagePainter(chat.profilePictureUrl)
        } else {
            rememberAsyncImagePainter("https://ui-avatars.com/api/?name=${chat.recipientName ?: "User"}&background=random")
        }

        Image(
            painter = painter,
            contentDescription = "Profile Picture",
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color.Gray),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = chat.recipientName ?: "Unknown User",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ProColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = formatTime(chat.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (chat.unreadCount > 0) ProColors.Primary else ProColors.TextSecondary
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = chat.lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ProColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                if (chat.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(ProColors.Primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (chat.unreadCount > 99) "99+" else chat.unreadCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = ProColors.OnPrimary,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val date = Date(timestamp)
    val now = Date()
    val diff = now.time - timestamp
    
    val oneDay = 24 * 60 * 60 * 1000
    
    return if (diff < oneDay && date.date == now.date) {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date)
    } else if (diff < 2 * oneDay) {
        "Yesterday"
    } else {
        SimpleDateFormat("MM/dd/yy", Locale.getDefault()).format(date)
    }
}
