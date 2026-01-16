package com.aura.screens.student

import android.Manifest
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.aura.utils.PermissionsManager
import com.aura.utils.VoiceRecognition
import com.aura.viewmodels.AuthViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class, ExperimentalPermissionsApi::class)
@Composable
fun PinkShieldScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    permissionsManager: PermissionsManager
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val context = LocalContext.current
    var panicKeyword by remember { mutableStateOf(currentUser?.panicKeyword ?: "HELP") }
    var isVoiceActivated by remember { mutableStateOf(false) }
    var isListening by remember { mutableStateOf(false) }

    val audioPermissionState = rememberPermissionState(
        Manifest.permission.RECORD_AUDIO
    )

    val voiceRecognition = remember { VoiceRecognition(context) }

    LaunchedEffect(Unit) {
        if (!audioPermissionState.status.isGranted) {
            audioPermissionState.launchPermissionRequest()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Pink Shield",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFFFE4E6)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Hero Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFE4E6)
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = "Shield",
                        modifier = Modifier.size(64.dp),
                        tint = Color(0xFFFF4081)
                    )

                    Text(
                        "Women's Safety Shield",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF4081)
                        )
                    )

                    Text(
                        "Your safety is our priority. Activate voice-activated SOS and access safety features designed for your protection.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Voice-Activated SOS
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "Voice",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Voice-Activated SOS",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    Switch(
                        checked = isVoiceActivated,
                        onCheckedChange = { isVoiceActivated = it },
                        thumbContent = {
                            Icon(
                                if (isVoiceActivated) Icons.Default.Check else Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize)
                            )
                        }
                    )

                    OutlinedTextField(
                        value = panicKeyword,
                        onValueChange = { panicKeyword = it },
                        label = { Text("Panic Keyword") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Key, contentDescription = "Keyword")
                        },
                        placeholder = { Text("e.g., HELP, RED ALERT") }
                    )

                    Text(
                        "When activated, saying \"$panicKeyword\" will trigger silent emergency alert with your location.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    )

                    Button(
                        onClick = {
                            if (audioPermissionState.status.isGranted) {
                                isListening = true
                                voiceRecognition.startListening(panicKeyword)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isVoiceActivated && !isListening
                    ) {
                        if (isListening) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Listening...")
                        } else {
                            Icon(Icons.Default.Mic, contentDescription = "Test")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Test Voice Recognition")
                        }
                    }
                }
            }

            // AI Guardian
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.SmartToy,
                            contentDescription = "AI",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "AI Guardian",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    Text(
                        "Feeling unsafe? Start a simulated phone call with our AI to deter potential harassers.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedButton(
                        onClick = { /* Start AI conversation */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Simulate Call")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Simulated Call")
                    }
                }
            }

            // Safe Route Planner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Map,
                            contentDescription = "Route",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Safe Route Scouter",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    Text(
                        "Get real-time safe route suggestions with good lighting and high foot traffic.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Button(
                        onClick = { /* Open map */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Directions, contentDescription = "Directions")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Plan Safe Route")
                    }
                }
            }

            // Emergency Contacts
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Contacts,
                            contentDescription = "Contacts",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Emergency Contacts",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    // Contact list would go here
                    OutlinedButton(
                        onClick = { /* Manage contacts */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Contact")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Emergency Contact")
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
