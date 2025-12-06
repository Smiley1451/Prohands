package com.anand.prohands.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.anand.prohands.data.ClientProfileDto
import com.anand.prohands.ui.theme.* // Import all theme colors
import com.anand.prohands.viewmodel.AuthState

@Composable
fun ProfileScreen(
    authState: AuthState,
    onEditProfile: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    Box(
        modifier = Modifier.fillMaxSize().background(SurfaceWhite),
        contentAlignment = Alignment.Center
    ) {
        when {
            authState.isLoading -> {
                CircularProgressIndicator()
            }
            authState.error != null -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Error: ${authState.error}", color = Color.Red)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onRefresh) {
                        Text("Retry")
                    }
                }
            }
            authState.profile != null -> {
                val profile = authState.profile
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    item { HeroHeader(profile) }
                    item { AiInsightCard(profile) }
                    item { SkillsSection(profile) }
                    item { TrustMetricsGrid(profile) }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
                StickyFooter(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    onEditProfile = onEditProfile,
                    onRefresh = onRefresh,
                    onLogout = onLogout
                )
            }
        }
    }
}

@Composable
fun HeroHeader(profile: ClientProfileDto) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(RoyalBlue, Color(0xFF3F51B5))
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = profile.profilePictureUrl?.let { url ->
                        url.replace("http://", "https://")
                           .replace(".heic", ".jpg")
                    } ?: "https://via.placeholder.com/150",
                    contentDescription = "Profile Picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .then(
                            if (profile.recommendationFlag) Modifier.background(Gold, CircleShape).padding(4.dp).clip(CircleShape)
                            else Modifier
                        )
                        .background(Color.White)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = profile.name ?: "User",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                if (profile.experienceLevel?.equals("Expert", ignoreCase = true) == true || profile.experienceLevel?.equals("Intermediate", ignoreCase = true) == true) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = "Verified",
                        tint = Gold,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Text(
                text = profile.experienceLevel ?: "N/A",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp
            )
        }

        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp)
                .offset(y = 20.dp)
                .fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "${profile.averageRating}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Icon(Icons.Default.Star, contentDescription = null, tint = Gold, modifier = Modifier.size(16.dp))
                    }
                    Text("Rating", fontSize = 12.sp, color = Color.Gray)
                }
                Box(modifier = Modifier.height(24.dp).width(1.dp).background(Color.LightGray))
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { (profile.jobSuccessRate / 100).toFloat() },
                            modifier = Modifier.size(40.dp),
                            color = SuccessGreen,
                            trackColor = LightBlueAccent,
                        )
                        Text(text = "${profile.jobSuccessRate.toInt()}%", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Success", fontSize = 12.sp, color = Color.Gray)
                }
                 Box(modifier = Modifier.height(24.dp).width(1.dp).background(Color.LightGray))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "$${profile.recommendedWagePerHour?.toInt() ?: 0}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("/hr", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun AiInsightCard(profile: ClientProfileDto) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 40.dp, bottom = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI Performance Summary", fontWeight = FontWeight.Bold, color = RoyalBlue)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = profile.aiGeneratedSummary ?: "No summary available yet.",
                color = Color.DarkGray,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            val keywords = profile.topReviewKeywords
            if (!keywords.isNullOrEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    keywords.take(3).forEach { keyword ->
                        SuggestionChip(
                            onClick = {},
                            label = { Text(keyword, fontSize = 12.sp, color = Color(0xFF2E7D32)) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = Color(0xFFE8F5E9),
                                labelColor = Color(0xFF2E7D32)
                            ),
                            border = null
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SkillsSection(profile: ClientProfileDto) {
    val skills = profile.skills
    if (!skills.isNullOrEmpty()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text("Skills & Expertise", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                skills.forEach { skill ->
                    Surface(
                        shape = CircleShape,
                        border = BorderStroke(1.dp, Color.LightGray),
                        color = Color.White
                    ) {
                        Text(
                            text = skill,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            fontSize = 12.sp,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TrustMetricsGrid(profile: ClientProfileDto) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Trust & Activity", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InfoCard(
                modifier = Modifier.weight(1f),
                title = "Profile Strength",
                value = "${profile.profileStrengthScore ?: 0}/100",
                icon = Icons.Default.Shield
            )
            InfoCard(
                modifier = Modifier.weight(1f),
                title = "Total Jobs",
                value = "${profile.totalReviews}",
                icon = Icons.Default.WorkHistory
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InfoCard(
                modifier = Modifier.weight(1f),
                title = "Location",
                value = "Live Map",
                icon = Icons.Default.LocationOn,
                isAction = true
            )
            InfoCard(
                modifier = Modifier.weight(1f),
                title = "Member Since",
                value = profile.createdAt?.take(4) ?: "N/A",
                icon = Icons.Default.CalendarMonth
            )
        }
    }
}

@Composable
fun InfoCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isAction: Boolean = false
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = if(isAction) RoyalBlue else Color.Gray, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if(isAction) RoyalBlue else Color.Black)
            Text(title, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun StickyFooter(
    modifier: Modifier = Modifier,
    onEditProfile: () -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 16.dp,
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onEditProfile,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Edit Profile", fontWeight = FontWeight.Bold)
            }
            
             OutlinedButton(
                onClick = onRefresh,
                modifier = Modifier
                    .size(48.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, RoyalBlue),
                contentPadding = PaddingValues(0.dp) 
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = RoyalBlue)
            }

            IconButton(onClick = onLogout) {
                Icon(Icons.Default.Logout, contentDescription = "Logout", tint = RoyalBlue)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
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
        skills = listOf("Android", "Kotlin", "Spring Boot", "Jetpack Compose", "Java"),
        profileStrengthScore = 92,
        totalReviews = 145,
        latitude = 0.0,
        longitude = 0.0,
        createdAt = "2023-01-15",
        lastAiUpdate = "2023-10-27",
        profileCompletionPercent = 100
    )
    ProHandsTheme {
        ProfileScreen(authState = AuthState(profile = dummyProfile))
    }
}
