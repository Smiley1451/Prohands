package com.anand.prohands.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.anand.prohands.viewmodel.EditProfileViewModel
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

// --- Professional Theme Colors (Consistent with your Profile Screen) ---
object ProEditColors {
    val PrimaryBlue = Color(0xFF2563EB)
    val SurfaceBg = Color(0xFFF8FAFC)
    val TextPrimary = Color(0xFF1E293B)
    val TextSecondary = Color(0xFF64748B)
    val White = Color.White
    val InputBg = Color(0xFFFFFFFF)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavController,
    userId: String
) {
    val editProfileViewModel: EditProfileViewModel = viewModel()
    val context = LocalContext.current

    // State
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var skills by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isSaving by remember { mutableStateOf(false) } // State for Save Button loading

    // Image Picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                imageUri = it
                val stream = context.contentResolver.openInputStream(it)
                val requestBody = stream?.readBytes()?.toRequestBody("image/jpeg".toMediaTypeOrNull())
                val multipartBody = requestBody?.let { body ->
                    MultipartBody.Part.createFormData("file", "profile.jpg", body)
                }
                multipartBody?.let { part ->
                    editProfileViewModel.uploadProfilePicture(userId, part) { _, _ -> }
                }
            }
        }
    )

    // Load Data
    LaunchedEffect(key1 = userId) {
        editProfileViewModel.loadProfile(userId)
    }

    val profile = editProfileViewModel.profile
    LaunchedEffect(key1 = profile) {
        profile?.let {
            name = it.name ?: ""
            phone = it.phone ?: ""
            skills = it.skills?.joinToString(", ") ?: ""
        }
    }

    Scaffold(
        containerColor = ProEditColors.SurfaceBg,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Edit Profile", color = ProEditColors.TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ProEditColors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = ProEditColors.SurfaceBg
                )
            )
        }
    ) { paddingValues ->
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()) // Allow scrolling for small screens
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .imePadding(), // Prevent keyboard from covering content
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- 1. Image Upload Section ---
            Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier
                    .size(140.dp)
                    .clickable { imagePickerLauncher.launch("image/*") }
            ) {
                // Profile Image
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color.Gray.copy(alpha = 0.1f))
                        .border(BorderStroke(4.dp, ProEditColors.White), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (editProfileViewModel.isImageUploading) {
                        CircularProgressIndicator(color = ProEditColors.PrimaryBlue)
                    } else {
                        Image(
                            painter = rememberAsyncImagePainter(imageUri ?: profile?.profilePictureUrl ?: "https://via.placeholder.com/150"),
                            contentDescription = "Profile Picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Camera Icon Badge
                Surface(
                    shape = CircleShape,
                    color = ProEditColors.PrimaryBlue,
                    border = BorderStroke(2.dp, ProEditColors.White),
                    modifier = Modifier.size(40.dp).offset(x = (-4).dp, y = (-4).dp),
                    shadowElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.CameraAlt,
                            contentDescription = "Edit Image",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- 2. Form Fields ---
            
            ProTextField(
                value = name,
                onValueChange = { name = it },
                label = "Full Name",
                icon = Icons.Outlined.Person,
                keyboardType = KeyboardType.Text
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            ProTextField(
                value = phone,
                onValueChange = { phone = it },
                label = "Phone Number",
                icon = Icons.Default.Phone,
                keyboardType = KeyboardType.Phone
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            ProTextField(
                value = skills,
                onValueChange = { skills = it },
                label = "Skills (comma-separated)",
                placeholder = "e.g., Electrician, Plumber",
                icon = Icons.Default.Build, // Tool icon
                singleLine = false,
                maxLines = 3
            )
            
            Spacer(modifier = Modifier.height(40.dp))

            // --- 3. Save Button ---
            Button(
                onClick = {
                    isSaving = true
                    profile?.let {
                        val updatedProfile = it.copy(
                            name = name,
                            phone = phone,
                            skills = skills.split(",").map { s -> s.trim() }.filter { s -> s.isNotEmpty() }
                        )
                        editProfileViewModel.updateProfile(userId, updatedProfile) { _, _ ->
                            isSaving = false
                            navController.previousBackStackEntry?.savedStateHandle?.set("shouldRefresh", true)
                            navController.popBackStack()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ProEditColors.PrimaryBlue,
                    disabledContainerColor = ProEditColors.PrimaryBlue.copy(alpha = 0.6f)
                ),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Saving...")
                } else {
                    Text("Save Changes", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// --- Reusable Component for Consistent Text Fields ---
@Composable
fun ProTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    maxLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = if (placeholder != null) { { Text(placeholder, color = Color.Gray) } } else null,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = ProEditColors.TextPrimary,
            unfocusedTextColor = ProEditColors.TextPrimary,
            focusedBorderColor = ProEditColors.PrimaryBlue,
            unfocusedBorderColor = Color.LightGray,
            focusedLabelColor = ProEditColors.PrimaryBlue,
            unfocusedContainerColor = ProEditColors.InputBg,
            focusedContainerColor = ProEditColors.InputBg
        ),
        leadingIcon = {
            Icon(imageVector = icon, contentDescription = null, tint = ProEditColors.TextSecondary)
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = ImeAction.Next
        ),
        singleLine = singleLine,
        maxLines = maxLines
    )
}
