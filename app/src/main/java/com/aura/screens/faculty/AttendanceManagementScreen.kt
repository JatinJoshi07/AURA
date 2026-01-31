package com.aura.screens.faculty

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                attendanceMap[student.id] = true // Default to present
            }
        }
    }

    val presentCount = attendanceMap.values.count { it }
    val absentCount = attendanceMap.values.count { !it }
    val totalCount = attendanceMap.size

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

            if (selectedBatch != null && totalCount > 0) {
                // Today's Stats Summary
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Today's Live Stats",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatMiniItem(label = "Total", value = totalCount.toString(), color = MaterialTheme.colorScheme.onSurface)
                            StatMiniItem(label = "Present", value = presentCount.toString(), color = Color(0xFF2E7D32))
                            StatMiniItem(label = "Absent", value = absentCount.toString(), color = Color(0xFFD32F2F))
                        }
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
                            onMarkPresent = { attendanceMap[student.id] = true },
                            onMarkAbsent = { attendanceMap[student.id] = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatMiniItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.7f))
    }
}

@Composable
fun AttendanceItem(
    student: User,
    isPresent: Boolean,
    onMarkPresent: () -> Unit,
    onMarkAbsent: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(student.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text(student.enrollmentNumber, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Absent Button
                Button(
                    onClick = onMarkAbsent,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isPresent) Color(0xFFD32F2F) else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (!isPresent) Color.White else Color.Gray
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Absent", fontSize = 12.sp)
                }
                
                // Present Button
                Button(
                    onClick = onMarkPresent,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPresent) Color(0xFF2E7D32) else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isPresent) Color.White else Color.Gray
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Present", fontSize = 12.sp)
                }
            }
        }
    }
}
