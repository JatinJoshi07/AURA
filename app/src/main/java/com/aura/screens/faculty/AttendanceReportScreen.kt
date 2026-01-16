package com.aura.screens.faculty

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.aura.components.LoadingAnimation
import com.aura.models.User
import com.aura.viewmodels.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceReportScreen(
    navController: NavController,
    adminViewModel: AdminViewModel = hiltViewModel()
) {
    val students by adminViewModel.userList.collectAsState()
    val isLoading by adminViewModel.isLoading.collectAsState()
    var selectedBatch by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        adminViewModel.loadAllStudents()
    }

    val filteredStudents = students.filter { student ->
        (selectedBatch == "All" || student.batchYear.toString() == selectedBatch) &&
        (student.name.contains(searchQuery, ignoreCase = true) || student.enrollmentNumber.contains(searchQuery, ignoreCase = true))
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Attendance Reports", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search and Filter Header
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search student name...") },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("All", "2022", "2023", "2024").forEach { batch ->
                            FilterChip(
                                selected = selectedBatch == batch,
                                onClick = { selectedBatch = batch },
                                label = { Text(batch) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }

            if (isLoading && students.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingAnimation()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            "Student Registry (${filteredStudents.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    
                    items(filteredStudents, key = { it.id }) { student ->
                        AttendanceStudentCard(student)
                    }
                    
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
fun AttendanceStudentCard(student: User) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Initials
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    student.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(student.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text("Enroll: ${student.enrollmentNumber}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text("Dept: ${student.department}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            
            // Attendance Percentage indicator (Mock data for now)
            Column(horizontalAlignment = Alignment.End) {
                val mockAttendance = (70..99).random() 
                Text(
                    "$mockAttendance%", 
                    fontWeight = FontWeight.ExtraBold, 
                    style = MaterialTheme.typography.titleMedium,
                    color = if(mockAttendance >= 75) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                )
                Text(
                    if(mockAttendance >= 75) "GOOD" else "LOW", 
                    fontSize = 10.sp, 
                    fontWeight = FontWeight.Bold,
                    color = if(mockAttendance >= 75) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                )
            }
        }
    }
}
