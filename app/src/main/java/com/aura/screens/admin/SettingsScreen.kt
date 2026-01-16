package com.aura.screens.admin

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.aura.components.LoadingAnimation
import com.aura.viewmodels.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    adminViewModel: AdminViewModel = hiltViewModel()
) {
    val settings by adminViewModel.settings.collectAsState()
    val isLoading by adminViewModel.isLoading.collectAsState()
    val context = LocalContext.current

    // Safety Configuration
    var emergencyRadius by remember { mutableStateOf(500f) }
    var autoResolveEnabled by remember { mutableStateOf(false) }
    var notificationPriority by remember { mutableStateOf(true) }
    
    // AI & Intelligence
    var aiSensitivity by remember { mutableStateOf(0.7f) }
    var voiceTriggerEnabled by remember { mutableStateOf(true) }
    var analyzeComplaintsWithAI by remember { mutableStateOf(true) }
    
    // Maintenance & Infrastructure
    var maintenanceAutoAssign by remember { mutableStateOf(false) }
    var minBudgetForApproval by remember { mutableStateOf(5000f) }
    
    // User & Data Privacy
    var allowAnonymousComplaints by remember { mutableStateOf(true) }
    var dataRetentionMonths by remember { mutableStateOf(6f) }
    var forceUserVerification by remember { mutableStateOf(false) }

    // Initialize local state when settings are loaded from Firestore
    LaunchedEffect(settings) {
        if (settings.isNotEmpty()) {
            emergencyRadius = (settings["emergencyRadius"] as? Number)?.toFloat() ?: 500f
            autoResolveEnabled = settings["autoResolveEnabled"] as? Boolean ?: false
            notificationPriority = settings["notificationPriority"] as? Boolean ?: true
            
            aiSensitivity = (settings["aiSensitivity"] as? Number)?.toFloat() ?: 0.7f
            voiceTriggerEnabled = settings["voiceTriggerEnabled"] as? Boolean ?: true
            analyzeComplaintsWithAI = settings["analyzeComplaintsWithAI"] as? Boolean ?: true
            
            maintenanceAutoAssign = settings["maintenanceAutoAssign"] as? Boolean ?: false
            minBudgetForApproval = (settings["minBudgetForApproval"] as? Number)?.toFloat() ?: 5000f
            
            allowAnonymousComplaints = settings["allowAnonymousComplaints"] as? Boolean ?: true
            dataRetentionMonths = (settings["dataRetentionMonths"] as? Number)?.toFloat() ?: 6f
            forceUserVerification = settings["forceUserVerification"] as? Boolean ?: false
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("System Control Panel", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading && settings.isEmpty()) {
            LoadingAnimation()
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Section: Safety & Emergency
                item { SectionHeader("Safety & Emergency", Icons.Default.Shield) }
                item {
                    SettingsCard {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            SettingLabel("Emergency Alert Radius", "${emergencyRadius.toInt()}m")
                            Slider(
                                value = emergencyRadius,
                                onValueChange = { emergencyRadius = it },
                                valueRange = 100f..5000f,
                                steps = 49
                            )
                            
                            ToggleSetting(
                                title = "Auto-resolve Incidents",
                                description = "Automatically mark alerts as resolved after 24h",
                                checked = autoResolveEnabled,
                                onCheckedChange = { autoResolveEnabled = it }
                            )
                            
                            ToggleSetting(
                                title = "High Priority Push",
                                description = "Override DND for critical campus alerts",
                                checked = notificationPriority,
                                onCheckedChange = { notificationPriority = it }
                            )
                        }
                    }
                }

                // Section: AI & Intelligence
                item { SectionHeader("AI & Intelligence", Icons.Default.Psychology) }
                item {
                    SettingsCard {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            SettingLabel("AI Detection Sensitivity", "${(aiSensitivity * 100).toInt()}%")
                            Slider(
                                value = aiSensitivity,
                                onValueChange = { aiSensitivity = it },
                                valueRange = 0.1f..1.0f
                            )
                            
                            ToggleSetting(
                                title = "Voice Trigger Recognition",
                                description = "Enable 'Help Help' background detection",
                                checked = voiceTriggerEnabled,
                                onCheckedChange = { voiceTriggerEnabled = it }
                            )
                            
                            ToggleSetting(
                                title = "Complaint AI Analysis",
                                description = "Automatically categorize and prioritize reports",
                                checked = analyzeComplaintsWithAI,
                                onCheckedChange = { analyzeComplaintsWithAI = it }
                            )
                        }
                    }
                }

                // Section: Infrastructure
                item { SectionHeader("Maintenance & Infrastructure", Icons.Default.Build) }
                item {
                    SettingsCard {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            ToggleSetting(
                                title = "Auto-Assign Maintenance",
                                description = "Automatically assign staff based on category",
                                checked = maintenanceAutoAssign,
                                onCheckedChange = { maintenanceAutoAssign = it }
                            )
                            
                            SettingLabel("Budget Approval Threshold", "₹${minBudgetForApproval.toInt()}")
                            Slider(
                                value = minBudgetForApproval,
                                onValueChange = { minBudgetForApproval = it },
                                valueRange = 1000f..50000f,
                                steps = 49
                            )
                        }
                    }
                }

                // Section: Privacy & Compliance
                item { SectionHeader("Privacy & Compliance", Icons.Default.Lock) }
                item {
                    SettingsCard {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            ToggleSetting(
                                title = "Anonymous Reporting",
                                description = "Allow students to submit complaints anonymously",
                                checked = allowAnonymousComplaints,
                                onCheckedChange = { allowAnonymousComplaints = it }
                            )
                            
                            ToggleSetting(
                                title = "Strict Identity Verification",
                                description = "Require OTP for all profile identity changes",
                                checked = forceUserVerification,
                                onCheckedChange = { forceUserVerification = it }
                            )
                            
                            SettingLabel("Data Retention Period", "${dataRetentionMonths.toInt()} Months")
                            Slider(
                                value = dataRetentionMonths,
                                onValueChange = { dataRetentionMonths = it },
                                valueRange = 1f..24f,
                                steps = 23
                            )
                        }
                    }
                }

                // Action Button
                item {
                    Button(
                        onClick = {
                            val newSettings = mapOf(
                                "emergencyRadius" to emergencyRadius.toLong(),
                                "autoResolveEnabled" to autoResolveEnabled,
                                "notificationPriority" to notificationPriority,
                                "aiSensitivity" to aiSensitivity.toDouble(),
                                "voiceTriggerEnabled" to voiceTriggerEnabled,
                                "analyzeComplaintsWithAI" to analyzeComplaintsWithAI,
                                "maintenanceAutoAssign" to maintenanceAutoAssign,
                                "minBudgetForApproval" to minBudgetForApproval.toDouble(),
                                "allowAnonymousComplaints" to allowAnonymousComplaints,
                                "forceUserVerification" to forceUserVerification,
                                "dataRetentionMonths" to dataRetentionMonths.toLong()
                            )
                            adminViewModel.saveSettings(newSettings)
                            Toast.makeText(context, "Global Configuration Updated", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Default.Save, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Apply Changes System-wide", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun SettingLabel(title: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ToggleSetting(title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Box(modifier = Modifier.padding(20.dp)) {
            content()
        }
    }
}
