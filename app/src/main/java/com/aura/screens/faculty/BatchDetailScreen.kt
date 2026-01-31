package com.aura.screens.faculty

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.aura.models.Batch
import com.aura.models.User
import com.aura.viewmodels.FacultyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchDetailScreen(
    batchId: String,
    navController: NavController,
    viewModel: FacultyViewModel = hiltViewModel()
) {
    val batches by viewModel.myBatches.collectAsState()
    val batch = batches.find { it.id == batchId }
    val students by viewModel.batchStudents.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(batch) {
        batch?.let {
            viewModel.loadStudentsInBatch(it.studentIds)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(batch?.name ?: "Batch Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (batch == null) {
                Text("Batch not found", modifier = Modifier.align(Alignment.Center))
            } else if (students.isEmpty() && !isLoading) {
                Text("No students in this batch", modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Department: ${batch.department}", style = MaterialTheme.typography.bodyLarge)
                                Text("Year: ${batch.year}", style = MaterialTheme.typography.bodyLarge)
                                Text("Total Students: ${batch.studentIds.size}", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        Text("Student List", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    items(students) { student ->
                        StudentItem(student)
                    }
                }
            }

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
fun StudentItem(student: User) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(student.name, style = MaterialTheme.typography.bodyLarge)
                Text(student.enrollmentNumber, style = MaterialTheme.typography.bodySmall)
                Text(student.email, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
