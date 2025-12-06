package com.anand.prohands.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anand.prohands.data.JobRequest
import com.anand.prohands.data.JobResponse
import com.anand.prohands.viewmodel.JobViewModel

// --- Professional Color Palette ---
val BrandBlue = Color(0xFF0D47A1)    // Professional Trust
val BrandTeal = Color(0xFF009688)    // Action/Success
val StatusOrange = Color(0xFFFF9800) // In Progress
val BgLight = Color(0xFFF5F7FA)      // Clean Background

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobScreen(
    currentUserId: String, // Ensure you pass this from NavHost
    onNavigateToCreateJob: () -> Unit
) {
    val viewModel: JobViewModel = viewModel()
    var selectedTab by remember { mutableStateOf(0) } // 0 = Find Work, 1 = Manage
    val tabs = listOf("Find Work", "Manage Postings")

    // Fetch data when tab changes
    LaunchedEffect(selectedTab) {
        if (selectedTab == 0) viewModel.fetchAllJobs()
        else viewModel.fetchMyJobs(currentUserId)
    }

    val allJobs by viewModel.allJobs.collectAsState()
    val myJobs by viewModel.myJobs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // State for Edit Dialog
    var showEditDialog by remember { mutableStateOf(false) }
    var jobToEdit by remember { mutableStateOf<JobResponse?>(null) }

    if (showEditDialog && jobToEdit != null) {
        EditJobDialog(
            job = jobToEdit!!,
            onDismiss = { showEditDialog = false },
            onConfirm = { updatedRequest ->
                viewModel.updateJobDetails(jobToEdit!!.jobId, updatedRequest, currentUserId)
                showEditDialog = false
            }
        )
    }

    Scaffold(
        containerColor = BgLight,
        topBar = {
            Column(Modifier.background(Color.White)) {
                CenterAlignedTopAppBar(
                    title = { Text("Job Hub", fontWeight = FontWeight.Bold, color = BrandBlue) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
                )
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.White,
                    contentColor = BrandBlue,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = BrandTeal,
                            height = 3.dp
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontWeight = FontWeight.SemiBold) },
                            selectedContentColor = BrandTeal,
                            unselectedContentColor = Color.Gray
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            // FAB only visible in "Manage Postings" tab
            if (selectedTab == 1) {
                ExtendedFloatingActionButton(
                    onClick = onNavigateToCreateJob,
                    containerColor = BrandTeal,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.Add, "Post Job") },
                    text = { Text("Post New Job") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = BrandTeal)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val list = if (selectedTab == 0) allJobs else myJobs

                    if (list.isEmpty()) {
                        item {
                            EmptyState(if (selectedTab == 0) "No jobs available nearby." else "You haven't posted any jobs yet.")
                        }
                    } else {
                        items(list) { job ->
                            if (selectedTab == 1) {
                                // --- PROVIDER VIEW (Manage) ---
                                MyJobManageCard(
                                    job = job,
                                    onEdit = { 
                                        jobToEdit = job
                                        showEditDialog = true 
                                    },
                                    onDelete = { viewModel.deleteJob(job.jobId, currentUserId) },
                                    onToggleStatus = { 
                                        val newStatus = if (job.status == "OPEN") "CLOSED" else "OPEN"
                                        viewModel.updateJobStatus(job.jobId, newStatus, currentUserId)
                                    }
                                )
                            } else {
                                // --- WORKER VIEW (Find) ---
                                // Don't show my own jobs in the "Find Work" tab to avoid confusion
                                if (job.providerId != currentUserId) {
                                    WorkerJobCard(job)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- 1. WORKER CARD (Simple, Information Heavy) ---
@Composable
fun WorkerJobCard(job: JobResponse) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(job.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(job.wage?.let { "$$it/hr" } ?: "N/A", color = BrandTeal, fontWeight = FontWeight.Bold)
            }
            Text(job.description ?: "No description", color = Color.Gray, maxLines = 2, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                Text(" View Map", style = MaterialTheme.typography.labelMedium, color = BrandBlue)
            }
        }
    }
}

// --- 2. MANAGER CARD (Control Heavy) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyJobManageCard(
    job: JobResponse,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleStatus: () -> Unit
) {
    val isOpen = job.status == "OPEN"
    
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            // Header with Status Chip
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(job.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                AssistChip(
                    onClick = onToggleStatus,
                    label = { Text(if (isOpen) "OPEN" else "CLOSED") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (isOpen) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                        labelColor = if (isOpen) Color(0xFF2E7D32) else Color(0xFFC62828)
                    ),
                    leadingIcon = {
                        Icon(
                            if (isOpen) Icons.Default.CheckCircle else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (isOpen) Color(0xFF2E7D32) else Color(0xFFC62828),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }

            Text("Posted on: ${job.createdAt.take(10)}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Spacer(Modifier.height(12.dp))
            
            Divider(color = Color.LightGray.copy(alpha = 0.5f))
            
            // Action Buttons Row
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp), 
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Edit Button
                OutlinedButton(onClick = onEdit, contentPadding = PaddingValues(horizontal = 12.dp)) {
                    Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Edit")
                }
                Spacer(Modifier.width(8.dp))
                // Delete Button
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, null, tint = Color.Red)
                }
            }
        }
    }
}

// --- 3. EDIT DIALOG ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditJobDialog(
    job: JobResponse,
    onDismiss: () -> Unit,
    onConfirm: (JobRequest) -> Unit
) {
    var title by remember { mutableStateOf(job.title) }
    var description by remember { mutableStateOf(job.description ?: "") }
    var wage by remember { mutableStateOf(job.wage?.toString() ?: "") }
    var employees by remember { mutableStateOf(job.numberOfEmployees?.toString() ?: "1") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Edit Job", style = MaterialTheme.typography.headlineSmall, color = BrandBlue, fontWeight = FontWeight.Bold)
                
                OutlinedTextField(
                    value = title, 
                    onValueChange = { title = it }, 
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = description, 
                    onValueChange = { description = it }, 
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = wage, 
                        onValueChange = { wage = it }, 
                        label = { Text("Wage ($)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = employees, 
                        onValueChange = { employees = it }, 
                        label = { Text("Workers") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Spacer(Modifier.height(8.dp))
                
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) } 
                    Button(
                        onClick = {
                            val request = JobRequest(
                                providerId = job.providerId,
                                title = title,
                                description = description,
                                wage = wage.toDoubleOrNull() ?: 0.0,
                                latitude = job.latitude, // Keep original loc
                                longitude = job.longitude, // Keep original loc
                                numberOfEmployees = employees.toIntOrNull() ?: 1,
                                requiredSkills = job.requiredSkills
                            )
                            onConfirm(request)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                    ) {
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState(text: String) {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, color = Color.Gray)
    }
}

@Composable
fun PostJobScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Post Job Screen")
    }
}
