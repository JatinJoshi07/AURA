package com.aura.screens.student

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.aura.components.*
import com.aura.models.WellnessCheck
import com.aura.viewmodels.AuthViewModel
import com.aura.viewmodels.WellnessViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WellnessScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    wellnessViewModel: WellnessViewModel = hiltViewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val myWellnessChecks by wellnessViewModel.myWellnessChecks.collectAsState()
    val campusMood by wellnessViewModel.campusMood.collectAsState()
    val isLoading by wellnessViewModel.isLoading.collectAsState()

    var selectedMood by remember { mutableIntStateOf(-1) }
    var showMoodSelector by remember { mutableStateOf(false) }
    var showCalendar by remember { mutableStateOf(false) }
    
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(currentUser) {
        currentUser?.id?.let { wellnessViewModel.loadMyWellnessChecks(it) }
        wellnessViewModel.loadCampusMood()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Wellness Tracker",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                    currentUser?.id?.let { wellnessViewModel.loadMyWellnessChecks(it) }
                    wellnessViewModel.loadCampusMood()
                    delay(1000)
                    isRefreshing = false
                }
            },
            modifier = Modifier.padding(paddingValues)
        ) {
            if (isLoading && !isRefreshing) {
                LoadingAnimation(type = LoadingType.Pulse)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // Welcome and Mood Check-in
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(top = 16.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    Icons.Default.Mood,
                                    contentDescription = "Wellness",
                                    modifier = Modifier.size(60.dp),
                                    tint = MaterialTheme.colorScheme.secondary
                                )

                                Text(
                                    "How are you feeling today?",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    textAlign = TextAlign.Center
                                )

                                Text(
                                    "Your response is completely anonymous. Regular check-ins help us understand campus wellness.",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    ),
                                    textAlign = TextAlign.Center
                                )

                                MoodSelector(
                                    selectedMood = selectedMood,
                                    onMoodSelected = { moodIndex ->
                                        selectedMood = moodIndex
                                        showMoodSelector = true
                                    }
                                )
                            }
                        }
                    }

                    // Campus Mood Overview
                    item {
                        Text(
                            "Campus Mood Overview",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Today's Campus Mood",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                    Text(
                                        "${String.format(Locale.getDefault(), "%.1f", campusMood.averageMood)}/5",
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = getMoodColor(campusMood.averageMood)
                                        )
                                    )
                                }

                                // Mood distribution
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(1, 2, 3, 4, 5).forEach { moodLevel ->
                                        MoodDistributionBar(
                                            moodLevel = moodLevel,
                                            percentage = when (moodLevel) {
                                                1 -> campusMood.veryPoor
                                                2 -> campusMood.poor
                                                3 -> campusMood.neutral
                                                4 -> campusMood.good
                                                5 -> campusMood.excellent
                                                else -> 0f
                                            }
                                        )
                                    }
                                }

                                // Stats
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    WellnessStat(
                                        title = "Total Checks",
                                        value = campusMood.totalChecks.toString(),
                                        icon = Icons.Default.Group,
                                        modifier = Modifier.weight(1f)
                                    )
                                    WellnessStat(
                                        title = "Avg. Mood",
                                        value = String.format(Locale.getDefault(), "%.1f", campusMood.averageMood),
                                        icon = Icons.Default.TrendingUp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    WellnessStat(
                                        title = "Positive",
                                        value = "${campusMood.positivePercentage}%",
                                        icon = Icons.Default.Mood,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // My Wellness History
                    if (myWellnessChecks.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "My Wellness History",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                OutlinedButton(
                                    onClick = { showCalendar = true },
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("View Calendar")
                                }
                            }
                        }

                        items(myWellnessChecks.take(7), key = { it.id }) { check ->
                            WellnessCheckCard(check = check)
                        }
                    }

                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }
        }
    }

    // Mood Submission Dialog
    if (showMoodSelector) {
        MoodSubmissionDialog(
            selectedMood = selectedMood,
            onDismiss = { 
                showMoodSelector = false
                selectedMood = -1
            },
            onSubmit = { mood, note ->
                wellnessViewModel.submitWellnessCheck(mood, note)
                showMoodSelector = false
                selectedMood = -1
            }
        )
    }

    // Calendar View
    if (showCalendar) {
        WellnessCalendarDialog(
            wellnessChecks = myWellnessChecks,
            onDismiss = { showCalendar = false }
        )
    }
}

@Composable
fun MoodSelector(
    selectedMood: Int,
    onMoodSelected: (Int) -> Unit
) {
    val moods = listOf(
        "😔" to "Very Poor",
        "😐" to "Poor",
        "🙂" to "Neutral",
        "😊" to "Good",
        "🤩" to "Excellent"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        moods.forEachIndexed { index, (emoji, label) ->
            MoodEmojiButton(
                emoji = emoji,
                label = label,
                isSelected = selectedMood == (index + 1),
                onClick = { onMoodSelected(index + 1) }
            )
        }
    }
}

@Composable
fun MoodEmojiButton(
    emoji: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Card(
            modifier = Modifier.size(60.dp),
            shape = CircleShape,
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ),
            onClick = onClick
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    emoji,
                    fontSize = 28.sp
                )
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        )
    }
}

@Composable
fun MoodDistributionBar(
    moodLevel: Int,
    percentage: Float
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            when (moodLevel) {
                1 -> "😔"
                2 -> "😐"
                3 -> "🙂"
                4 -> "😊"
                5 -> "🤩"
                else -> ""
            },
            modifier = Modifier.width(24.dp)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            LinearProgressIndicator(
                progress = { percentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = getMoodColor(moodLevel.toDouble()),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
fun WellnessStat(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
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

@Composable
fun WellnessCheckCard(check: WellnessCheck) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
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
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(getMoodColor(check.moodLevel.toDouble()).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    getMoodEmoji(check.moodLevel),
                    fontSize = 24.sp
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    getMoodLabel(check.moodLevel),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
                if (check.note.isNotEmpty()) {
                    Text(
                        check.note,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                }
                Text(
                    formatWellnessTime(check.timestamp.toDate()),
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
fun MoodSubmissionDialog(
    selectedMood: Int,
    onDismiss: () -> Unit,
    onSubmit: (Int, String) -> Unit
) {
    var note by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    isSubmitting = true
                    onSubmit(selectedMood, note)
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Submit")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Submit")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = {
            Text(
                "Submit Mood Check",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Mood display
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = getMoodColor(selectedMood.toDouble()).copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            getMoodEmoji(selectedMood),
                            fontSize = 40.sp
                        )
                        Column {
                            Text(
                                getMoodLabel(selectedMood),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = getMoodColor(selectedMood.toDouble())
                                )
                            )
                            Text(
                                "Mood Level: $selectedMood/5",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }
                }

                // Optional note
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Optional Note") },
                    placeholder = { Text("Add any additional comments about how you're feeling...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    maxLines = 3
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WellnessCalendarDialog(
    wellnessChecks: List<WellnessCheck>,
    onDismiss: () -> Unit
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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Wellness Calendar",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Calendar visualization placeholder
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    wellnessChecks.take(10).forEach { check ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                SimpleDateFormat("MMM dd", Locale.getDefault())
                                    .format(check.timestamp.toDate()),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(getMoodEmoji(check.moodLevel))
                                Text(
                                    "${check.moodLevel}/5",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = getMoodColor(check.moodLevel.toDouble())
                                    )
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
fun ResourceItem(
    title: String,
    description: String,
    icon: ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        onClick = { /* Navigate to resource */ }
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
                Icon(
                    icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium.copy(
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
                contentDescription = "View",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

// Helper functions
private fun getMoodEmoji(moodLevel: Int): String {
    return when (moodLevel) {
        1 -> "😔"
        2 -> "😐"
        3 -> "🙂"
        4 -> "😊"
        5 -> "🤩"
        else -> "😶"
    }
}

private fun getMoodLabel(moodLevel: Int): String {
    return when (moodLevel) {
        1 -> "Very Poor"
        2 -> "Poor"
        3 -> "Neutral"
        4 -> "Good"
        5 -> "Excellent"
        else -> "Unknown"
    }
}

private fun getMoodColor(moodLevel: Double): Color {
    return when (moodLevel.toInt()) {
        1 -> Color(0xFFF44336)
        2 -> Color(0xFFFF9800)
        3 -> Color(0xFFFFC107)
        4 -> Color(0xFF4CAF50)
        5 -> Color(0xFF2196F3)
        else -> Color.Gray
    }
}

private fun formatWellnessTime(date: Date): String {
    val now = Date()
    val diff = now.time - date.time
    val minutes = diff / (60 * 1000)
    val hours = diff / (60 * 60 * 1000)
    val days = diff / (24 * 60 * 60 * 1000)

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes minutes ago"
        hours < 24 -> "$hours hours ago"
        days < 7 -> "$days days ago"
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(date)
    }
}
