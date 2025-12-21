package com.anand.prohands.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.anand.prohands.data.ClientProfileDto
import com.anand.prohands.data.JobResponse
import com.anand.prohands.ui.theme.ProColors
import com.anand.prohands.viewmodel.ProfileViewModel
import com.anand.prohands.viewmodel.ProfileViewModelFactory
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    userId: String,
    isReadOnly: Boolean,
    onEditProfile: () -> Unit = {},
    onLogout: () -> Unit = {},
    onMessageClick: () -> Unit = {},
    onJobClick: (String) -> Unit = {},
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModelFactory())
) {
    val profile by viewModel.profile.collectAsState()
    val nearbyJobs by viewModel.nearbyJobs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Check and request location permissions
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocation = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocation = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        
        if (fineLocation || coarseLocation) {
             getCurrentLocation(context) { location ->
                 viewModel.fetchNearbyJobs(location.latitude, location.longitude)
             }
        }
    }

    LaunchedEffect(userId) {
        viewModel.fetchProfile(userId)
    }

    // Attempt to get current location if user location is missing or if we just want to ensure we have it for the map
    LaunchedEffect(Unit) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
             getCurrentLocation(context) { location ->
                 // If the profile doesn't have a location, or even if it does, 
                 // we might want to prioritize current location for "Nearby Jobs" 
                 // or update the view model's concept of "current location" for the map center.
                 // For now, let's just ensure we fetch jobs around the current location 
                 // if the profile location was missing or old.
                 // Ideally, we compare or just use current.
                 viewModel.fetchNearbyJobs(location.latitude, location.longitude)
             }
        }
    }

    Scaffold(
        containerColor = ProColors.SurfaceVariant
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> CircularProgressIndicator(color = ProColors.Primary)
                error != null -> ErrorView(error) { viewModel.fetchProfile(userId) }
                profile != null -> ProfileContent(profile!!, nearbyJobs, isReadOnly, onEditProfile, { viewModel.fetchProfile(userId) }, onLogout, onMessageClick, onJobClick)
            }
        }
    }
}

private fun getCurrentLocation(context: Context, onLocationResult: (Location) -> Unit) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    try {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    onLocationResult(it)
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@Composable
fun ErrorView(error: String?, onRefresh: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = ProColors.Error, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text("Unable to load profile", style = MaterialTheme.typography.titleMedium)
        Text(error ?: "Unknown error", color = ProColors.TextSecondary, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRefresh,
            colors = ButtonDefaults.buttonColors(containerColor = ProColors.Primary)
        ) {
            Text("Try Again", color = ProColors.OnPrimary)
        }
    }
}

@Composable
fun ProfileContent(
    profile: ClientProfileDto,
    nearbyJobs: List<JobResponse>,
    isReadOnly: Boolean,
    onEditProfile: () -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit,
    onMessageClick: () -> Unit,
    onJobClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item { ModernHeroHeader(profile) }
        item { AiInsightCard(profile) }
        item { SkillsSection(profile) }
        item { TrustMetricsGrid(profile, nearbyJobs, onJobClick) }
        if (!isReadOnly) {
            item { ProfileActions(onEditProfile, onRefresh, onLogout) }
        } else {
            item { WorkerActions(onMessageClick) }
        }
    }
}

@Composable
fun ModernHeroHeader(profile: ClientProfileDto) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp) 
    ) {
        // --- UPDATED BANNER ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp) 
                .clip(RoundedCornerShape(bottomStart = 48.dp, bottomEnd = 48.dp))
                .background(
                    Brush.verticalGradient(
                        // Yellow/Gold Gradient
                        colors = listOf(ProColors.PrimaryVariant, ProColors.Primary)
                    )
                )
        )

        // Profile Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 60.dp), 
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar with Ring
            Box(contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = profile.profilePictureUrl?.let { url ->
                        url.replace("http://", "https://").replace(".heic", ".jpg")
                    } ?: "https://via.placeholder.com/150",
                    contentDescription = "Profile",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(124.dp)
                        .clip(CircleShape)
                        .border(4.dp, ProColors.Surface, CircleShape) 
                        .shadow(elevation = 10.dp, shape = CircleShape)
                )

                if (profile.recommendationFlag == true) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = "Recommended",
                        tint = ProColors.PrimaryVariant,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = (-4).dp, y = (-4).dp)
                            .size(32.dp)
                            .background(ProColors.Surface, CircleShape)
                            .padding(4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Name & Verification
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = profile.name ?: "User",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = ProColors.OnBackground
                )
                if (profile.experienceLevel.equals("Expert", true) || profile.experienceLevel.equals("Intermediate", true)) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = "Verified",
                        tint = ProColors.Primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Text(
                text = profile.experienceLevel ?: "Beginner",
                style = MaterialTheme.typography.bodyMedium,
                color = ProColors.TextSecondary
            )
        }
    }
}

@Composable
fun AiInsightCard(profile: ClientProfileDto) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = ProColors.Surface),
        border = BorderStroke(1.dp, ProColors.Divider),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ProColors.Primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI Profile Insights", fontWeight = FontWeight.Bold, color = ProColors.PrimaryVariant)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = profile.aiGeneratedSummary ?: "No AI insights generated yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = ProColors.TextPrimary,
                lineHeight = 20.sp
            )
            if (!profile.topReviewKeywords.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    profile.topReviewKeywords.take(3).forEach { keyword ->
                        Text(
                            text = "#$keyword",
                            color = ProColors.PrimaryVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .background(ProColors.SurfaceVariant, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SkillsSection(profile: ClientProfileDto) {
    Column(modifier = Modifier.padding(16.dp)) {
        SectionTitle("Skills & Expertise")
        if (profile.skills.isNullOrEmpty()) {
            Text("No skills added yet.", color = ProColors.TextSecondary, fontSize = 14.sp)
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                profile.skills.forEach { skill ->
                    AssistChip(
                        onClick = {},
                        label = { Text(skill, color = ProColors.TextPrimary) },
                        border = BorderStroke(1.dp, ProColors.Divider)
                    )
                }
            }
        }
    }
}

@Composable
fun TrustMetricsGrid(profile: ClientProfileDto, nearbyJobs: List<JobResponse>, onJobClick: (String) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        SectionTitle("Activity & Trust")
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TrustCard(
                modifier = Modifier.weight(1f),
                title = "Trust Score",
                value = "${profile.profileStrengthScore ?: 0}/100",
                icon = Icons.Default.Shield,
                color = ProColors.Primary
            )
            TrustCard(
                modifier = Modifier.weight(1f),
                title = "Jobs Done",
                value = "${profile.totalReviews}",
                icon = Icons.Default.WorkHistory,
                color = ProColors.PrimaryVariant
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TrustCard(
                modifier = Modifier.weight(1f),
                title = "Member Since",
                value = profile.createdAt?.take(4) ?: "N/A",
                icon = Icons.Default.CalendarMonth,
                color = ProColors.TextSecondary
            )
            
            // Location Card with Map Interaction
            LocationMapCard(
                modifier = Modifier.weight(1f),
                profile = profile,
                nearbyJobs = nearbyJobs,
                onJobClick = onJobClick
            )
        }
    }
}

@Composable
fun LocationMapCard(
    modifier: Modifier, 
    profile: ClientProfileDto, 
    nearbyJobs: List<JobResponse>,
    onJobClick: (String) -> Unit
) {
    var showMapDialog by remember { mutableStateOf(false) }
    var currentUserLocation by remember { mutableStateOf<LatLng?>(null) }
    val context = LocalContext.current
    
    // Fetch current location when the card is clicked or component loads
    LaunchedEffect(showMapDialog) {
        if (showMapDialog) {
             getCurrentLocation(context) { location ->
                 currentUserLocation = LatLng(location.latitude, location.longitude)
             }
        }
    }

    Card(
        modifier = modifier.clickable { showMapDialog = true },
        colors = CardDefaults.cardColors(containerColor = ProColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = ProColors.Error, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text("View Map", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ProColors.TextPrimary)
            Text("Location", fontSize = 12.sp, color = ProColors.TextSecondary)
        }
    }

    if (showMapDialog) {
        val userLocation = currentUserLocation ?: if (profile.latitude != null && profile.longitude != null) {
            LatLng(profile.latitude, profile.longitude)
        } else {
            // Default to India/Bengaluru if nothing available, rather than SF
            LatLng(12.9716, 77.5946) 
        }

        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(userLocation, 12f)
        }
        
        // Update camera when location is found
        LaunchedEffect(currentUserLocation) {
             currentUserLocation?.let {
                 cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(it, 14f))
             }
        }

        Dialog(onDismissRequest = { showMapDialog = false }) {
            Card(
                 shape = RoundedCornerShape(16.dp),
                 modifier = Modifier.fillMaxWidth().height(500.dp)
            ) {
                 Column {
                    Box(modifier = Modifier.weight(1f)) {
                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = cameraPositionState,
                            properties = MapProperties(isMyLocationEnabled = true), // Enable the blue dot
                            uiSettings = MapUiSettings(myLocationButtonEnabled = true) // Enable the "go to my location" button
                        ) {
                            // User Profile Location Marker (static registered address)
                            if (profile.latitude != null && profile.longitude != null) {
                                Marker(
                                    state = MarkerState(position = LatLng(profile.latitude, profile.longitude)),
                                    title = "Registered Address",
                                    snippet = profile.name ?: "User",
                                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                                )
                            }
                            
                            // Nearby Jobs Markers
                            nearbyJobs.forEach { job ->
                                Marker(
                                    state = MarkerState(position = LatLng(job.latitude, job.longitude)),
                                    title = job.title,
                                    snippet = "Wage: $${job.wage}/hr (Click to View)",
                                    onInfoWindowClick = {
                                        onJobClick(job.jobId)
                                        showMapDialog = false
                                    }
                                )
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { showMapDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = ProColors.Primary)
                        ) {
                            Text("Close", color = ProColors.OnPrimary)
                        }
                    }
                 }
            }
        }
    }
}

@Composable
fun TrustCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = ProColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ProColors.TextPrimary)
            Text(title, fontSize = 12.sp, color = ProColors.TextSecondary)
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = ProColors.TextPrimary,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
fun ProfileActions(
    onEditProfile: () -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Main Action: Edit Profile
        Button(
            onClick = onEditProfile,
            modifier = Modifier
                .weight(1f)
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ProColors.Primary)
        ) {
            Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp), tint = ProColors.OnPrimary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Edit Profile", color = ProColors.OnPrimary)
        }

        // Secondary Action: Refresh
        OutlinedButton(
            onClick = onRefresh,
            modifier = Modifier.size(50.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, ProColors.Primary.copy(alpha = 0.5f)),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(Icons.Outlined.Refresh, contentDescription = "Refresh", tint = ProColors.Primary)
        }

        // Destructive Action: Logout
        OutlinedIconButton(
            onClick = onLogout,
            modifier = Modifier.size(50.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, ProColors.Error.copy(alpha = 0.2f))
        ) {
            Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = "Logout", tint = ProColors.Error)
        }
    }
}

@Composable
fun WorkerActions(onMessageClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Button(
            onClick = onMessageClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ProColors.Primary)
        ) {
            Icon(Icons.AutoMirrored.Outlined.Message, contentDescription = null, modifier = Modifier.size(18.dp), tint = ProColors.OnPrimary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Message Worker", color = ProColors.OnPrimary)
        }
    }
}
