package com.anand.prohands.ui.screens

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.anand.prohands.data.chat.*
import com.anand.prohands.ui.theme.ProColors
import com.anand.prohands.viewmodel.ChatItem
import com.anand.prohands.viewmodel.ChatUiState
import com.anand.prohands.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MessagesScreen(
    navController: NavController,
    chatViewModel: ChatViewModel,
    recipientName: String,
    recipientAvatar: String?
) {
    val uiState by chatViewModel.uiState.collectAsState()
    val recipientPresence by chatViewModel.recipientPresence.collectAsState()
    val isTyping by chatViewModel.isTyping.collectAsState()
    val isRecording by chatViewModel.isRecording.collectAsState()

    // --- Permission & Activity Launchers ---
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { chatViewModel.sendMediaMessage(it, MessageType.IMAGE) }
    }
    
    // You need to provide a file URI for the camera to save the image to
    val cameraLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.TakePicture()) { success: Boolean ->
        if (success) {
            // The URI should be retrieved from where it was stored before launching
            // For demo purposes, we are not fully implementing this part.
        }
    }

    val recordAudioLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            // voiceRecorder.start()
            chatViewModel.setRecordingState(true)
        }
    }

    Scaffold(
        topBar = {
            ChatTopBar(
                navController = navController, 
                name = recipientName, 
                avatarUrl = recipientAvatar, 
                presenceInfo = when {
                    isTyping -> "typing..."
                    recipientPresence?.isOnline == true -> "Online"
                    else -> "Offline"
                },
                isOnline = recipientPresence?.isOnline == true
            )
        },
        containerColor = ProColors.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            MessagesList(
                uiState = uiState,
                currentUserId = "", // Pass current user ID
                modifier = Modifier.weight(1f)
            )

            MessageInput(
                onMessageSend = { chatViewModel.sendMessage(it) }, 
                onTyping = { chatViewModel.onUserTyping() },
                onAttachFile = { imagePickerLauncher.launch("image/*") },
                onCameraClick = { /* cameraLauncher.launch(uri) */ },
                onStartRecording = { recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                onStopRecording = {
                    //val file = voiceRecorder.stop()
                    //file?.let { chatViewModel.sendMediaMessage(Uri.fromFile(it), MessageType.VOICE) }
                    chatViewModel.setRecordingState(false)
                },
                isRecording = isRecording
            )
        }
    }
}

@Composable
fun ChatTopBar(navController: NavController, name: String, avatarUrl: String?, presenceInfo: String, isOnline: Boolean) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val painter = if (!avatarUrl.isNullOrEmpty()) {
                    rememberAsyncImagePainter(avatarUrl)
                } else {
                    rememberAsyncImagePainter("https://ui-avatars.com/api/?name=$name&background=random")
                }
                Image(
                    painter = painter,
                    contentDescription = name,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Gray),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = name, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(text = presenceInfo, fontSize = 12.sp, color = if(isOnline) Color.LightGray else Color.Gray)
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = ProColors.Primary)
    )
}

@Composable
fun MessagesList(uiState: ChatUiState, currentUserId: String, modifier: Modifier) {
    val listState = rememberLazyListState()
    LaunchedEffect(uiState.items.size) {
        if (uiState.items.isNotEmpty()) {
            listState.animateScrollToItem(uiState.items.size - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.padding(horizontal = 8.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        items(uiState.items) { item ->
            when (item) {
                is ChatItem.DateSeparator -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = item.date,
                            style = MaterialTheme.typography.labelSmall,
                            color = ProColors.TextSecondary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(ProColors.Surface.copy(alpha = 0.5f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                is ChatItem.Message -> {
                    MessageBubble(message = item.message, isFromCurrentUser = item.message.senderId == currentUserId)
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: MessageEntity, isFromCurrentUser: Boolean) {
    val alignment = if (isFromCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
    val shape = if (isFromCurrentUser) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    }
    val colors = if (isFromCurrentUser) {
        BubbleColors(background = ProColors.Primary, text = Color.White)
    } else {
        BubbleColors(background = ProColors.Surface, text = ProColors.TextPrimary)
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Surface(
            modifier = Modifier.padding(vertical = 4.dp),
            shape = shape,
            color = colors.background,
            shadowElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 2.dp)) {
                 when (message.type) {
                    MessageType.TEXT -> {
                        Text(text = message.content, color = colors.text, fontSize = 16.sp)
                    }
                    MessageType.IMAGE -> {
                        Image(
                            painter = rememberAsyncImagePainter(message.content),
                            contentDescription = "Image",
                            modifier = Modifier.sizeIn(maxHeight = 250.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    MessageType.VOICE -> {
                        AudioPlayer(uri = Uri.parse(message.content), tint = colors.text)
                    }
                    else -> {}
                }
                Spacer(modifier = Modifier.height(4.dp))
                 Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                    Text(
                        text = formatMessageTime(message.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.text.copy(alpha = 0.7f)
                    )
                    if (isFromCurrentUser) {
                        Spacer(modifier = Modifier.width(4.dp))
                        MessageStatusIndicator(status = message.status)
                    }
                }
            }
        }
    }
}

@Composable
fun AudioPlayer(uri: Uri, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play", tint = tint)
        Slider(value = 0f, onValueChange = {}, modifier = Modifier.weight(1f))
        Text(text = "0:00", style = MaterialTheme.typography.labelSmall, color = tint)
    }
}

@Composable
fun MessageStatusIndicator(status: MessageStatus) {
    val icon = when (status) {
        MessageStatus.PENDING -> Icons.Default.Schedule
        MessageStatus.SENT -> Icons.Default.Check
        MessageStatus.DELIVERED -> Icons.Default.DoneAll
        MessageStatus.READ -> Icons.Default.DoneAll
        MessageStatus.FAILED -> Icons.Default.Error
        MessageStatus.DELETED -> Icons.Default.Delete
    }
    val color = when (status) {
        MessageStatus.READ -> ProColors.Primary
        MessageStatus.FAILED -> Color.Red
        else -> Color.White.copy(alpha = 0.7f)
    }
    Icon(
        imageVector = icon, 
        contentDescription = "Status", 
        modifier = Modifier.size(16.dp), 
        tint = color
    )
}

@Composable
fun MessageInput(
    onMessageSend: (String) -> Unit, 
    onTyping: () -> Unit,
    onAttachFile: () -> Unit,
    onCameraClick: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    isRecording: Boolean
) {
    var text by remember { mutableStateOf("") }
    var showAttachmentMenu by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isMicPressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(isMicPressed) {
        if (isMicPressed) onStartRecording() else if(isRecording) onStopRecording()
    }

    Surface(shadowElevation = 4.dp, color = ProColors.Surface) {
        Column {
            AnimatedVisibility(visible = showAttachmentMenu) {
                AttachmentMenu(onAttachFile, onCameraClick)
            }
            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = { 
                        text = it
                        onTyping()
                    },
                    modifier = Modifier.weight(1f),
                    cursorBrush = SolidColor(ProColors.Primary),
                    textStyle = TextStyle(color = ProColors.TextPrimary, fontSize = 16.sp),
                    decorationBox = {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(24.dp))
                                .background(ProColors.Background)
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (text.isEmpty()) {
                                Text("Message...", color = ProColors.TextSecondary)
                            }
                            it()
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = { showAttachmentMenu = !showAttachmentMenu }) {
                                Icon(Icons.Default.AttachFile, contentDescription = "Attach", tint = ProColors.TextSecondary)
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                
                val isSendButton = text.isNotBlank()

                 AnimatedVisibility(
                    visible = isRecording,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = "Recording", tint = Color.Red)
                    }
                }

                AnimatedVisibility(
                    visible = !isRecording,
                    enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }),
                    exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it })
                ) {
                     FloatingActionButton(
                        onClick = { 
                            if(isSendButton) {
                                onMessageSend(text)
                                text = ""
                            } 
                        },
                        interactionSource = if (!isSendButton) interactionSource else remember { MutableInteractionSource() },
                        modifier = Modifier.size(56.dp),
                        containerColor = ProColors.Primary,
                        elevation = FloatingActionButtonDefaults.elevation(0.dp)
                    ) {
                        Icon(
                            imageVector = if(isSendButton) Icons.AutoMirrored.Filled.Send else Icons.Default.Mic,
                            contentDescription = if(isSendButton) "Send" else "Record", 
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AttachmentMenu(onGalleryClick: () -> Unit, onCameraClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        AttachmentMenuItem(icon = Icons.Default.PhotoLibrary, text = "Gallery", onClick = onGalleryClick)
        AttachmentMenuItem(icon = Icons.Default.CameraAlt, text = "Camera", onClick = onCameraClick)
    }
}

@Composable
fun AttachmentMenuItem(icon: ImageVector, text: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Icon(imageVector = icon, contentDescription = text, modifier = Modifier.size(48.dp), tint = ProColors.Primary)
        Text(text, fontSize = 12.sp, color = ProColors.TextSecondary)
    }
}

fun formatMessageTime(timestamp: Long): String {
    return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp))
}

data class BubbleColors(val background: Color, val text: Color)
