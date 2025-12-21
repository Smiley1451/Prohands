package com.anand.prohands.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.anand.prohands.data.chat.ChatItem
import com.anand.prohands.data.chat.ChatMessage
import com.anand.prohands.ui.theme.ProColors
import com.anand.prohands.viewmodel.ChatViewModel
import com.anand.prohands.viewmodel.ChatViewModelFactory
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    currentUserId: String,
    recipientId: String,
    navController: NavController
) {
    // Only instantiate ViewModel if we have valid IDs
    if (currentUserId.isEmpty() || recipientId.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Invalid user information", color = ProColors.Error)
        }
        return
    }

    val viewModel: ChatViewModel = viewModel(
        factory = ChatViewModelFactory(currentUserId, recipientId)
    )
    
    val chatItems by viewModel.chatItems.collectAsState()
    val isTyping by viewModel.typing.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val recipientPresence by viewModel.recipientPresence.collectAsState()
    
    val listState = rememberLazyListState()
    var messageText by remember { mutableStateOf("") }
    
    // Auto-scroll to bottom when new items arrive
    LaunchedEffect(chatItems.size) {
        if (chatItems.isNotEmpty()) {
            listState.animateScrollToItem(chatItems.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = recipientId, // In a real app, resolve this to a name
                            style = MaterialTheme.typography.titleMedium,
                            color = ProColors.TextPrimary
                        )
                        if (isTyping) {
                            Text(
                                text = "typing...",
                                style = MaterialTheme.typography.bodySmall,
                                color = ProColors.Primary
                            )
                        } else {
                            val statusText = if (recipientPresence?.online == true) "Online" else "Offline"
                            val lastSeen = recipientPresence?.lastSeen?.let { formatLastSeen(it) } ?: ""
                            Text(
                                text = if (recipientPresence?.online == true) statusText else "$statusText $lastSeen",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (recipientPresence?.online == true) ProColors.Success else ProColors.TextSecondary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ProColors.TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ProColors.Surface,
                    titleContentColor = ProColors.TextPrimary
                )
            )
        },
        bottomBar = {
            ChatInput(
                value = messageText,
                onValueChange = { 
                    messageText = it
                    viewModel.sendTyping()
                },
                onSend = {
                    if (messageText.isNotBlank()) {
                        viewModel.sendMessage(messageText)
                        messageText = ""
                    }
                },
                enabled = isConnected
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFE5DDD5)) // WhatsApp-like background color
        ) {
            
            if (chatItems.isEmpty() && !isConnected) {
                // Connecting State
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ProColors.Primary)
                }
            } else if (chatItems.isEmpty()) {
                // Empty State
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No messages yet.\nSay hello!",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chatItems) { item ->
                    when (item) {
                        is ChatItem.Message -> {
                            MessageBubble(
                                message = item.message,
                                isMe = item.message.senderId == currentUserId
                            )
                        }
                        is ChatItem.DateSeparator -> {
                            DateSeparator(date = item.date)
                        }
                    }
                }
                if (isTyping) {
                     item {
                        TypingIndicator()
                    }
                }
            }
        }
    }
}

@Composable
fun DateSeparator(date: String) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0xFFDDF2FA),
            shape = RoundedCornerShape(8.dp),
            shadowElevation = 0.dp
        ) {
            Text(
                text = date,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF555555),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage, isMe: Boolean) {
    val bubbleColor = if (isMe) Color(0xFFDCF8C6) else Color.White
    val align = if (isMe) Alignment.End else Alignment.Start
    val shape = if (isMe) 
        RoundedCornerShape(8.dp, 0.dp, 8.dp, 8.dp) 
    else 
        RoundedCornerShape(0.dp, 8.dp, 8.dp, 8.dp)
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = align
    ) {
        Surface(
            color = bubbleColor,
            shape = shape,
            shadowElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Black
                )
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = formatTime(message.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                    if (isMe) {
                        StatusIcon(status = message.status)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusIcon(status: String) {
    val icon = when (status) {
        "SENT" -> Icons.Default.Check
        "DELIVERED" -> Icons.Default.DoneAll
        "READ" -> Icons.Default.DoneAll
        else -> Icons.Default.Check
    }
    
    val tint = if (status == "READ") Color(0xFF34B7F1) else Color.Gray
    
    Icon(
        imageVector = icon,
        contentDescription = status,
        modifier = Modifier.size(16.dp),
        tint = tint
    )
}

@Composable
fun ChatInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean
) {
    Surface(
        color = Color.White,
        shadowElevation = 8.dp,
        modifier = Modifier.imePadding() // Handle keyboard
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White),
                placeholder = { Text("Message") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF0F0F0),
                    unfocusedContainerColor = Color(0xFFF0F0F0),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                maxLines = 5,
                enabled = enabled
            )
            Spacer(modifier = Modifier.width(8.dp))
            FloatingActionButton(
                onClick = onSend,
                containerColor = ProColors.Primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
            }
        }
    }
}

@Composable
fun TypingIndicator() {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(0.dp, 8.dp, 8.dp, 8.dp),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
             // Simple animated dots could be added here
            Text(
                text = "...",
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )
        }
    }
}

fun formatTime(isoString: String): String {
    return try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val instant = Instant.parse(isoString)
            val formatter = DateTimeFormatter.ofPattern("HH:mm")
                .withZone(ZoneId.systemDefault())
            formatter.format(instant)
        } else {
             if (isoString.length > 16) isoString.substring(11, 16) else ""
        }
    } catch (e: Exception) {
        ""
    }
}

fun formatLastSeen(isoString: String): String {
    // Basic implementation
    return try {
         "at " + formatTime(isoString)
    } catch (e: Exception) {
        ""
    }
}
