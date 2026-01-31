package com.aura.screens.faculty

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.aura.components.LoadingAnimation
import com.aura.components.LoadingType
import com.aura.models.*
import com.aura.viewmodels.AuthViewModel
import com.aura.viewmodels.FacultyViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacultyDashboard(
    navController: NavController,
    authViewModel: AuthViewModel,
    facultyViewModel: FacultyViewModel = hiltViewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val dashboardStats by facultyViewModel.dashboardStats.collectAsState()
    val assignedEmergencies by facultyViewModel.assignedEmergencies.collectAsState()
    val assignedIssues by facultyViewModel.assignedIssues.collectAsState()
    val isLoading by facultyViewModel.isLoading.collectAsState()
    
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val pullToRefreshState = rememberPullToRefreshState()

    LaunchedEffect(Unit) {
        facultyViewModel.loadDashboardData()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Faculty Dashboard",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                actions = {
                    IconButton(onClick = { authViewModel.logout() }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("gemini_companion") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "AI Companion")
            }
        }
    ) { paddingValues ->
        if (isLoading && !isRefreshing) {
            LoadingAnimation(type = LoadingType.Pulse)
        } else {
            PullToRefreshBox(
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
                onRefresh = {
                    scope.launch {
                        isRefreshing = true
                        facultyViewModel.loadDashboardData()
                        delay(1500)
                        isRefreshing = false
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Welcome Section
                    item {
                        AnimatedVisibility(
                            visible = currentUser != null,
                            enter = fadeIn() + slideInVertically()
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .padding(top = 16.dp)
                                    .clickable { navController.navigate("profile") },
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            "Welcome back,",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                            )
                                        )
                                        Text(
                                            "Prof. ${currentUser?.name?.split(" ")?.firstOrNull() ?: "Faculty"}",
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "${currentUser?.department ?: "Faculty"} Department",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.horizontalGradient(
                                                    colors = listOf(
                                                        MaterialTheme.colorScheme.secondary,
                                                        MaterialTheme.colorScheme.tertiary
                                                    )
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.School,
                                            contentDescription = "Faculty",
                                            tint = Color.White,
                                            modifier = Modifier.size(30.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Quick Stats
                    item {
                        Text(
                            "Your Overview",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            FacultyStatCard(
                                title = "Assigned Incidents",
                                value = dashboardStats.assignedIncidents.toString(),
                                icon = Icons.Default.Emergency,
                                color = MaterialTheme.colorScheme.errorContainer,
                                onClick = { navController.navigate("incident_dashboard") },
                                modifier = Modifier.weight(1f)
                            )

                            FacultyStatCard(
                                title = "Pending Issues",
                                value = dashboardStats.pendingIssues.toString(),
                                icon = Icons.Default.Build,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                onClick = { navController.navigate("infrastructure_management") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(top = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            FacultyStatCard(
                                title = "Resolved Today",
                                value = dashboardStats.resolvedToday.toString(),
                                icon = Icons.Default.DoneAll,
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                onClick = { /* View resolved */ },
                                modifier = Modifier.weight(1f)
                            )

                            FacultyStatCard(
                                title = "Response Time",
                                value = "${dashboardStats.avgResponseTime}m",
                                icon = Icons.Default.Timer,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                onClick = { /* View analytics */ },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Quick Actions
                    item {
                        Text(
                            "Quick Actions",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FacultyActionButton(
                                title = "Manage Batches",
                                description = "Create and manage student groups",
                                icon = Icons.Default.Group,
                                onClick = { navController.navigate("manage_batches") }
                            )

                            FacultyActionButton(
                                title = "Take Attendance",
                                description = "Mark daily student presence",
                                icon = Icons.Default.HowToReg,
                                onClick = { navController.navigate("take_attendance") }
                            )

                            FacultyActionButton(
                                title = "Quiz Management",
                                description = "Create and monitor assessments",
                                icon = Icons.Default.Quiz,
                                onClick = { navController.navigate("quiz_list") }
                            )

                            FacultyActionButton(
                                title = "Performance Analytics",
                                description = "Compare academic health",
                                icon = Icons.AutoMirrored.Filled.TrendingUp,
                                onClick = { navController.navigate("batch_comparison") }
                            )

                            FacultyActionButton(
                                title = "Incident Dashboard",
                                description = "Manage assigned emergencies",
                                icon = Icons.Default.Emergency,
                                onClick = { navController.navigate("incident_dashboard") }
                            )

                            FacultyActionButton(
                                title = "Infrastructure Management",
                                description = "Handle maintenance issues",
                                icon = Icons.Default.Build,
                                onClick = { navController.navigate("infrastructure_management") }
                            )

                            FacultyActionButton(
                                title = "Student Complaints",
                                description = "Review anonymous complaints",
                                icon = Icons.Default.Report,
                                onClick = { navController.navigate("student_complaints") }
                            )
                            
                            FacultyActionButton(
                                title = "Attendance Reports",
                                description = "View class attendance",
                                icon = Icons.AutoMirrored.Filled.Assignment,
                                onClick = { navController.navigate("attendance_report") }
                            )
                        }
                    }

                    // Assigned Emergencies
                    if (assignedEmergencies.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Assigned Emergencies",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                TextButton(onClick = { navController.navigate("incident_dashboard") }) {
                                    Text("View All")
                                }
                            }
                        }

                        items(assignedEmergencies.take(2)) { emergency ->
                            FacultyEmergencyCard(
                                emergency = emergency,
                                onClick = { /* View details */ },
                                onResolve = { facultyViewModel.resolveEmergency(emergency.id) }
                            )
                        }
                    }

                    // Assigned Infrastructure Issues
                    if (assignedIssues.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Assigned Issues",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                TextButton(onClick = { navController.navigate("infrastructure_management") }) {
                                    Text("View All")
                                }
                            }
                        }

                        items(assignedIssues.take(2)) { issue ->
                            FacultyIssueCard(
                                issue = issue,
                                onClick = { /* View details */ },
                                onUpdate = { facultyViewModel.updateIssueStatus(issue.id, "in_progress") }
                            )
                        }
                    }

                    // Today's Schedule
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Schedule,
                                        contentDescription = "Schedule",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        "Today's Schedule",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }

                                // Sample schedule items
                                ScheduleItem(
                                    time = "9:00 AM - 10:30 AM",
                                    subject = "Data Structures",
                                    room = "CS-101",
                                    batch = "CSE 3rd Year"
                                )

                                ScheduleItem(
                                    time = "11:00 AM - 12:30 PM",
                                    subject = "Algorithms Lab",
                                    room = "CS-Lab-2",
                                    batch = "CSE 4th Year"
                                )

                                ScheduleItem(
                                    time = "2:00 PM - 3:30 PM",
                                    subject = "Faculty Meeting",
                                    room = "Conference Hall",
                                    batch = "All Faculty"
                                )
                            }
                        }
                    }

                    // Performance Metrics
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.TrendingUp,
                                        contentDescription = "Performance",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        "Performance Metrics",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    PerformanceMetric(
                                        title = "Response Rate",
                                        value = "${dashboardStats.responseRate}%",
                                        icon = Icons.Default.Speed,
                                        modifier = Modifier.weight(1f)
                                    )

                                    PerformanceMetric(
                                        title = "Resolution Rate",
                                        value = "${dashboardStats.resolutionRate}%",
                                        icon = Icons.Default.DoneAll,
                                        modifier = Modifier.weight(1f)
                                    )

                                    PerformanceMetric(
                                        title = "Avg. Rating",
                                        value = "${dashboardStats.avgRating}/5",
                                        icon = Icons.Default.Star,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }
        }
    }
}

@Composable
fun FacultyStatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(120.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    icon,
                    contentDescription = title,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    value,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 2
            )
        }
    }
}

@Composable
fun FacultyActionButton(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Go",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun FacultyEmergencyCard(
    emergency: Emergency,
    onClick: () -> Unit,
    onResolve: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (emergency.priority) {
                "critical" -> Color(0xFFFFE5E5)
                "high" -> Color(0xFFFFF3CD)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                when (emergency.priority) {
                                    "critical" -> Color.Red
                                    "high" -> Color(0xFFFF9800)
                                    else -> Color(0xFF4CAF50)
                                }
                            )
                    )
                    Text(
                        emergency.type.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (emergency.status) {
                        "active" -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.primaryContainer
                    }
                ) {
                    Text(
                        emergency.status.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                emergency.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Reported by: ${emergency.userName}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    )
                    Text(
                        emergency.timestamp?.let { formatEmergencyTime(it.toDate()) } ?: "Just now",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    )
                }

                Button(
                    onClick = onResolve,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Mark Resolved", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun FacultyIssueCard(
    issue: InfrastructureIssue,
    onClick: () -> Unit,
    onUpdate: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (issue.priority) {
                "critical" -> Color(0xFFFFE5E5)
                "high" -> Color(0xFFFFF3CD)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Build,
                        contentDescription = "Issue",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        issue.category.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
                Text(
                    issue.description.take(40) + if (issue.description.length > 40) "..." else "",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
                Text(
                    "${issue.building}, ${issue.floor} Floor",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )
            }

            Button(
                onClick = onUpdate,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = Color.White
                ),
                modifier = Modifier.height(36.dp)
            ) {
                Text("Start Work", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun ScheduleItem(
    time: String,
    subject: String,
    room: String,
    batch: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    time.split(" ").firstOrNull()?.take(5) ?: "",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    subject,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Text(
                    "$room • $batch",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )
            }

            Icon(
                Icons.Default.Notifications,
                contentDescription = "Reminder",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun PerformanceMetric(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            title,
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            ),
            textAlign = TextAlign.Center
        )
    }
}

private fun formatEmergencyTime(date: Date): String {
    val now = Date()
    val diff = now.time - date.time
    val minutes = diff / (60 * 1000)

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes min ago"
        else -> SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date)
    }
}
