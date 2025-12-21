package com.anand.prohands.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddComment
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
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.anand.prohands.data.chat.ConversationWithParticipants
import com.anand.prohands.ui.components.AppHeader
import com.anand.prohands.ui.theme.ProColors
import com.anand.prohands.viewmodel.ChatListViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatListScreen(
    navController: NavController,
    currentUserId: String,
    viewModel: ChatListViewModel
) {
    val conversations by viewModel.conversations.collectAsState()

    Scaffold(
        topBar = { 
            AppHeader(
                title = "ProHands Chat"
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("search") }, // Assuming search screen is for finding users
                containerColor = ProColors.Primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.AddComment, contentDescription = "New Chat")
            }
        },
        containerColor = ProColors.Background
    ) { padding ->
        if (conversations.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No conversations yet.\nStart a new chat!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = ProColors.TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                items(conversations) { conversation ->
                    val otherParticipant = conversation.participants.find { it.userId != currentUserId }
                    if (otherParticipant != null) {
                        ConversationItem(conversation = conversation, otherParticipant = otherParticipant, currentUserId = currentUserId) {
                            navController.navigate("chat/${otherParticipant.userId}")
                        }
                        Divider(color = Color.LightGray.copy(alpha = 0.2f), thickness = 0.5.dp, modifier = Modifier.padding(start = 80.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ConversationItem(
    conversation: ConversationWithParticipants,
    otherParticipant: com.anand.prohands.data.chat.ParticipantEntity,
    currentUserId: String,
    onClick: () -> Unit
) {
    val unreadCount = conversation.conversation.unreadCounts[currentUserId] ?: 0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Profile Picture with Online Status
        Box {
            val painter = if (!otherParticipant.profilePictureUrl.isNullOrEmpty()) {
                rememberAsyncImagePainter(otherParticipant.profilePictureUrl)
            } else {
                rememberAsyncImagePainter("https://ui-avatars.com/api/?name=${otherParticipant.name}&background=random")
            }
            
            Image(
                painter = painter,
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray),
                contentScale = ContentScale.Crop
            )
            
            if (otherParticipant.isOnline) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color.Green)
                        .align(Alignment.BottomEnd)
                        .padding(2.dp)
                        .background(ProColors.Background, CircleShape) // Border effect
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = otherParticipant.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ProColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                conversation.conversation.lastMessageTimestamp?.let {
                    Text(
                        text = formatTime(it),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (unreadCount > 0) ProColors.Primary else ProColors.TextSecondary,
                        fontWeight = if (unreadCount > 0) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = conversation.conversation.lastMessage ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (unreadCount > 0) ProColors.TextPrimary else ProColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = if (unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )
                
                if (unreadCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(ProColors.Primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val date = Date(timestamp)
    val now = Calendar.getInstance()
    val messageDate = Calendar.getInstance().apply { time = date }

    return when {
        now.get(Calendar.YEAR) != messageDate.get(Calendar.YEAR) -> {
            SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(date)
        }
        now.get(Calendar.DAY_OF_YEAR) == messageDate.get(Calendar.DAY_OF_YEAR) -> {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
        }
        now.get(Calendar.DAY_OF_YEAR) - messageDate.get(Calendar.DAY_OF_YEAR) == 1 -> {
            "Yesterday"
        }
        else -> {
            SimpleDateFormat("E, dd MMM", Locale.getDefault()).format(date)
        }
    }
}
