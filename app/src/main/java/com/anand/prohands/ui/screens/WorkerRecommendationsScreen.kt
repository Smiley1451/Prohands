package com.anand.prohands.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.anand.prohands.data.WorkerRecommendationDto
import com.anand.prohands.ui.components.ShimmerEffect
import com.anand.prohands.viewmodel.WorkerRecommendationsViewModel
import com.anand.prohands.viewmodel.WorkerRecommendationsViewModelFactory
import kotlinx.coroutines.launch

@Composable
fun WorkerRecommendationsScreen(
    navController: NavController,
    jobId: String,
    latitude: Double,
    longitude: Double,
    jobTitle: String,
    viewModel: WorkerRecommendationsViewModel = viewModel(factory = WorkerRecommendationsViewModelFactory())
) {
    val recommendations by viewModel.recommendations.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(jobId) {
        viewModel.fetchRecommendations(jobTitle, "", latitude, longitude, 0, 20)
    }

    LaunchedEffect(error) {
        error?.let {
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = it,
                    actionLabel = "Retry"
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.fetchRecommendations(jobTitle, "", latitude, longitude, 0, 20)
                }
            }
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) {
        Box(modifier = Modifier.padding(it)) {
            if (isLoading) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(5) { 
                        ShimmerEffect()
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(recommendations) { recommendation ->
                        WorkerCard(recommendation = recommendation, navController = navController)
                    }
                }
            }
        }
    }
}

@Composable
fun WorkerCard(recommendation: WorkerRecommendationDto, navController: NavController) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { navController.navigate("worker_profile/${recommendation.profile.userId}") }
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            AsyncImage(
                model = recommendation.profile.profilePictureUrl,
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(text = recommendation.profile.name ?: "Unknown")
                Text(text = "Match Score: ${recommendation.matchScore}%", color = Color.Green)
                Text(text = "${recommendation.distanceKm} km away")
            }
        }
    }
}