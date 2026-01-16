package com.aura.screens.faculty

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.aura.components.*
import com.aura.models.InfrastructureIssue
import com.aura.viewmodels.AuthViewModel
import com.aura.viewmodels.FacultyViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfrastructureManagement(
    navController: NavController,
    authViewModel: AuthViewModel,
    facultyViewModel: FacultyViewModel = hiltViewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val assignedIssues by facultyViewModel.assignedIssues.collectAsState()
    val allIssues by facultyViewModel.allIssues.collectAsState()
    val isLoading by facultyViewModel.isLoading.collectAsState()
    
    var selectedIssue by remember { mutableStateOf<InfrastructureIssue?>(null) }
    var filterCategory by remember { mutableStateOf("all") }
    var filterStatus by remember { mutableStateOf("all") }
    var searchQuery by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf("Date") } // Date, Priority
    var showFilters by remember { mutableStateOf(false) }
    var isMapView by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        facultyViewModel.loadInfrastructureIssues()
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text("Infrastructure", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                        Text("Manage campus maintenance", style = MaterialTheme.typography.bodySmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { isMapView = !isMapView }) {
                        Icon(if (isMapView) Icons.Default.List else Icons.Default.Map, contentDescription = "Toggle View")
                    }
                    IconButton(onClick = { facultyViewModel.loadInfrastructureIssues() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search and Filter Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search building, room...") },
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        )
                        
                        FilledIconButton(
                            onClick = { showFilters = !showFilters },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (showFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Icon(Icons.Default.FilterList, null)
                        }
                    }

                    AnimatedVisibility(visible = showFilters) {
                        Column(modifier = Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Category Filter
                            Text("Category", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(listOf("all", "electrical", "plumbing", "furniture", "safety", "other")) { category ->
                                    FilterChip(
                                        selected = filterCategory == category,
                                        onClick = { filterCategory = category },
                                        label = { Text(category.replaceFirstChar { it.uppercase() }) }
                                    )
                                }
                            }

                            // Status Filter
                            Text("Status", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(listOf("all", "reported", "assigned", "in_progress", "resolved")) { status ->
                                    FilterChip(
                                        selected = filterStatus == status,
                                        onClick = { filterStatus = status },
                                        label = { Text(status.replace("_", " ").replaceFirstChar { it.uppercase() }) }
                                    )
                                }
                            }
                            
                            // Sort
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Sort by: ", style = MaterialTheme.typography.labelMedium)
                                Spacer(Modifier.width(8.dp))
                                AssistChip(
                                    onClick = { sortBy = if (sortBy == "Date") "Priority" else "Date" },
                                    label = { Text(sortBy) },
                                    leadingIcon = { Icon(Icons.Default.Sort, null, modifier = Modifier.size(16.dp)) }
                                )
                            }
                        }
                    }
                }
            }

            if (isLoading && allIssues.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val filteredIssues = allIssues.filter { issue ->
                    (filterCategory == "all" || issue.category == filterCategory) &&
                    (filterStatus == "all" || issue.status == filterStatus) &&
                    (searchQuery.isBlank() || issue.building.contains(searchQuery, true) || 
                     issue.description.contains(searchQuery, true) || issue.room.contains(searchQuery, true))
                }.let { list ->
                    if (sortBy == "Priority") {
                        list.sortedByDescending { getPriorityWeight(it.priority) }
                    } else {
                        list.sortedByDescending { it.createdAt }
                    }
                }

                if (isMapView) {
                    InfrastructureMapView(
                        issues = filteredIssues,
                        onIssueClick = { selectedIssue = it }
                    )
                } else {
                    if (filteredIssues.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Default.Inventory,
                            title = "No issues found",
                            description = "Adjust filters or search query"
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Quick Stats
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    StatBadge(
                                        label = "Pending",
                                        count = allIssues.count { it.status != "resolved" },
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.weight(1f)
                                    )
                                    StatBadge(
                                        label = "My Tasks",
                                        count = assignedIssues.count { it.status != "resolved" },
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    StatBadge(
                                        label = "Resolved",
                                        count = allIssues.count { it.status == "resolved" },
                                        color = Color(0xFF4CAF50),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            items(filteredIssues, key = { it.id }) { issue ->
                                EnhancedIssueCard(
                                    issue = issue,
                                    isAssignedToMe = issue.assignedTo == currentUser?.id,
                                    onClick = { selectedIssue = issue }
                                )
                            }
                            
                            item { Spacer(modifier = Modifier.height(80.dp)) }
                        }
                    }
                }
            }
        }
    }

    // Issue Detail Modal
    selectedIssue?.let { issue ->
        InfrastructureIssueDetailDialog(
            issue = issue,
            onDismiss = { selectedIssue = null },
            onUpdateStatus = { newStatus ->
                facultyViewModel.updateIssueStatus(issue.id, newStatus)
                selectedIssue = null
            }
        )
    }
}

@Composable
fun InfrastructureMapView(
    issues: List<InfrastructureIssue>,
    onIssueClick: (InfrastructureIssue) -> Unit
) {
    val context = LocalContext.current
    val cameraPositionState = rememberCameraPositionState {
        // Default to a central campus location
        position = CameraPosition.fromLatLngZoom(LatLng(28.6139, 77.2090), 16f)
    }

    // Attempt to center on first issue with location
    LaunchedEffect(issues) {
        issues.firstOrNull { it.location != null }?.location?.let {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(LatLng(it.latitude, it.longitude), 16f)
        }
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        issues.forEach { issue ->
            issue.location?.let { loc ->
                Marker(
                    state = MarkerState(position = LatLng(loc.latitude, loc.longitude)),
                    title = issue.building,
                    snippet = "${issue.category.uppercase()} - ${issue.status}",
                    onClick = {
                        onIssueClick(issue)
                        true
                    }
                )
            }
        }
    }
}

@Composable
fun StatBadge(label: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(count.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.8f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedIssueCard(
    issue: InfrastructureIssue,
    isAssignedToMe: Boolean,
    onClick: () -> Unit
) {
    val statusColor = when (issue.status) {
        "reported" -> Color(0xFFEF4444)
        "assigned" -> Color(0xFF3B82F6)
        "in_progress" -> Color(0xFFF59E0B)
        "resolved" -> Color(0xFF10B981)
        else -> Color.Gray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = onClick
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(statusColor)
            )
            
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            getCategoryIcon(issue.category),
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            issue.category.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            fontWeight = FontWeight.Bold
                        )
                        if (isAssignedToMe) {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            ) {
                                Text(
                                    "MY TASK",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                    color = Color.White
                                )
                            }
                        }
                    }
                    
                    Text(
                        issue.building,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        issue.description,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Place, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                        Text(
                            " Floor ${issue.floor}, Room ${issue.room}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            formatIssueTime(issue.createdAt.toDate()),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(icon: ImageVector, title: String, description: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, modifier = Modifier.size(80.dp), tint = Color.LightGray)
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(description, textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = Color.Gray)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfrastructureIssueDetailDialog(
    issue: InfrastructureIssue,
    onDismiss: () -> Unit,
    onUpdateStatus: (String) -> Unit
) {
    var selectedStatus by remember { mutableStateOf(issue.status) }
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(getCategoryIcon(issue.category), null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(issue.category.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Reported by ${issue.reportedByName}", style = MaterialTheme.typography.labelSmall)
                }
                Spacer(Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = getPriorityColor(issue.priority).copy(alpha = 0.1f)
                ) {
                    Text(
                        issue.priority.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = getPriorityColor(issue.priority)
                    )
                }
            }

            // Image and Description
            if (issue.imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = issue.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }

            Text(issue.description, style = MaterialTheme.typography.bodyLarge)

            // Location Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Location", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("${issue.building}, Floor ${issue.floor}", fontWeight = FontWeight.Bold)
                        Text("Room ${issue.room}", style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = {
                        val gmmIntentUri = if (issue.location != null) {
                            Uri.parse("geo:${issue.location.latitude},${issue.location.longitude}?q=${issue.location.latitude},${issue.location.longitude}(Issue)")
                        } else {
                            Uri.parse("geo:0,0?q=${Uri.encode("${issue.building} campus")}")
                        }
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                        mapIntent.setPackage("com.google.android.apps.maps")
                        context.startActivity(mapIntent)
                    }) {
                        Icon(Icons.Default.Directions, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // AI Insights
            issue.aiAnalysis?.let { ai ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4))
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF16A34A))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("AI Analysis", style = MaterialTheme.typography.labelSmall, color = Color(0xFF16A34A))
                            Text(ai.objectIdentified, fontWeight = FontWeight.Bold)
                            Text(ai.suggestedAction, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // Status Stepper
            Column {
                Text("Action Center", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val statusOptions = listOf("assigned", "in_progress", "resolved")
                    statusOptions.forEach { status ->
                        FilterChip(
                            selected = selectedStatus == status,
                            onClick = { selectedStatus = status },
                            label = { Text(status.replace("_", " ").uppercase(), fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Button(
                onClick = { onUpdateStatus(selectedStatus) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = selectedStatus != issue.status
            ) {
                Text("Update Implementation Status")
            }
        }
    }
}

private fun getPriorityColor(priority: String): Color {
    return when (priority.lowercase()) {
        "critical" -> Color(0xFFDC2626)
        "high" -> Color(0xFFF59E0B)
        "medium" -> Color(0xFF3B82F6)
        else -> Color(0xFF10B981)
    }
}

private fun getPriorityWeight(priority: String): Int {
    return when (priority.lowercase()) {
        "critical" -> 4
        "high" -> 3
        "medium" -> 2
        else -> 1
    }
}

private fun getCategoryIcon(category: String): ImageVector {
    return when (category.lowercase()) {
        "electrical" -> Icons.Default.FlashOn
        "plumbing" -> Icons.Default.WaterDrop
        "furniture" -> Icons.Default.Chair
        "safety" -> Icons.Default.Security
        else -> Icons.Default.Build
    }
}

private fun formatIssueTime(date: Date): String {
    val now = Date()
    val diff = now.time - date.time
    val minutes = diff / (60 * 1000)
    val hours = diff / (60 * 60 * 1000)
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes min ago"
        hours < 24 -> "$hours hours ago"
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(date)
    }
}

