package com.aura.components

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.models.Project
import com.aura.models.ProjectApplication
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProjectCard(
    project: Project,
    onClick: () -> Unit,
    showApplications: Boolean = false,
    applications: List<ProjectApplication> = emptyList(),
    onAcceptApplication: (String) -> Unit = {},
    onRejectApplication: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 6.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with title and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = project.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )

                ProjectStatusChip(status = project.status)
            }

            // Project description
            Text(
                text = project.description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 20.sp
                ),
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            // Skills and requirements
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                project.requiredSkills.forEach { skill ->
                    SkillChip(skill = skill)
                }

                TeamSizeChip(
                    current = project.teamMembers.size,
                    max = project.maxTeamSize
                )
            }

            // Footer with creator and deadline
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Creator avatar
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = project.creatorName.firstOrNull()?.toString() ?: "?",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    Column {
                        Text(
                            text = "Created by",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        )
                        Text(
                            text = project.creatorName,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                if (project.deadline != null) {
                    DeadlineIndicator(deadline = project.deadline.toDate())
                }
            }

            // Applications section (expandable)
            AnimatedVisibility(
                visible = showApplications && applications.isNotEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )

                    Text(
                        text = "Applications (${applications.size})",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )

                    applications.forEach { application ->
                        ApplicationItem(
                            application = application,
                            onAccept = { onAcceptApplication(application.id) },
                            onReject = { onRejectApplication(application.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProjectStatusChip(status: String) {
    SuggestionChip(
        onClick = {},
        label = {
            Text(
                text = status.replace("_", " ").replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    )
}

@Composable
fun SkillChip(skill: String) {
    AssistChip(
        onClick = {},
        label = {
            Text(
                text = skill,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium
                )
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
            labelColor = MaterialTheme.colorScheme.secondary
        )
    )
}

@Composable
fun TeamSizeChip(current: Int, max: Int) {
    AssistChip(
        onClick = {},
        label = {
            Text(
                text = "$current/$max members",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium
                )
            )
        },
        leadingIcon = {
            Icon(
                Icons.Default.Group,
                contentDescription = "Team size",
                modifier = Modifier.size(16.dp)
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            labelColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
fun DeadlineIndicator(deadline: Date) {
    val now = Date()
    val diff = deadline.time - now.time
    val daysLeft = diff / (24 * 60 * 60 * 1000)

    val isUrgent = daysLeft <= 3
    val isOverdue = diff < 0

    Column(
        horizontalAlignment = Alignment.End
    ) {
        Text(
            text = "Deadline",
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        )
        Text(
            text = if (isOverdue) {
                "Overdue!"
            } else if (isUrgent) {
                "$daysLeft day${if (daysLeft.toInt() != 1) "s" else ""} left"
            } else {
                SimpleDateFormat("MMM dd", Locale.getDefault()).format(deadline)
            },
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = when {
                    isOverdue -> MaterialTheme.colorScheme.error
                    isUrgent -> Color(0xFFFF9800)
                    else -> MaterialTheme.colorScheme.primary
                }
            )
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ApplicationItem(
    application: ProjectApplication,
    onAccept: () -> Unit,
    onReject: () -> Unit
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
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = application.applicantName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Text(
                    text = application.message.take(60) + if (application.message.length > 60) "..." else "",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    application.skills.take(3).forEach { skill ->
                        Text(
                            text = skill,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    if (application.skills.size > 3) {
                        Text(
                            text = "+${application.skills.size - 3}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onAccept,
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color(0xFFE8F5E9)
                    )
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Accept",
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onReject,
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color(0xFFFFEBEE)
                    )
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Reject",
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ProjectCompactCard(
    project: Project,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick
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
                Text(
                    text = project.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 1
                )
                Text(
                    text = "${project.teamMembers.size}/${project.maxTeamSize} members",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )
            }

            ProjectStatusIndicator(status = project.status)
        }
    }
}

@Composable
fun ProjectStatusIndicator(status: String) {
    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(
                when (status.lowercase()) {
                    "open" -> MaterialTheme.colorScheme.primary
                    "in_progress" -> MaterialTheme.colorScheme.secondary
                    "completed" -> Color(0xFF4CAF50)
                    "cancelled" -> Color.Gray
                    else -> MaterialTheme.colorScheme.tertiary
                }
            )
    )
}
