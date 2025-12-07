package com.anand.prohands.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.anand.prohands.data.JobResponse
import com.anand.prohands.network.JobService
import com.anand.prohands.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ManageJobsViewModel(private val jobService: JobService) : ViewModel() {

    private val _jobs = MutableStateFlow<List<JobResponse>>(emptyList())
    val jobs: StateFlow<List<JobResponse>> = _jobs

    fun fetchJobs(providerId: String) {
        viewModelScope.launch {
            try {
                val response = jobService.getJobsByProvider(providerId)
                if (response.isSuccessful) {
                    _jobs.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}

class ManageJobsViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ManageJobsViewModel::class.java)) {
            val jobService = RetrofitClient.instance.create(JobService::class.java)
            @Suppress("UNCHECKED_CAST")
            return ManageJobsViewModel(jobService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}