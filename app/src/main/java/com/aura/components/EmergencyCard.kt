package com.aura.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.sp
import com.aura.models.Emergency
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyCard(
    emergency: Emergency,
    onClick: () -> Unit = {},
    showActions: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (emergency.priority) {
                "critical" -> Color(0xFF6E5F5F)
                "high" -> Color(0xFF6E5F5F)
                "medium" -> Color(0xFF6E5F5F)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with status and time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = when (emergency.type) {
                            "voice" -> Icons.Default.Mic
                            "manual" -> Icons.Default.Emergency
                            "infrastructure" -> Icons.Default.Warning
                            else -> Icons.Default.Error
                        },
                        contentDescription = "Emergency Type",
                        tint = when (emergency.type) {
                            "voice" -> MaterialTheme.colorScheme.error
                            "manual" -> Color.Red
                            "infrastructure" -> Color(0xFFFF9800)
                            else -> MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(20.dp)
                    )

                    Text(
                        text = emergency.type.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (emergency.status) {
                        "active" -> MaterialTheme.colorScheme.errorContainer
                        "resolved" -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = emergency.status.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = when (emergency.status) {
                                "active" -> MaterialTheme.colorScheme.error
                                "resolved" -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Emergency description
            Text(
                text = emergency.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            // User info and time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Reported by",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    )
                    Text(
                        text = emergency.userName,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                Text(
                    text = emergency.timestamp?.let { formatEmergencyTime(it.toDate()) } ?: "Just now",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )
            }

            // Danger level indicator
            if (emergency.dangerLevel > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Danger Level:",
                        style = MaterialTheme.typography.labelSmall
                    )

                    DangerLevelIndicator(level = emergency.dangerLevel)
                }
            }

            // Actions (expandable)
            AnimatedVisibility(
                visible = showActions,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        EmergencyActionButton(
                            icon = Icons.Default.LocationOn,
                            text = "Location",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (emergency.location != null) {
                                    val geoPoint = emergency.location
                                    val uri = "geo:${geoPoint.latitude},${geoPoint.longitude}?q=${geoPoint.latitude},${geoPoint.longitude}(Emergency Location)"
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                                    intent.setPackage("com.google.android.apps.maps")
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        // Fallback if Google Maps is not installed
                                        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${geoPoint.latitude},${geoPoint.longitude}"))
                                        context.startActivity(webIntent)
                                    }
                                } else {
                                    Toast.makeText(context, "Location coordinates not available", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )

                        if (emergency.audioClipUrl != null) {
                            EmergencyActionButton(
                                icon = Icons.Default.PlayArrow,
                                text = "Audio",
                                modifier = Modifier.weight(1f),
                                onClick = { /* Play audio clip */ }
                            )
                        }

                        if (emergency.imageUrl != null) {
                            EmergencyActionButton(
                                icon = Icons.Default.Photo,
                                text = "Image",
                                modifier = Modifier.weight(1f),
                                onClick = { /* View image */ }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DangerLevelIndicator(level: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(5) { index ->
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (index < level) {
                            when (level) {
                                1, 2 -> Color(0xFF4CAF50)
                                3 -> Color(0xFFFFC107)
                                4, 5 -> Color(0xFFF44336)
                                else -> Color.Gray
                            }
                        } else {
                            Color.Gray.copy(alpha = 0.2f)
                        }
                    )
            )
        }
    }
}

@Composable
fun EmergencyActionButton(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyCompactCard(
    emergency: Emergency,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (emergency.status == "active") MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (emergency.description.length > 40) emergency.description.take(40) + "..." else emergency.description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1
                )
                Text(
                    text = emergency.timestamp?.let { formatEmergencyTimeShort(it.toDate()) } ?: "Just now",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )
            }

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        when (emergency.priority) {
                            "critical" -> Color.Red
                            "high" -> Color(0xFFFF9800)
                            "medium" -> Color(0xFF4CAF50)
                            else -> Color.Gray
                        }
                    )
            )
        }
    }
}

@Composable
fun EmergencyStatusChip(status: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = when (status.lowercase()) {
            "active" -> MaterialTheme.colorScheme.errorContainer
            "resolved" -> MaterialTheme.colorScheme.primaryContainer
            "cancelled" -> MaterialTheme.colorScheme.surfaceVariant
            else -> MaterialTheme.colorScheme.secondaryContainer
        }
    ) {
        Text(
            text = status.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = when (status.lowercase()) {
                    "active" -> MaterialTheme.colorScheme.error
                    "resolved" -> MaterialTheme.colorScheme.primary
                    "cancelled" -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.secondary
                }
            ),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

private fun formatEmergencyTime(date: Date): String {
    val now = Date()
    val diff = now.time - date.time
    val minutes = diff / (60 * 1000)
    val hours = diff / (60 * 60 * 1000)
    val days = diff / (24 * 60 * 60 * 1000)

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes min ago"
        hours < 24 -> "$hours hour${if (hours > 1) "s" else ""} ago"
        days < 7 -> "$days day${if (days > 1) "s" else ""} ago"
        else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
    }
}

private fun formatEmergencyTimeShort(date: Date): String {
    val now = Date()
    val diff = now.time - date.time
    val minutes = diff / (60 * 1000)
    val hours = diff / (60 * 60 * 1000)

    return when {
        minutes < 60 -> "${minutes}m"
        hours < 24 -> "${hours}h"
        else -> SimpleDateFormat("MM/dd", Locale.getDefault()).format(date)
    }
}
