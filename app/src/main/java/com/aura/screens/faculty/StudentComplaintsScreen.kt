package com.aura.screens.faculty

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.aura.components.LoadingAnimation
import com.aura.models.Complaint
import com.aura.viewmodels.AuthViewModel
import com.aura.viewmodels.FacultyViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentComplaintsScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    facultyViewModel: FacultyViewModel = hiltViewModel()
) {
    val complaints by facultyViewModel.allComplaints.collectAsState()
    val isLoading by facultyViewModel.isLoading.collectAsState()
    var filterStatus by remember { mutableStateOf("pending") }
    
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        facultyViewModel.loadAllComplaints()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Student Complaints",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { facultyViewModel.loadAllComplaints() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                scope.launch {
                    isRefreshing = true
                    facultyViewModel.loadAllComplaints()
                    delay(1000)
                    isRefreshing = false
                }
            },
            modifier = Modifier.padding(paddingValues)
        ) {
            if (isLoading && !isRefreshing) {
                LoadingAnimation()
            } else {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Filter Status
                    ScrollableTabRow(
                        selectedTabIndex = when(filterStatus) {
                            "pending" -> 0
                            "in_review" -> 1
                            "resolved" -> 2
                            else -> 0
                        },
                        edgePadding = 16.dp,
                        containerColor = Color.Transparent,
                        divider = {}
                    ) {
                        listOf("pending", "in_review", "resolved").forEach { status ->
                            Tab(
                                selected = filterStatus == status,
                                onClick = { filterStatus = status },
                                text = { Text(status.replace("_", " ").uppercase()) }
                            )
                        }
                    }

                    if (complaints.filter { it.status == filterStatus }.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No $filterStatus complaints found", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(complaints.filter { it.status == filterStatus }) { complaint ->
                                ComplaintFacultyCard(
                                    complaint = complaint,
                                    onUpdateStatus = { newStatus ->
                                        facultyViewModel.updateComplaintStatus(complaint.id, newStatus)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ComplaintFacultyCard(
    complaint: Complaint,
    onUpdateStatus: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (complaint.priority) {
                "high" -> Color(0xFFFFF3CD)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(complaint.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when(complaint.priority) {
                        "high" -> Color.Red
                        else -> Color.Gray
                    }
                ) {
                    Text(
                        complaint.priority.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }

            Text(complaint.description, style = MaterialTheme.typography.bodyMedium)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                Spacer(Modifier.width(4.dp))
                Text(
                    if (complaint.isAnonymous) "Anonymous" else complaint.userName,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Spacer(Modifier.weight(1f))
                Text(
                    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(complaint.timestamp.toDate()),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }

            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (complaint.status != "in_review") {
                    Button(
                        onClick = { onUpdateStatus("in_review") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Review", fontSize = 12.sp)
                    }
                }
                if (complaint.status != "resolved") {
                    Button(
                        onClick = { onUpdateStatus("resolved") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Resolve", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
