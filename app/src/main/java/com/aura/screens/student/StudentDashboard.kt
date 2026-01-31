package com.aura.screens.student

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.aura.components.*
import com.aura.models.Quiz
import com.aura.ui.theme.PrimaryGradient
import com.aura.ui.theme.SecondaryGradient
import com.aura.viewmodels.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboard(
    navController: NavController,
    authViewModel: AuthViewModel,
    emergencyViewModel: EmergencyViewModel = hiltViewModel(),
    projectViewModel: ProjectViewModel = hiltViewModel(),
    broadcastViewModel: BroadcastViewModel = hiltViewModel(),
    quizViewModel: QuizViewModel = hiltViewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val emergencies by emergencyViewModel.emergencies.collectAsState()
    val activeEmergency by emergencyViewModel.activeEmergency.collectAsState()
    val projects by projectViewModel.allProjects.collectAsState()
    val broadcasts by broadcastViewModel.broadcasts.collectAsState()
    val availableQuizzes by quizViewModel.availableQuizzes.collectAsState()

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            quizViewModel.loadQuizzesForStudent()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Decorative Header background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "AURA",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    actions = {
                        IconButton(onClick = { authViewModel.logout() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Logout",
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                        }
                    }
                )
            },
            floatingActionButton = {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // AI Companion FAB
                    SmallFloatingActionButton(
                        onClick = { navController.navigate("gemini_companion") },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.secondary,
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI Companion")
                    }

                    // SOS FAB
                    ExtendedFloatingActionButton(
                        onClick = { navController.navigate("emergency") },
                        icon = { Icon(Icons.Default.Emergency, contentDescription = null) },
                        text = { Text(if (activeEmergency != null) "SOS ACTIVE" else "SOS HELP") },
                        containerColor = if (activeEmergency != null) Color.Red else MaterialTheme.colorScheme.error,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(20.dp),
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                    )
                }
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                // Profile Section
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                            .clickable { navController.navigate("profile") },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Hello,",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            )
                            Text(
                                currentUser?.name?.split(" ")?.firstOrNull() ?: "Student",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            )
                        }

                        // Circular Profile Image with Border
                        Surface(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                            ,
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        ) {
                            if (currentUser?.profileImage != null) {
                                Image(
                                    painter = rememberAsyncImagePainter(currentUser?.profileImage),
                                    contentDescription = "Profile",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize().background(
                                        brush = Brush.linearGradient(PrimaryGradient)
                                    ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        currentUser?.name?.firstOrNull()?.toString() ?: "S",
                                        style = MaterialTheme.typography.titleLarge.copy(color = Color.White)
                                    )
                                }
                            }
                        }
                    }
                }

                // Active Quiz Alert (Highest Priority Notification)
                if (availableQuizzes.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .clickable { navController.navigate("student_quiz") },
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Row(
                                modifier = Modifier.padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Quiz, contentDescription = null, tint = Color.White)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "NEW QUIZ AVAILABLE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "${availableQuizzes.size} active assessment(s) found",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null)
                            }
                        }
                    }
                }

                // Active User SOS Alert
                activeEmergency?.let { emergency ->
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .clickable { navController.navigate("emergency") },
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red)
                                        Spacer(Modifier.width(12.dp))
                                        Text("YOUR SOS IS ACTIVE", color = Color.Red, fontWeight = FontWeight.Bold)
                                    }
                                    TextButton(onClick = { emergencyViewModel.updateEmergencyStatus(emergency.id, "resolved") }) {
                                        Text("Resolve", color = Color.Red)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Security and contacts have been notified. Help is on the way.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Black.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                // Quick Actions Grid
                item {
                    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                        Text(
                            "Quick Actions",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            DashboardActionCard(
                                title = "Emergency SOS",
                                icon = Icons.Default.Emergency,
                                gradient = listOf(Color(0xFFEF4444), Color(0xFF991B1B)),
                                onClick = { navController.navigate("emergency") },
                                modifier = Modifier.weight(1f)
                            )

                            DashboardActionCard(
                                title = "Quizzes",
                                icon = Icons.Default.Quiz,
                                gradient = listOf(Color(0xFF6366F1), Color(0xFF4338CA)),
                                onClick = { navController.navigate("student_quiz") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            DashboardActionCard(
                                title = "Project Hub",
                                icon = Icons.Default.Hub,
                                gradient = PrimaryGradient,
                                onClick = { navController.navigate("project_hub") },
                                modifier = Modifier.weight(1f)
                            )

                            DashboardActionCard(
                                title = "Wellness",
                                icon = Icons.Default.Mood,
                                gradient = SecondaryGradient,
                                onClick = { navController.navigate("wellness") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        // Row for Report Issue
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            DashboardActionCard(
                                title = "Report Issue",
                                icon = Icons.Default.Flag,
                                gradient = listOf(Color(0xFFF59E0B), Color(0xFFD97706)),
                                onClick = { navController.navigate("complaint") },
                                modifier = Modifier.weight(1f)
                            )
                            if (currentUser?.gender == "Female") {
                                DashboardActionCard(
                                    title = "Pink Shield",
                                    icon = Icons.Default.Security,
                                    gradient = listOf(Color(0xFFEC4899), Color(0xFFBE185D)),
                                    onClick = { navController.navigate("pink_shield") },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                // Active Alerts Section
                val filteredEmergencies = emergencies.filter { it.id != activeEmergency?.id }
                if (filteredEmergencies.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Active Emergencies",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                TextButton(onClick = { navController.navigate("emergency") }) {
                                    Text("View All", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primary))
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    items(filteredEmergencies.take(5), key = { it.id }) { emergency ->
                        EmergencyCard(
                            emergency = emergency,
                            onClick = { /* Handle click */ },
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // Projects Section
                if (projects.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Collaboration Hub",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                TextButton(onClick = { navController.navigate("project_hub") }) {
                                    Text("Explore", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primary))
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    items(projects.take(2), key = { it.id }) { projectItem ->
                        ProjectCard(
                            project = projectItem,
                            onClick = { /* Handle click */ },
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // Academic Status / Events
                item {
                    val latestEvent = broadcasts.firstOrNull { it.type == "event" || it.type == "exam" }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .clickable { navController.navigate("broadcast") },
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.AutoMirrored.Filled.EventNote, contentDescription = null, tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    "Upcoming Events",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    latestEvent?.title ?: "No academic events scheduled",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardActionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradient: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(110.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = Brush.linearGradient(gradient))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    modifier = Modifier.size(32.dp),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.padding(6.dp)
                    )
                }

                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
        }
    }
}
