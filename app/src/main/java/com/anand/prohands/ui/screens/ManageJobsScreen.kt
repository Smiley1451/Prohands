package com.anand.prohands.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.anand.prohands.data.JobResponse
import com.anand.prohands.viewmodel.ManageJobsViewModel
import com.anand.prohands.viewmodel.ManageJobsViewModelFactory

@Composable
fun ManageJobsScreen(
    navController: NavController,
    currentUserId: String,
    viewModel: ManageJobsViewModel = viewModel(factory = ManageJobsViewModelFactory())
) {
    val jobs by viewModel.jobs.collectAsState()

    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotEmpty()) {
            viewModel.fetchJobs(currentUserId)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(jobs) { job ->
            ProviderJobCard(job = job, navController = navController)
        }
    }
}

@Composable
fun ProviderJobCard(job: JobResponse, navController: NavController) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = job.title)
            Text(text = "Status: ${job.status}")

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = job.description)
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Button(onClick = {
                        navController.navigate(
                            "worker_recommendations/${job.jobId}/${job.latitude}/${job.longitude}/${job.title}"
                        )
                    }) {
                        Text("Find Workers")
                    }
                    Button(onClick = { Toast.makeText(context, "Ping sent to workers!", Toast.LENGTH_SHORT).show() }) {
                        Text("Ping")
                    }
                }
            }
        }
    }
}