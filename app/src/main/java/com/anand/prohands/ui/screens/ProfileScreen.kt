package com.anand.prohands.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.anand.prohands.data.ClientProfileDto
import com.anand.prohands.viewmodel.ProfileViewModel
import com.anand.prohands.viewmodel.ProfileViewModelFactory

// --- 1. Enhanced Professional Palette ---
object ProColors {
    val PrimaryBlue = Color(0xFF2563EB) // Standard Trust Blue
    val DeepBlue = Color(0xFF172554)    // Darker, more solid blue for gradient start
    val VividBlue = Color(0xFF1D4ED8)   // Vibrant blue for gradient end
    val SurfaceBg = Color(0xFFF8FAFC)
    val TextPrimary = Color(0xFF1E293B)
    val TextSecondary = Color(0xFF64748B)
    val Gold = Color(0xFFF59E0B)
    val Green = Color(0xFF10B981)
    val White = Color.White
}

@Composable
fun ProfileScreen(
    userId: String,
    isReadOnly: Boolean,
    onEditProfile: () -> Unit = {},
    onLogout: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModelFactory())
) {
    val profile by viewModel.profile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(userId) {
        viewModel.fetchProfile(userId)
    }

    Scaffold(
        containerColor = ProColors.SurfaceBg
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> CircularProgressIndicator(color = ProColors.PrimaryBlue)
                error != null -> ErrorView(error) { viewModel.fetchProfile(userId) }
                profile != null -> ProfileContent(profile!!, isReadOnly, onEditProfile, { viewModel.fetchProfile(userId) }, onLogout)
            }
        }
    }
}

@Composable
fun ErrorView(error: String?, onRefresh: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color.Red, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text("Unable to load profile", style = MaterialTheme.typography.titleMedium)
        Text(error ?: "Unknown error", color = ProColors.TextSecondary, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRefresh,
            colors = ButtonDefaults.buttonColors(containerColor = ProColors.PrimaryBlue)
        ) {
            Text("Try Again")
        }
    }
}

@Composable
fun ProfileContent(
    profile: ClientProfileDto,
    isReadOnly: Boolean,
    onEditProfile: () -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item { ModernHeroHeader(profile) }
        item { AiInsightCard(profile) }
        item { SkillsSection(profile) }
        item { TrustMetricsGrid(profile) }
        if (!isReadOnly) {
            item { ProfileActions(onEditProfile, onRefresh, onLogout) }
        }
    }
}

@Composable
fun ModernHeroHeader(profile: ClientProfileDto) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp) // Height to accommodate header + overlap
    ) {
        // --- UPDATED BANNER ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp) // The blue area height
                // This clip creates the "Radial" / Curved bottom effect
                .clip(RoundedCornerShape(bottomStart = 48.dp, bottomEnd = 48.dp))
                .background(
                    Brush.verticalGradient(
                        // Solid Dark Blue Gradient
                        colors = listOf(ProColors.DeepBlue, ProColors.VividBlue)
                    )
                )
        )

        // Profile Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 60.dp), // Push content down to overlap properly
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
                        .border(4.dp, ProColors.White, CircleShape) // Crisp white border
                        .shadow(elevation = 10.dp, shape = CircleShape)
                )

                if (profile.recommendationFlag == true) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = "Recommended",
                        tint = ProColors.Gold,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = (-4).dp, y = (-4).dp)
                            .size(32.dp)
                            .background(ProColors.White, CircleShape)
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
                    color = ProColors.TextPrimary
                )
                if (profile.experienceLevel.equals("Expert", true) || profile.experienceLevel.equals("Intermediate", true)) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = "Verified",
                        tint = ProColors.PrimaryBlue,
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
fun StatItem(value: String, label: String, icon: ImageVector, iconColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = ProColors.TextPrimary)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(16.dp))
        }
        Text(text = label, fontSize = 12.sp, color = ProColors.TextSecondary)
    }
}

@Composable
fun AiInsightCard(profile: ClientProfileDto) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
        border = BorderStroke(1.dp, Color(0xFFDBEAFE)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ProColors.PrimaryBlue, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI Profile Insights", fontWeight = FontWeight.Bold, color = ProColors.VividBlue)
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
                            color = ProColors.VividBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .background(Color.White, RoundedCornerShape(4.dp))
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
                        border = BorderStroke(1.dp, Color.LightGray)
                    )
                }
            }
        }
    }
}

@Composable
fun TrustMetricsGrid(profile: ClientProfileDto) {
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
                color = ProColors.PrimaryBlue
            )
            TrustCard(
                modifier = Modifier.weight(1f),
                title = "Jobs Done",
                value = "${profile.totalReviews}",
                icon = Icons.Default.WorkHistory,
                color = ProColors.Gold
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
             // Placeholder for Location or other stat
            TrustCard(
                modifier = Modifier.weight(1f),
                title = "Location",
                value = "View Map",
                icon = Icons.Default.LocationOn,
                color = Color(0xFFE11D48)
            )
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
        colors = CardDefaults.cardColors(containerColor = ProColors.White),
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
        // Main Action: Edit Profile (Takes up most space)
        Button(
            onClick = onEditProfile,
            modifier = Modifier
                .weight(1f)
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ProColors.PrimaryBlue)
        ) {
            Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Edit Profile")
        }

        // Secondary Action: Refresh
        OutlinedButton(
            onClick = onRefresh,
            modifier = Modifier.size(50.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, ProColors.PrimaryBlue.copy(alpha = 0.5f)),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(Icons.Outlined.Refresh, contentDescription = "Refresh", tint = ProColors.PrimaryBlue)
        }

        // Destructive/Secondary Action: Logout
        OutlinedIconButton(
            onClick = onLogout,
            modifier = Modifier.size(50.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.2f))
        ) {
            Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = "Logout", tint = Color.Red)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreviewEnhanced() {
    val dummyProfile = ClientProfileDto(
        userId = "anand1234",
        name = "Anand J",
        email = "anand@example.com",
        phone = "1234567890",
        profilePictureUrl = null,
        experienceLevel = "Expert",
        recommendationFlag = true,
        averageRating = 4.9,
        jobSuccessRate = 98.0,
        recommendedWagePerHour = 25.0,
        aiGeneratedSummary = "Anand is a highly rated expert with consistent 5-star reviews in system design. Clients praise his punctuality and problem-solving skills.",
        topReviewKeywords = listOf("Punctual", "Fast", "Reliable"),
        skills = listOf("Android", "Kotlin", "System Design", "Compose"),
        profileStrengthScore = 92,
        totalReviews = 145,
        latitude = 0.0,
        longitude = 0.0,
        createdAt = "2023-01-15",
        lastAiUpdate = "2023-10-27",
        profileCompletionPercent = 100
    )
    MaterialTheme {
        ProfileScreen(userId = "anand1234", isReadOnly = false)
    }
}
