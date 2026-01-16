package com.aura.screens.admin

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.aura.components.LoadingAnimation
import com.aura.models.*
import com.aura.viewmodels.AdminViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    navController: NavController,
    adminViewModel: AdminViewModel = hiltViewModel()
) {
    val stats by adminViewModel.dashboardStats.collectAsState()
    val userList by adminViewModel.userList.collectAsState()
    val emergencies by adminViewModel.recentEmergencies.collectAsState()
    val complaints by adminViewModel.recentComplaints.collectAsState()
    val infraIssues by adminViewModel.infrastructureIssues.collectAsState()
    val facultyList by adminViewModel.facultyList.collectAsState()
    val isLoading by adminViewModel.isLoading.collectAsState()
    val context = LocalContext.current
    
    var currentView by remember { mutableStateOf("summary") }
    var studentToDelete by remember { mutableStateOf<User?>(null) }
    var selectedComplaint by remember { mutableStateOf<Complaint?>(null) }
    var selectedInfraIssue by remember { mutableStateOf<InfrastructureIssue?>(null) }

    LaunchedEffect(Unit) {
        adminViewModel.loadDashboardData()
        adminViewModel.loadAllStudents()
        adminViewModel.loadAllEmergencies()
        adminViewModel.loadAllComplaints()
        adminViewModel.loadAllInfrastructureIssues()
        adminViewModel.loadFacultyList()
    }

    if (studentToDelete != null) {
        AlertDialog(
            onDismissRequest = { studentToDelete = null },
            title = { Text("Remove Student") },
            text = { Text("Are you sure you want to remove ${studentToDelete?.name}? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        studentToDelete?.let { 
                            adminViewModel.deleteStudent(it.id)
                            Toast.makeText(context, "Student removed", Toast.LENGTH_SHORT).show()
                        }
                        studentToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { studentToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        when(currentView) {
                            "users" -> "Registered Students"
                            "emergencies" -> "Active Emergencies"
                            "complaints" -> "Open Complaints"
                            "infra" -> "Infrastructure Issues"
                            else -> "System Analytics"
                        }, 
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (currentView != "summary") currentView = "summary" else navController.popBackStack() 
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading && stats.totalUsers == 0 && currentView == "summary") {
            LoadingAnimation()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when(currentView) {
                    "summary" -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(24.dp),
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            item {
                                Text("Usage Statistics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }

                            item {
                                AnalyticsCard(
                                    title = "User Engagement",
                                    value = "${stats.totalUsers}",
                                    label = "Registered Students",
                                    icon = Icons.Default.People,
                                    color = MaterialTheme.colorScheme.primary,
                                    onClick = { currentView = "users" }
                                )
                            }

                            item {
                                AnalyticsCard(
                                    title = "Safety Response",
                                    value = "${stats.activeEmergencies}",
                                    label = "Active Incidents",
                                    icon = Icons.Default.Emergency,
                                    color = MaterialTheme.colorScheme.error,
                                    onClick = { currentView = "emergencies" }
                                )
                            }

                            item {
                                AnalyticsCard(
                                    title = "Issue Resolution",
                                    value = "${stats.pendingComplaints}",
                                    label = "Open Complaints",
                                    icon = Icons.AutoMirrored.Filled.Assignment,
                                    color = MaterialTheme.colorScheme.secondary,
                                    onClick = { currentView = "complaints" }
                                )
                            }

                            item {
                                AnalyticsCard(
                                    title = "Infrastructure",
                                    value = "${stats.infrastructureIssues}",
                                    label = "Reported Maintenance",
                                    icon = Icons.Default.Build,
                                    color = Color(0xFFF59E0B),
                                    onClick = { currentView = "infra" }
                                )
                            }
                        }
                    }
                    "users" -> {
                        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (userList.isEmpty() && !isLoading) {
                                item { EmptyState(text = "No students registered", icon = Icons.Default.People) }
                            }
                            items(userList) { user -> 
                                UserListItem(user = user, onDelete = { studentToDelete = user }) 
                            }
                        }
                    }
                    "emergencies" -> {
                        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (emergencies.isEmpty() && !isLoading) {
                                item { EmptyState(text = "No active emergencies", icon = Icons.Default.CheckCircle) }
                            }
                            items(emergencies) { emergency -> EmergencyItem(emergency = emergency) }
                        }
                    }
                    "complaints" -> {
                        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (complaints.isEmpty() && !isLoading) {
                                item { EmptyState(text = "No open complaints", icon = Icons.Default.ThumbUp) }
                            }
                            items(complaints) { complaint -> 
                                ComplaintItem(
                                    complaint = complaint, 
                                    onClick = { selectedComplaint = complaint }
                                ) 
                            }
                        }
                    }
                    "infra" -> {
                        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (infraIssues.isEmpty() && !isLoading) {
                                item { EmptyState(text = "No infrastructure issues", icon = Icons.Default.House) }
                            }
                            items(infraIssues) { issue -> 
                                InfraItem(
                                    issue = issue,
                                    onClick = { selectedInfraIssue = issue }
                                ) 
                            }
                        }
                    }
                }
            }
        }
    }

    // Assignment Dialogs
    selectedComplaint?.let { complaint ->
        AdminComplaintDetailDialog(
            complaint = complaint,
            facultyList = facultyList,
            onDismiss = { selectedComplaint = null },
            onAssign = { facultyId ->
                adminViewModel.assignComplaint(complaint.id, facultyId)
                selectedComplaint = null
                Toast.makeText(context, "Complaint assigned to faculty", Toast.LENGTH_SHORT).show()
            }
        )
    }

    selectedInfraIssue?.let { issue ->
        AdminInfraDetailDialog(
            issue = issue,
            facultyList = facultyList,
            onDismiss = { selectedInfraIssue = null },
            onAssign = { facultyId ->
                adminViewModel.assignInfrastructureIssue(issue.id, facultyId)
                selectedInfraIssue = null
                Toast.makeText(context, "Issue assigned to faculty", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun EmptyState(text: String, icon: ImageVector) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        Spacer(Modifier.height(16.dp))
        Text(text, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
    }
}

@Composable
fun AnalyticsCard(title: String, value: String, label: String, icon: ImageVector, color: Color, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(color.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(label, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
        }
    }
}

@Composable
fun UserListItem(user: User, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Text(user.name.take(1).uppercase(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(user.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text("${user.role.uppercase()} • ${user.department}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Text(user.email, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Student", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun EmergencyItem(emergency: Emergency) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(emergency.type.uppercase(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.weight(1f))
                Text(SimpleDateFormat("hh:mm a", Locale.getDefault()).format(emergency.timestamp?.toDate()), style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(8.dp))
            Text("Reported by: ${emergency.userName}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(emergency.description, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun ComplaintItem(complaint: Complaint, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }, 
        shape = RoundedCornerShape(16.dp), 
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(complaint.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
            Text("Status: ${complaint.status.uppercase()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(4.dp))
            Text(complaint.description, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            Text("By: ${if(complaint.isAnonymous) "Anonymous" else complaint.userName}", style = MaterialTheme.typography.labelSmall)
            
            if (complaint.assignedTo != null) {
                Text("Assigned", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun InfraItem(issue: InfrastructureIssue, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }, 
        shape = RoundedCornerShape(16.dp), 
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row {
                Text(issue.category.uppercase(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                Spacer(Modifier.weight(1f))
                Text(issue.priority.uppercase(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
            }
            Text(issue.building, fontWeight = FontWeight.Bold)
            Text(issue.description, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            Text("Status: ${issue.status.replace("_", " ").uppercase()}", style = MaterialTheme.typography.labelSmall)

            if (issue.assignedTo != null) {
                Text("Assigned", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}
