package com.anand.prohands.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.anand.prohands.data.ClientProfileDto
import com.anand.prohands.data.JobResponse
import com.anand.prohands.network.JobService
import com.anand.prohands.network.ProfileApi
import com.anand.prohands.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val profileApi: ProfileApi,
    private val jobService: JobService
) : ViewModel() {

    private val _profile = MutableStateFlow<ClientProfileDto?>(null)
    val profile: StateFlow<ClientProfileDto?> = _profile

    private val _nearbyJobs = MutableStateFlow<List<JobResponse>>(emptyList())
    val nearbyJobs: StateFlow<List<JobResponse>> = _nearbyJobs

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun fetchProfile(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = profileApi.getProfile(userId)
                if (response.isSuccessful) {
                    _profile.value = response.body()
                    // If profile has location, fetch nearby jobs
                    val userProfile = response.body()
                    if (userProfile?.latitude != null && userProfile.longitude != null) {
                        fetchNearbyJobs(userProfile.latitude, userProfile.longitude)
                    }
                } else {
                    _error.value = "Error: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "An unexpected error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchNearbyJobs(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            try {
                val response = jobService.getJobFeed(latitude, longitude, 0, 50)
                if (response.isSuccessful) {
                    _nearbyJobs.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                // Silently fail or log for nearby jobs
            }
        }
    }
}

class ProfileViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            val profileApi = RetrofitClient.instance.create(ProfileApi::class.java)
            val jobService = RetrofitClient.instance.create(JobService::class.java)
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(profileApi, jobService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
