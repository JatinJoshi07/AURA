package com.aura.screens.faculty

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import com.aura.components.*
import com.aura.models.Emergency
import com.aura.viewmodels.AuthViewModel
import com.aura.viewmodels.FacultyViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentDashboard(
    navController: NavController,
    authViewModel: AuthViewModel,
    facultyViewModel: FacultyViewModel = hiltViewModel()
) {
    val assignedEmergencies by facultyViewModel.assignedEmergencies.collectAsState()
    val isLoading by facultyViewModel.isLoading.collectAsState()
    var selectedEmergency by remember { mutableStateOf<Emergency?>(null) }
    var showMap by remember { mutableStateOf(false) }
    var filterStatus by remember { mutableStateOf("all") }
    
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        facultyViewModel.loadDashboardData()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Incident Dashboard",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showMap = !showMap }) {
                        Icon(
                            if (showMap) Icons.AutoMirrored.Filled.List else Icons.Default.Map,
                            contentDescription = "Toggle View"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading && !isRefreshing) {
            LoadingAnimation()
        } else {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    scope.launch {
                        isRefreshing = true
                        facultyViewModel.loadDashboardData()
                        delay(1000)
                        isRefreshing = false
                    }
                },
                modifier = Modifier.padding(paddingValues)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (showMap) {
                        IncidentMapView(
                            emergencies = assignedEmergencies,
                            onEmergencySelect = { emergency ->
                                selectedEmergency = emergency
                            }
                        )
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Filter chips
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("all", "active", "resolved", "critical").forEach { status ->
                                    FilterChip(
                                        selected = filterStatus == status,
                                        onClick = { filterStatus = status },
                                        label = {
                                            Text(status.replaceFirstChar { it.uppercase() })
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = when (status) {
                                                "active" -> MaterialTheme.colorScheme.errorContainer
                                                "critical" -> Color(0xFFFFCDD2)
                                                "resolved" -> MaterialTheme.colorScheme.primaryContainer
                                                else -> MaterialTheme.colorScheme.surfaceVariant
                                            }
                                        ),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }

                            // Statistics card
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    IncidentStat(
                                        title = "Total Assigned",
                                        value = assignedEmergencies.size.toString(),
                                        icon = Icons.Default.Emergency
                                    )
                                    IncidentStat(
                                        title = "Active Now",
                                        value = assignedEmergencies.count { it.status == "active" }.toString(),
                                        icon = Icons.Default.Warning
                                    )
                                    IncidentStat(
                                        title = "Resolved Today",
                                        value = assignedEmergencies.count { it.status == "resolved" }.toString(),
                                        icon = Icons.Default.DoneAll
                                    )
                                }
                            }

                            // Incidents list
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val filteredEmergencies = when (filterStatus) {
                                    "active" -> assignedEmergencies.filter { it.status == "active" }
                                    "resolved" -> assignedEmergencies.filter { it.status == "resolved" }
                                    "critical" -> assignedEmergencies.filter { it.priority == "critical" }
                                    else -> assignedEmergencies
                                }

                                if (filteredEmergencies.isEmpty()) {
                                    item {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(32.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Emergency,
                                                contentDescription = "No Incidents",
                                                modifier = Modifier.size(80.dp),
                                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text(
                                                "No Assigned Incidents",
                                                style = MaterialTheme.typography.titleLarge.copy(
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                )
                                            )
                                            Text(
                                                "All incidents are handled",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                )
                                            )
                                        }
                                    }
                                } else {
                                    items(filteredEmergencies, key = { it.id }) { emergency ->
                                        IncidentDetailCard(
                                            emergency = emergency,
                                            onResolve = { 
                                                facultyViewModel.resolveEmergency(emergency.id) 
                                            },
                                            onViewDetails = { selectedEmergency = emergency }
                                        )
                                    }
                                }

                                item { Spacer(modifier = Modifier.height(32.dp)) }
                            }
                        }
                    }
                }
            }
        }

        // Emergency Detail Bottom Sheet
        selectedEmergency?.let { emergency ->
            EmergencyDetailBottomSheet(
                emergency = emergency,
                onDismiss = { selectedEmergency = null },
                onResolve = {
                    facultyViewModel.resolveEmergency(emergency.id)
                    selectedEmergency = null
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyDetailBottomSheet(
    emergency: Emergency,
    onDismiss: () -> Unit,
    onResolve: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Emergency Details",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Info items
            DetailItem(label = "Type", value = emergency.type.replaceFirstChar { it.uppercase() })
            DetailItem(label = "Priority", value = emergency.priority.replaceFirstChar { it.uppercase() })
            DetailItem(label = "Status", value = emergency.status.replaceFirstChar { it.uppercase() })
            DetailItem(label = "Reported By", value = emergency.userName)
            DetailItem(
                label = "Time",
                value = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()).format(emergency.timestamp?.toDate() ?: Date())
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text("Description", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(emergency.description, style = MaterialTheme.typography.bodyMedium)

            if (emergency.status == "active") {
                Button(
                    onClick = onResolve,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Mark as Resolved")
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun IncidentStat(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            title,
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        )
    }
}

@Composable
fun IncidentDetailCard(
    emergency: Emergency,
    onResolve: () -> Unit,
    onViewDetails: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onViewDetails() },
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
                            .size(12.dp)
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
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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

                if (emergency.status == "active") {
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
}

@Composable
fun IncidentMapView(
    emergencies: List<Emergency>,
    onEmergencySelect: (Emergency) -> Unit
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(19.0760, 72.8777), 12f)
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        emergencies.forEach { emergency ->
            emergency.location?.let { loc ->
                Marker(
                    state = MarkerState(position = LatLng(loc.latitude, loc.longitude)),
                    title = emergency.type,
                    snippet = "By: ${emergency.userName}",
                    onClick = {
                        onEmergencySelect(emergency)
                        true
                    }
                )
            }
        }
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
