package com.anand.prohands.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anand.prohands.data.JobRequest
import com.anand.prohands.data.JobResponse
import com.anand.prohands.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class JobViewModel : ViewModel() {

    private val _allJobs = MutableStateFlow<List<JobResponse>>(emptyList())
    val allJobs = _allJobs.asStateFlow()

    private val _myJobs = MutableStateFlow<List<JobResponse>>(emptyList())
    val myJobs = _myJobs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun fetchAllJobs() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = RetrofitClient.jobService.getAllJobs()
                if (response.isSuccessful) _allJobs.value = response.body() ?: emptyList()
            } catch (e: Exception) { /* Handle Error */ } 
            finally { _isLoading.value = false }
        }
    }

    fun fetchMyJobs(providerId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = RetrofitClient.jobService.getJobsByProvider(providerId)
                if (response.isSuccessful) _myJobs.value = response.body() ?: emptyList()
            } catch (e: Exception) { /* Handle Error */ }
            finally { _isLoading.value = false }
        }
    }

    fun deleteJob(jobId: String, providerId: String) {
        viewModelScope.launch {
            val response = RetrofitClient.jobService.deleteJob(jobId)
            if (response.isSuccessful) fetchMyJobs(providerId) // Refresh list
        }
    }

    fun updateJobStatus(jobId: String, status: String, providerId: String) {
        viewModelScope.launch {
            val response = RetrofitClient.jobService.updateStatus(jobId, status)
            if (response.isSuccessful) fetchMyJobs(providerId)
        }
    }

    fun updateJobDetails(jobId: String, request: JobRequest, providerId: String) {
        viewModelScope.launch {
            val response = RetrofitClient.jobService.updateJob(jobId, request)
            if (response.isSuccessful) fetchMyJobs(providerId)
        }
    }
    
    fun postJob(title: String, desc: String, wage: String, lat: Double, lng: Double, employees: String, providerId: String) {
        val request = JobRequest(
            providerId = providerId,
            title = title,
            description = desc,
            wage = wage.toDoubleOrNull() ?: 0.0, 
            latitude = lat,
            longitude = lng,
            requiredSkills = listOf("General"), 
            numberOfEmployees = employees.toIntOrNull() ?: 1
        )
        
        viewModelScope.launch {
            val response = RetrofitClient.jobService.createJob(request)
            if (response.isSuccessful) {
                // Handle success
            }
        }
    }
}