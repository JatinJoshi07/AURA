package com.aura.screens.admin

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.aura.components.LoadingAnimation
import com.aura.components.LoadingType
import com.aura.models.*
import com.aura.viewmodels.AdminViewModel
import com.aura.viewmodels.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboard(
    navController: NavController,
    authViewModel: AuthViewModel,
    adminViewModel: AdminViewModel = hiltViewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val statistics by adminViewModel.dashboardStats.collectAsState()
    val recentEmergencies by adminViewModel.recentEmergencies.collectAsState()
    val recentComplaints by adminViewModel.recentComplaints.collectAsState()
    val profileRequests by adminViewModel.profileRequests.collectAsState()
    val systemHealth by adminViewModel.systemHealth.collectAsState()
    val adminAddress by adminViewModel.adminLocationAddress.collectAsState()
    val isLoading by adminViewModel.isLoading.collectAsState()
    val context = LocalContext.current
    
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        adminViewModel.loadDashboardData()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Admin Dashboard",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
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
                onClick = { navController.navigate("broadcast") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(Icons.Default.Campaign, contentDescription = "Broadcast")
            }
        }
    ) { paddingValues ->
        if (isLoading && !isRefreshing) {
            LoadingAnimation(type = LoadingType.Aura)
        } else {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    scope.launch {
                        isRefreshing = true
                        adminViewModel.loadDashboardData()
                        delay(1000)
                        isRefreshing = false
                    }
                },
                modifier = Modifier.padding(paddingValues)
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
                                    .padding(start = 16.dp, top = 16.dp, end = 16.dp)
                                    .clickable { navController.navigate("profile") },
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Welcome, Admin",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                            )
                                        )
                                        Text(
                                            currentUser?.name ?: "Administrator",
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        
                                        // Admin Current Address Display
                                        adminAddress?.let { address ->
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                                                Icon(Icons.Default.MyLocation, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    address,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    maxLines = 2
                                                )
                                            }
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.horizontalGradient(
                                                    colors = listOf(
                                                        MaterialTheme.colorScheme.primary,
                                                        MaterialTheme.colorScheme.secondary
                                                    )
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "A",
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Profile Requests Notification
                    if (profileRequests.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .clickable { navController.navigate("profile_requests") },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.VerifiedUser, null, tint = MaterialTheme.colorScheme.error)
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        "${profileRequests.size} Pending Profile Verifications",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(Modifier.weight(1f))
                                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                                }
                            }
                        }
                    }

                    // Quick Stats
                    item {
                        Text(
                            "System Overview",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            StatCard(
                                title = "Active SOS",
                                value = statistics.activeEmergencies.toString(),
                                icon = Icons.Default.Emergency,
                                color = MaterialTheme.colorScheme.errorContainer,
                                onClick = { navController.navigate("analytics") },
                                modifier = Modifier.weight(1f)
                            )

                            StatCard(
                                title = "Total Users",
                                value = statistics.totalUsers.toString(),
                                icon = Icons.Default.People,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                onClick = { navController.navigate("analytics") },
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
                            StatCard(
                                title = "Pending Complaints",
                                value = statistics.pendingComplaints.toString(),
                                icon = Icons.Default.Report,
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                onClick = { navController.navigate("analytics") },
                                modifier = Modifier.weight(1f)
                            )

                            StatCard(
                                title = "Infrastructure Issues",
                                value = statistics.infrastructureIssues.toString(),
                                icon = Icons.Default.Build,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                onClick = { navController.navigate("analytics") },
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
                            QuickActionButton(
                                title = "Manage Faculty",
                                description = "Add or remove faculty members",
                                icon = Icons.Default.School,
                                onClick = { navController.navigate("manage_faculty") }
                            )

                            QuickActionButton(
                                title = "Send Broadcast",
                                description = "Send announcements to all users",
                                icon = Icons.Default.Campaign,
                                onClick = { navController.navigate("broadcast") }
                            )

                            QuickActionButton(
                                title = "Profile Requests",
                                description = "Review identity verification requests",
                                icon = Icons.Default.VerifiedUser,
                                onClick = { navController.navigate("profile_requests") }
                            )

                            QuickActionButton(
                                title = "System Settings",
                                description = "Configure system parameters",
                                icon = Icons.Default.Settings,
                                onClick = { navController.navigate("settings") }
                            )
                        }
                    }

                    // Active Emergencies
                    if (recentEmergencies.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Active Emergencies",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                TextButton(onClick = { navController.navigate("analytics") }) {
                                    Text("View All")
                                }
                            }
                        }

                        items(recentEmergencies.take(3)) { emergency ->
                            EmergencyAdminCard(
                                emergency = emergency,
                                onResolve = { adminViewModel.resolveEmergency(emergency.id) },
                                onOpenMap = {
                                    emergency.location?.let { loc ->
                                        val uri = "geo:${loc.latitude},${loc.longitude}?q=${loc.latitude},${loc.longitude}(SOS)"
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                                        intent.setPackage("com.google.android.apps.maps")
                                        context.startActivity(intent)
                                    }
                                }
                            )
                        }
                    }

                    // System Health
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
                                        Icons.Default.HealthAndSafety,
                                        contentDescription = "System Health",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        "System Health",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    HealthIndicator(
                                        title = "Server",
                                        status = systemHealth.serverStatus,
                                        isHealthy = systemHealth.serverHealth > 90,
                                        value = "${systemHealth.serverHealth}%",
                                        modifier = Modifier.weight(1f)
                                    )

                                    HealthIndicator(
                                        title = "Database",
                                        status = systemHealth.databaseStatus,
                                        isHealthy = systemHealth.databaseHealth > 80,
                                        value = "${systemHealth.databaseHealth}%",
                                        modifier = Modifier.weight(1f)
                                    )

                                    HealthIndicator(
                                        title = "AI Services",
                                        status = systemHealth.aiStatus,
                                        isHealthy = systemHealth.aiHealth > 90,
                                        value = "${systemHealth.aiHealth}%",
                                        modifier = Modifier.weight(1f)
                                    )
                                    
                                    HealthIndicator(
                                        title = "Maps SDK",
                                        status = systemHealth.mapsStatus,
                                        isHealthy = systemHealth.mapsHealth > 90,
                                        value = "${systemHealth.mapsHealth}%",
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
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
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
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

@Composable
fun QuickActionButton(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
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
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
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
fun EmergencyAdminCard(
    emergency: Emergency,
    onResolve: () -> Unit,
    onOpenMap: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (emergency.priority) {
                "critical" -> Color(0xFFFFE5E5)
                "high" -> Color(0xFFFFF3CD)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (emergency.type) {
                            "voice" -> Icons.Default.Mic
                            "manual" -> Icons.Default.Emergency
                            "infrastructure" -> Icons.Default.Warning
                            else -> Icons.Default.Error
                        },
                        contentDescription = "Type",
                        tint = Color.Red,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        emergency.type.uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                    )
                }
                
                IconButton(onClick = onOpenMap) {
                    Icon(Icons.Default.LocationOn, "Open Map", tint = MaterialTheme.colorScheme.primary)
                }
            }
            
            Text(
                "By: ${emergency.userName}",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                emergency.description,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2
            )
            
            Spacer(Modifier.height(12.dp))
            
            Button(
                onClick = onResolve,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Mark as Resolved", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun HealthIndicator(
    title: String,
    status: String,
    isHealthy: Boolean,
    value: String,
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
                .background(
                    if (isHealthy) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isHealthy) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                )
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold
            )
        )
        Text(
            status,
            style = MaterialTheme.typography.labelSmall.copy(
                color = if (isHealthy) Color(0xFF2E7D32) else Color(0xFFD32F2F)
            )
        )
    }
}
