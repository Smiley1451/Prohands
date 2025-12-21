package com.anand.prohands.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.anand.prohands.R
import com.anand.prohands.ui.theme.ProColors

@Composable
fun AppHeader(
    title: String? = null,
    isHome: Boolean = false,
    profileImageUrl: String? = null,
    onBackClick: (() -> Unit)? = null,
    onProfileClick: (() -> Unit)? = null,
    onMessagesClick: (() -> Unit)? = null,
    onNotificationsClick: (() -> Unit)? = null
) {
    Surface(
        color = ProColors.Primary,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth().height(60.dp) // Fixed minimal height
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp), 
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Section
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isHome) {
                    // Profile Image first (as per request)
                    AsyncImage(
                        model = profileImageUrl?.replace("http://", "https://")?.replace(".heic", ".jpg")
                            ?: "https://via.placeholder.com/150",
                        contentDescription = "Profile",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(36.dp) 
                            .clip(CircleShape)
                            .background(ProColors.Surface)
                            .border(1.dp, ProColors.Surface, CircleShape)
                            .clickable { onProfileClick?.invoke() }
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Text(
                        text = "ProHand's",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ProColors.OnPrimary
                    )
                } else {
                    // Back button and Title for other screens
                    if (onBackClick != null) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = ProColors.OnPrimary
                            )
                        }
                    }
                    Text(
                        text = title ?: "",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ProColors.OnPrimary,
                        modifier = Modifier.padding(start = if (onBackClick == null) 0.dp else 4.dp)
                    )
                }
            }

            // Right Section
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isHome) {
                    IconButton(onClick = { onMessagesClick?.invoke() }) {
                         Icon(
                             imageVector = Icons.Default.Mail,
                             contentDescription = "Messages",
                             tint = ProColors.OnPrimary
                         )
                    }
                    IconButton(onClick = { onNotificationsClick?.invoke() }) {
                         Icon(
                             imageVector = Icons.Default.Notifications,
                             contentDescription = "Notifications",
                             tint = ProColors.OnPrimary
                         )
                    }
                }
            }
        }
    }
}
