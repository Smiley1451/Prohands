package com.anand.prohands.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.anand.prohands.ui.theme.* // Import theme colors
import com.anand.prohands.viewmodel.AuthViewModel
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    viewModel: AuthViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val profile = state.profile ?: return

    var name by remember { mutableStateOf(profile.name ?: "") }
    var phone by remember { mutableStateOf(profile.phone ?: "") }
    var skillsText by remember { mutableStateOf(profile.skills?.joinToString(", ") ?: "") }
    var latitude by remember { mutableStateOf(profile.latitude?.toString() ?: "") }
    var longitude by remember { mutableStateOf(profile.longitude?.toString() ?: "") }

    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(it)
            val tempFile = File.createTempFile("profile_pic", ".jpg", context.cacheDir)
            val outputStream = FileOutputStream(tempFile)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()

            val requestFile = tempFile.asRequestBody("image/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", tempFile.name, requestFile)
            viewModel.uploadProfilePicture(body)
        }
    }

    LaunchedEffect(state.updateSuccess) {
        if (state.updateSuccess) {
            Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
            viewModel.clearStatusFlags()
            onNavigateBack()
        }
    }

    LaunchedEffect(state.uploadSuccess) {
        if (state.uploadSuccess) {
            Toast.makeText(context, "Profile picture updated!", Toast.LENGTH_SHORT).show()
            viewModel.clearStatusFlags()
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val updatedProfile = profile.copy(
                            name = name,
                            phone = phone,
                            skills = skillsText.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                            latitude = latitude.toDoubleOrNull(),
                            longitude = longitude.toDoubleOrNull()
                        )
                        viewModel.updateProfile(updatedProfile)
                    }) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = RoyalBlue,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Check, contentDescription = "Save", tint = RoyalBlue)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                val imageUrl = remember(state.profile?.profilePictureUrl, state.uploadSuccess) {
                    val url = state.profile?.profilePictureUrl ?: "https://via.placeholder.com/150"
                    if (url.contains("?")) "$url&t=${System.currentTimeMillis()}" else "$url?t=${System.currentTimeMillis()}"
                }
                
                Image(
                    painter = rememberAsyncImagePainter(
                        model = imageUrl
                    ),
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray),
                    contentScale = ContentScale.Crop
                )
                
                if (state.isLoading && !state.updateSuccess) { 
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
                    }
                }

                IconButton(
                    onClick = { launcher.launch("image/*") },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(RoyalBlue)
                        .size(40.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Change Picture", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = TextStyle(color = Color.Black),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RoyalBlue,
                    focusedLabelColor = RoyalBlue,
                    cursorColor = Color.Black
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                textStyle = TextStyle(color = Color.Black),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RoyalBlue,
                    focusedLabelColor = RoyalBlue,
                    cursorColor = Color.Black
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = skillsText,
                onValueChange = { skillsText = it },
                label = { Text("Skills (comma separated)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                textStyle = TextStyle(color = Color.Black),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RoyalBlue,
                    focusedLabelColor = RoyalBlue,
                    cursorColor = Color.Black
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = latitude,
                    onValueChange = { latitude = it },
                    label = { Text("Latitude") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    textStyle = TextStyle(color = Color.Black),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RoyalBlue,
                        focusedLabelColor = RoyalBlue,
                        cursorColor = Color.Black
                    )
                )
                OutlinedTextField(
                    value = longitude,
                    onValueChange = { longitude = it },
                    label = { Text("Longitude") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    textStyle = TextStyle(color = Color.Black),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RoyalBlue,
                        focusedLabelColor = RoyalBlue,
                        cursorColor = Color.Black
                    )
                )
            }
        }
    }
}
