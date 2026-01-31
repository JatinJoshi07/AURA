package com.aura.screens.faculty

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.aura.models.Batch
import com.aura.models.User
import com.aura.viewmodels.FacultyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceManagementScreen(
    navController: NavController,
    viewModel: FacultyViewModel = hiltViewModel()
) {
    val batches by viewModel.myBatches.collectAsState()
    var selectedBatch by remember { mutableStateOf<Batch?>(null) }
    val students by viewModel.batchStudents.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val attendanceMap = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(selectedBatch) {
        selectedBatch?.let {
            viewModel.loadStudentsInBatch(it.studentIds)
        }
    }

    LaunchedEffect(students) {
        students.forEach { student ->
            if (!attendanceMap.containsKey(student.id)) {
                attendanceMap[student.id] = true // Default present
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Take Attendance") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (selectedBatch != null && students.isNotEmpty()) {
                        TextButton(onClick = {
                            viewModel.saveAttendance(selectedBatch!!.id, attendanceMap.toMap())
                            navController.popBackStack()
                        }) {
                            Text("Submit", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Batch Selector
            var expanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.padding(16.dp)) {
                OutlinedCard(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(selectedBatch?.name ?: "Select a Batch", style = MaterialTheme.typography.bodyLarge)
                        Icon(Icons.Default.Groups, null)
                    }
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    batches.forEach { batch ->
                        DropdownMenuItem(
                            text = { Text(batch.name) },
                            onClick = {
                                selectedBatch = batch
                                expanded = false
                            }
                        )
                    }
                }
            }

            if (selectedBatch == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Please select a batch to begin.", color = Color.Gray)
                }
            } else if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(students) { student ->
                        AttendanceItem(
                            student = student,
                            isPresent = attendanceMap[student.id] ?: true,
                            onToggle = { attendanceMap[student.id] = it }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AttendanceItem(student: User, isPresent: Boolean, onToggle: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isPresent) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(student.name, fontWeight = FontWeight.Bold)
                Text(student.enrollmentNumber, style = MaterialTheme.typography.bodySmall)
            }
            Switch(
                checked = isPresent,
                onCheckedChange = onToggle,
                thumbContent = {
                    if (isPresent) Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                }
            )
        }
    }
}
