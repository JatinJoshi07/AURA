package com.aura.screens.student

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.aura.utils.PermissionsManager
import com.aura.utils.VoiceRecognitionService
import com.aura.viewmodels.AuthViewModel
import com.aura.viewmodels.EmergencyViewModel
import com.aura.viewmodels.PinkShieldViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun PinkShieldScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    @Suppress("UNUSED_PARAMETER") permissionsManager: PermissionsManager,
    emergencyViewModel: EmergencyViewModel = hiltViewModel(),
    pinkShieldViewModel: PinkShieldViewModel = hiltViewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val context = LocalContext.current
    
    // UI States from the singleton Voice Recognition engine
    val isListening by pinkShieldViewModel.isListening.collectAsState()
    val recognizedText by pinkShieldViewModel.recognizedText.collectAsState()
    val panicDetected by pinkShieldViewModel.panicDetected.collectAsState()

    // Persistent settings
    val prefs = remember { context.getSharedPreferences("pink_shield_prefs", Context.MODE_PRIVATE) }
    var panicKeyword by remember { mutableStateOf(prefs.getString("panic_keyword", currentUser?.panicKeyword ?: "HELP") ?: "HELP") }
    var isVoiceActivated by remember { mutableStateOf(prefs.getBoolean("is_voice_activated", false)) }

    val audioPermissionState = rememberPermissionState(
        Manifest.permission.RECORD_AUDIO
    )

    // Handle SOS trigger when keyword is detected
    LaunchedEffect(panicDetected) {
        if (panicDetected) {
            emergencyViewModel.createEmergency("voice", "Voice SOS triggered with keyword: $panicKeyword")
            Toast.makeText(context, "🚨 PANIC DETECTED! HELP IS ON THE WAY.", Toast.LENGTH_LONG).show()
            pinkShieldViewModel.resetPanic()
        }
    }

    // Function to start/stop the background service
    val toggleVoiceGuard = { enabled: Boolean ->
        if (enabled) {
            if (audioPermissionState.status.isGranted) {
                val intent = Intent(context, VoiceRecognitionService::class.java).apply {
                    action = VoiceRecognitionService.ACTION_START_GUARD
                    putExtra(VoiceRecognitionService.EXTRA_PANIC_WORD, panicKeyword)
                }
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                
                isVoiceActivated = true
                prefs.edit { putBoolean("is_voice_activated", true) }
            } else {
                audioPermissionState.launchPermissionRequest()
            }
        } else {
            val intent = Intent(context, VoiceRecognitionService::class.java).apply {
                action = VoiceRecognitionService.ACTION_STOP_GUARD
            }
            context.startService(intent)
            isVoiceActivated = false
            prefs.edit { putBoolean("is_voice_activated", false) }
        }
    }

    // Update settings when changed
    LaunchedEffect(panicKeyword) {
        prefs.edit { putString("panic_keyword", panicKeyword) }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Pink Shield",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
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
                        "Always active protection. Voice Guard monitors for your panic word even when the app is closed.",
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
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = "Voice",
                                tint = if (isVoiceActivated) Color(0xFFFF4081) else Color.Gray
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Voice Guard Status",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                        
                        Switch(
                            checked = isVoiceActivated,
                            onCheckedChange = { toggleVoiceGuard(it) }
                        )
                    }

                    OutlinedTextField(
                        value = panicKeyword,
                        onValueChange = { panicKeyword = it },
                        label = { Text("Panic Keyword") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Key, contentDescription = "Keyword")
                        },
                        placeholder = { Text("e.g., HELP, RED ALERT") },
                        enabled = !isVoiceActivated // Lock while active
                    )

                    // Live Status Box
                    if (isVoiceActivated) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isListening) Icons.Default.GraphicEq else Icons.Default.MicOff,
                                        contentDescription = null,
                                        tint = if (isListening) Color(0xFFFF4081) else Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = if (isListening) "Listening for \"$panicKeyword\"" else "Standby...",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                if (recognizedText.isNotEmpty() && isListening) {
                                    Text(
                                        text = "Recognized: \"$recognizedText\"",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        "Voice Guard service runs in the background. Triggering the SOS will notify security and your trusted contacts with your live location.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    )
                }
            }

            // AI Guardian
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp)
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
                            tint = Color(0xFF9C27B0)
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
                        onClick = { 
                            Toast.makeText(context, "Feature coming soon!", Toast.LENGTH_SHORT).show()
                        },
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
                shape = RoundedCornerShape(24.dp)
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
                            tint = Color(0xFF4CAF50)
                        )
                        Text(
                            "Safe Route Scouter",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    Text(
                        "Get real-time safe route suggestions based on campus lighting and activity.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Button(
                        onClick = { 
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=well+lit+paths+near+me"))
                            context.startActivity(intent)
                        },
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
                shape = RoundedCornerShape(24.dp)
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
                            "Emergency Circle",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    currentUser?.emergencyContacts?.forEach { contact ->
                        ListItem(
                            headlineContent = { Text(contact) },
                            leadingContent = { Icon(Icons.Default.Phone, null, modifier = Modifier.size(18.dp)) }
                        )
                    }

                    OutlinedButton(
                        onClick = { navController.navigate("profile") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Manage")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Manage Circle")
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
