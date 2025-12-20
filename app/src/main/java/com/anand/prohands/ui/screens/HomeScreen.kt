package com.anand.prohands.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anand.prohands.data.JobResponse
import com.anand.prohands.ui.components.ShimmerEffect
import com.anand.prohands.ui.theme.ProColors
import com.anand.prohands.viewmodel.HomeViewModel
import com.anand.prohands.viewmodel.HomeViewModelFactory
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory())
) {
    val jobs by viewModel.jobs.collectAsState()
    val location by viewModel.location.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    RequestLocation(context) { newLocation ->
        viewModel.setLocation(newLocation)
    }

    LaunchedEffect(error) {
        error?.let {
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = it,
                    actionLabel = "Retry"
                )
                if (result == SnackbarResult.ActionPerformed) {
                    location?.let { loc ->
                        viewModel.fetchJobs(loc.latitude, loc.longitude)
                    }
                }
            }
        }
    }
    
    Scaffold(
        containerColor = ProColors.Background,
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
            } else if (location == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ProColors.Primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Getting your location...", color = ProColors.TextSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(jobs) { job ->
                        JobFeedCard(job = job, userLocation = location)
                    }
                }
            }
        }
    }
}

@Composable
fun JobFeedCard(job: JobResponse, userLocation: Location?) {
    var showMap by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ProColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = job.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ProColors.OnSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = job.description,
                style = MaterialTheme.typography.bodyMedium,
                color = ProColors.TextSecondary,
                maxLines = 3
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Wage: $${job.wage}/hr",
                style = MaterialTheme.typography.labelLarge,
                color = ProColors.Primary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { showMap = true },
                colors = ButtonDefaults.buttonColors(containerColor = ProColors.Primary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Locate on Map", color = ProColors.OnPrimary)
            }
        }
    }

    if (showMap) {
        Dialog(onDismissRequest = { showMap = false }) {
            val jobLocation = LatLng(job.latitude, job.longitude)
            val userLatLng = userLocation?.let { LatLng(it.latitude, it.longitude) }
            
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(jobLocation, 12f)
            }

            Card(
                 shape = RoundedCornerShape(16.dp),
                 modifier = Modifier.fillMaxWidth().height(400.dp)
            ) {
                 Column {
                    GoogleMap(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        cameraPositionState = cameraPositionState
                    ) {
                        Marker(
                            state = MarkerState(position = jobLocation),
                            title = job.title,
                            snippet = "Job Location"
                        )
                        userLatLng?.let {
                            Marker(
                                state = MarkerState(position = it),
                                title = "Your Location",
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                            )
                        }
                    }
                    Button(
                        onClick = { showMap = false },
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ProColors.Primary)
                    ) {
                        Text("Close", color = ProColors.OnPrimary)
                    }
                 }
            }
        }
    }
}

@Composable
fun RequestLocation(context: Context, onLocationReceived: (Location) -> Unit) {
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    DisposableEffect(Unit) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let { onLocationReceived(it) }
            }
        }
        onDispose { }
    }
}
