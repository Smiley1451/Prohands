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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.anand.prohands.data.JobResponse
import com.anand.prohands.ui.theme.ProColors
import com.anand.prohands.viewmodel.ManageJobsViewModel
import com.anand.prohands.viewmodel.ManageJobsViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Posted Jobs", color = ProColors.OnPrimary, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ProColors.Primary)
            )
        },
        containerColor = ProColors.Background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(jobs) { job ->
                ProviderJobCard(job = job, navController = navController)
            }
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
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = ProColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = job.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ProColors.OnSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Status: ${job.status}",
                style = MaterialTheme.typography.bodyMedium,
                color = ProColors.Secondary
            )

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = job.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ProColors.OnSurface
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            navController.navigate(
                                "worker_recommendations/${job.jobId}/${job.latitude}/${job.longitude}/${job.title}"
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ProColors.Primary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Find Workers", color = ProColors.OnPrimary)
                    }
                    Button(
                        onClick = { Toast.makeText(context, "Ping sent to workers!", Toast.LENGTH_SHORT).show() },
                        colors = ButtonDefaults.buttonColors(containerColor = ProColors.Secondary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Ping", color = ProColors.OnSecondary)
                    }
                }
            }
        }
    }
}
