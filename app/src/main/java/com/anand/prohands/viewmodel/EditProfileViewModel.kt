package com.anand.prohands.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anand.prohands.data.ClientProfileDto
import com.anand.prohands.network.ProfileApi
import com.anand.prohands.network.RetrofitClient
import kotlinx.coroutines.launch
import okhttp3.MultipartBody

class EditProfileViewModel : ViewModel() {

    private val profileApi: ProfileApi = RetrofitClient.profileApi

    var profile by mutableStateOf<ClientProfileDto?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var isImageUploading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    fun loadProfile(userId: String) {
        viewModelScope.launch {
            isLoading = true
            error = null
            try {
                val response = profileApi.getProfile(userId)
                if (response.isSuccessful) {
                    profile = response.body()
                } else {
                    error = "Error loading profile: ${response.message()}"
                }
            } catch (e: Exception) {
                error = "Error loading profile: ${e.message}"
            }
            isLoading = false
        }
    }

    fun updateProfile(userId: String, updatedProfile: ClientProfileDto, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            isLoading = true
            error = null
            try {
                val response = profileApi.updateProfile(userId, updatedProfile)
                if (response.isSuccessful) {
                    profile = response.body()
                    onResult(true, null)
                } else {
                    error = "Error updating profile: ${response.message()}"
                    onResult(false, error)
                }
            } catch (e: Exception) {
                error = "Error updating profile: ${e.message}"
                onResult(false, error)
            }
            isLoading = false
        }
    }

    fun uploadProfilePicture(userId: String, file: MultipartBody.Part, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            isImageUploading = true
            error = null
            try {
                val response = profileApi.uploadProfilePicture(userId, file)
                if (response.isSuccessful) {
                    profile = response.body()
                    onResult(true, null)
                } else {
                    error = "Error uploading profile picture: ${response.message()}"
                    onResult(false, error)
                }
            } catch (e: Exception) {
                error = "Error uploading profile picture: ${e.message}"
                onResult(false, error)
            }
            isImageUploading = false
        }
    }
}
