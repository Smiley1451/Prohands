package com.anand.prohands.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavigation(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : BottomNavigation(
        route = "home",
        title = "Home",
        icon = Icons.Default.Home
    )
    object Search : BottomNavigation(
        route = "search",
        title = "Search",
        icon = Icons.Default.Search
    )
    object PostJob : BottomNavigation(
        route = "post_job",
        title = "Post Job",
        icon = Icons.Default.AddCircle
    )
    object Messages : BottomNavigation(
        route = "messages",
        title = "Messages",
        icon = Icons.AutoMirrored.Filled.Message
    )
    object Profile : BottomNavigation(
        route = "profile",
        title = "Profile",
        icon = Icons.Default.AccountCircle
    )
}