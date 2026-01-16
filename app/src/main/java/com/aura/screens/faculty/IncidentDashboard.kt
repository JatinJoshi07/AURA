package com.aura.screens.faculty

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
        facultyViewModel.loadAssignedEmergencies()
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
                        facultyViewModel.loadAssignedEmergencies()
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
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when (emergency.priority) {
                            "critical" -> Color(0xFFFFCDD2)
                            "high" -> Color(0xFFFFECB3)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Text(
                            emergency.priority.replaceFirstChar { it.uppercase() },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Text(
                emergency.description,
                style = MaterialTheme.typography.bodyMedium
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
                        emergency.timestamp?.let { formatIncidentTime(it.toDate()) } ?: "Just now",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { /* View location */ },
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = "Location", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Location", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onResolve,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Resolve", fontSize = 12.sp)
                    }
                }
            }

            // Danger level
            if (emergency.dangerLevel > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = "Danger", modifier = Modifier.size(16.dp))
                    Text(
                        "Danger Level:",
                        style = MaterialTheme.typography.labelSmall
                    )
                    DangerLevelIndicator(level = emergency.dangerLevel)
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
    // Default camera position (college location)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(28.6139, 77.2090), 15f) // Delhi coordinates
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            emergencies.forEach { emergency ->
                emergency.location?.let { location ->
                    Marker(
                        state = MarkerState(
                            position = LatLng(location.latitude, location.longitude)
                        ),
                        title = emergency.description,
                        snippet = "Priority: ${emergency.priority}",
                        onClick = {
                            onEmergencySelect(emergency)
                            true
                        }
                    )
                }
            }
        }

        // Map controls
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color.Red)
                    )
                    Text("Critical", style = MaterialTheme.typography.labelSmall)

                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF9800))
                    )
                    Text("High", style = MaterialTheme.typography.labelSmall)

                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50))
                    )
                    Text("Normal", style = MaterialTheme.typography.labelSmall)
                }
            }
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
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Incident Details",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Type and Priority
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoChip(
                    label = emergency.type.replaceFirstChar { it.uppercase() },
                    icon = Icons.Default.Emergency
                )
                InfoChip(
                    label = emergency.priority.replaceFirstChar { it.uppercase() },
                    icon = Icons.Default.PriorityHigh,
                    color = when (emergency.priority) {
                        "critical" -> MaterialTheme.colorScheme.errorContainer
                        "high" -> Color(0xFFFFF3CD)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
                InfoChip(
                    label = emergency.status.replaceFirstChar { it.uppercase() },
                    icon = Icons.Default.Info,
                    color = when (emergency.status) {
                        "active" -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.primaryContainer
                    }
                )
            }

            // Description
            Column {
                Text(
                    "Description",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    emergency.description,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // User Information
            Column {
                Text(
                    "Reported By",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Card(
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
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                emergency.userName.firstOrNull()?.uppercase() ?: "U",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                        Column {
                            Text(
                                emergency.userName,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Text(
                                "Reported ${emergency.timestamp?.let { formatIncidentTime(it.toDate()) } ?: "Just now"}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }
                }
            }

            // Location
            emergency.location?.let {
                Column {
                    Text(
                        "Location",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.clickable { /* Open full map */ }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = "Location",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    emergency.address.ifEmpty { "College Campus" },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "Lat: ${it.latitude}, Lng: ${it.longitude}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                )
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = "View",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { /* Call student */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Call, contentDescription = "Call")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Call Student")
                }

                Button(
                    onClick = onResolve,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.DoneAll, contentDescription = "Resolve")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mark Resolved")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun InfoChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    AssistChip(
        onClick = {},
        label = {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium
                )
            )
        },
        leadingIcon = {
            Icon(
                icon,
                contentDescription = label,
                modifier = Modifier.size(16.dp)
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = color
        )
    )
}

private fun formatIncidentTime(date: Date): String {
    val now = Date()
    val diff = now.time - date.time
    val minutes = diff / (60 * 1000)
    val hours = diff / (60 * 60 * 1000)

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes minutes ago"
        hours < 24 -> "$hours hours ago"
        else -> SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(date)
    }
}
