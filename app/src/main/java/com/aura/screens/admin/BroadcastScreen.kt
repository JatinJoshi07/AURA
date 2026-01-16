package com.aura.screens.admin

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.aura.components.GradientButton
import com.aura.components.LoadingAnimation
import com.aura.components.LoadingType
import com.aura.models.Broadcast
import com.aura.viewmodels.AdminViewModel
import com.aura.viewmodels.AuthViewModel
import com.aura.viewmodels.BroadcastViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BroadcastScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    adminViewModel: AdminViewModel = hiltViewModel(),
    broadcastViewModel: BroadcastViewModel = hiltViewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val isAdmin = currentUser?.role == "admin"
    
    // Admins use AdminViewModel for sending/stats, Students use BroadcastViewModel for just viewing
    val broadcasts by if (isAdmin) adminViewModel.broadcasts.collectAsState() 
                    else broadcastViewModel.broadcasts.collectAsState()
    
    val isLoading by if (isAdmin) adminViewModel.isLoading.collectAsState()
                    else broadcastViewModel.isLoading.collectAsState()
                    
    var showSendDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (isAdmin) adminViewModel.loadBroadcasts()
        else broadcastViewModel.loadBroadcasts()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (isAdmin) "Campus Broadcast" else "Campus Announcements",
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
                    if (isAdmin) {
                        IconButton(onClick = { showSendDialog = true }) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Broadcast")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            LoadingAnimation(type = LoadingType.Wave)
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // Admin only Stats card
                    if (isAdmin) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            "Broadcast Center",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                            )
                                        )
                                        Text(
                                            "${broadcasts.size} Messages",
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        Text(
                                            "Sent to all campus users",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Icon(
                                        Icons.Default.Campaign,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        // Send broadcast button
                        item {
                            GradientButton(
                                text = "Send New Broadcast",
                                onClick = { showSendDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                icon = Icons.AutoMirrored.Filled.Send
                            )
                        }
                    } else {
                        // Student welcome info
                        item {
                            Text(
                                "Stay updated with latest campus activities, exam schedules, and emergency alerts.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                ),
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                            )
                        }
                    }

                    // Recent broadcasts
                    if (broadcasts.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.NotificationsNone,
                                    contentDescription = null,
                                    modifier = Modifier.size(80.dp),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "No Announcements",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                )
                            }
                        }
                    } else {
                        item {
                            Text(
                                "Recent Updates",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }

                        items(broadcasts, key = { it.id }) { broadcast ->
                            BroadcastCard(broadcast = broadcast)
                        }
                    }
                }
            }
        }
    }

    // Send Broadcast Dialog (Admin only)
    if (showSendDialog && isAdmin) {
        SendBroadcastDialog(
            currentUserName = currentUser?.name ?: "Admin",
            onDismiss = { showSendDialog = false },
            onSendBroadcast = { title, message, type, priority, audience ->
                adminViewModel.sendBroadcast(title, message, type, priority, audience)
                showSendDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BroadcastCard(broadcast: Broadcast) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (broadcast.type) {
                "emergency" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                "exam" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                "event" -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
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
                                when (broadcast.priority) {
                                    "high", "critical" -> Color.Red
                                    "medium" -> Color(0xFFFF9800)
                                    else -> Color(0xFF4CAF50)
                                }
                            )
                    )
                    Text(
                        broadcast.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (broadcast.type) {
                        "emergency" -> MaterialTheme.colorScheme.error
                        "exam" -> MaterialTheme.colorScheme.primary
                        "event" -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                ) {
                    Text(
                        broadcast.type.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall.copy(color = Color.White),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                broadcast.message,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 20.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "From: ${broadcast.sentByName}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )

                Text(
                    formatBroadcastTime(broadcast.createdAt.toDate()),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendBroadcastDialog(
    currentUserName: String,
    onDismiss: () -> Unit,
    onSendBroadcast: (String, String, String, String, List<String>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("general") }
    var selectedPriority by remember { mutableStateOf("normal") }
    var selectedAudience by remember { mutableStateOf(listOf("all")) }
    var isSending by remember { mutableStateOf(false) }

    val broadcastTypes = listOf(
        "emergency" to Icons.Default.Emergency,
        "exam" to Icons.Default.School,
        "event" to Icons.Default.Event,
        "general" to Icons.Default.Info
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "New Campus Broadcast",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Announcement Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Message Content") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Type", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(broadcastTypes) { (type, icon) ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                                label = { Text(type.replaceFirstChar { it.uppercase() }) },
                                leadingIcon = { Icon(icon, null, modifier = Modifier.size(18.dp)) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && message.isNotBlank()) {
                        isSending = true
                        onSendBroadcast(title, message, selectedType, selectedPriority, selectedAudience)
                    }
                },
                enabled = title.isNotBlank() && message.isNotBlank() && !isSending,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSending) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Text("Broadcast Now")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatBroadcastTime(date: Date): String {
    val now = Date()
    val diff = now.time - date.time
    val minutes = diff / (60 * 1000)
    val hours = diff / (60 * 60 * 1000)

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes min ago"
        hours < 24 -> "$hours hours ago"
        else -> SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(date)
    }
}
