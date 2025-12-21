package com.anand.prohands.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.anand.prohands.data.SearchResultDto
import com.anand.prohands.ui.components.AppHeader
import com.anand.prohands.ui.theme.ProColors
import com.anand.prohands.viewmodel.SearchViewModel
import com.anand.prohands.viewmodel.SearchViewModelFactory

@Composable
fun SearchScreen(
    navController: NavController,
    viewModel: SearchViewModel = viewModel(factory = SearchViewModelFactory())
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val listState = rememberLazyListState()

    // Infinite scroll detection
    val buffer = 2
    val isScrollToEnd by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItemIndex >= (totalItems - buffer)
        }
    }

    LaunchedEffect(isScrollToEnd) {
        if (isScrollToEnd && !isLoading) {
            viewModel.loadNextPage()
        }
    }

    Scaffold(
        topBar = { AppHeader(title = "Search Workers") },
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
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                label = { Text("Search by name, skills...", color = ProColors.TextSecondary) },
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

            // Results List
            Box(modifier = Modifier.fillMaxSize()) {
                if (searchResults.isEmpty() && !isLoading && searchQuery.length >= 2) {
                    Text(
                        text = "No results found.",
                        color = ProColors.TextSecondary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (searchQuery.length < 2) {
                    Text(
                        text = "Type at least 2 characters to search",
                        color = ProColors.TextSecondary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(searchResults) { result ->
                            SearchResultItem(searchResult = result, onClick = {
                                val userId = result.profile.userId
                                navController.navigate("worker_profile/$userId")
                            })
                        }
                        
                        if (isLoading && searchResults.isNotEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = ProColors.Primary, modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }

                if (isLoading && searchResults.isEmpty()) {
                    CircularProgressIndicator(
                        color = ProColors.Primary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                error?.let {
                   if (searchResults.isEmpty()) {
                       Text(
                           text = it,
                           color = ProColors.Error,
                           modifier = Modifier.align(Alignment.Center).padding(16.dp)
                       )
                   }
                }
            }
        }
    }
}

@Composable
fun SearchResultItem(
    searchResult: SearchResultDto,
    onClick: () -> Unit
) {
    val profile = searchResult.profile
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ProColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Picture
            val painter = if (!profile.profilePictureUrl.isNullOrEmpty()) {
                rememberAsyncImagePainter(profile.profilePictureUrl)
            } else {
                rememberAsyncImagePainter("https://ui-avatars.com/api/?name=${profile.name ?: "User"}&background=random")
            }
            
            Image(
                painter = painter,
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color.Gray),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.name ?: "Unknown User",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ProColors.TextPrimary
                )
                
                // User ID
                Text(
                    text = "@${profile.userId}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ProColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                profile.aiGeneratedSummary?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = ProColors.TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (!profile.skills.isNullOrEmpty()) {
                    Text(
                        text = profile.skills.take(3).joinToString(", "),
                        style = MaterialTheme.typography.labelSmall,
                        color = ProColors.Primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            // Note: distance is not in SearchResultDto/ClientProfileDto in the updated schema.
            // Using score instead if relevant or omitted. 
            // The prompt says "score: Double" is in SearchResultDto.
            Column(horizontalAlignment = Alignment.End) {
                Text(
                     text = "Match: ${(searchResult.score * 100).toInt()}%", // Example usage of score
                     style = MaterialTheme.typography.labelMedium,
                     color = ProColors.TextSecondary
                )
            }
        }
    }
}
