package com.anand.prohands.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.anand.prohands.ui.components.AppHeader
import com.anand.prohands.ui.theme.ProColors

@Composable
fun SearchScreen(navController: NavController) {
    Scaffold(
        topBar = { AppHeader(title = "Search") },
        containerColor = ProColors.Background
    ) { padding ->
        Box(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Search Screen", color = ProColors.TextSecondary)
        }
    }
}
