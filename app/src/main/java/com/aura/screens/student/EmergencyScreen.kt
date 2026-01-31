package com.aura.screens.student

import android.Manifest
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.aura.utils.PermissionsManager
import com.aura.viewmodels.AuthViewModel
import com.aura.viewmodels.EmergencyViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class, ExperimentalPermissionsApi::class)
@Composable
fun EmergencyScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    permissionsManager: PermissionsManager,
    emergencyViewModel: EmergencyViewModel = hiltViewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isEmergencyActive by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(5) }
    var timerJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var selectedEmergencyType by remember { mutableStateOf("General") }
    
    // Safety Tools
    var isSirenOn by remember { mutableStateOf(false) }
    var isFlashlightOn by remember { mutableStateOf(false) }
    var isFakeCallActive by remember { mutableStateOf(false) }

    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val audioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    // Flashlight 05
    val cameraManager = remember { context.getSystemService(Context.CAMERA_SERVICE) as CameraManager }
    val cameraId = remember { 
        try { 
            cameraManager.cameraIdList.firstOrNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                characteristics.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: Exception) { null } 
    }

    // Siren Logic
    val mediaPlayer = remember { 
        MediaPlayer.create(context, android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI).apply {
            isLooping = true
        }
    }

    LaunchedEffect(isFlashlightOn) {
        if (cameraId != null) {
            try {
                cameraManager.setTorchMode(cameraId, isFlashlightOn)
            } catch (e: Exception) {
                Log.e("EmergencyScreen", "Flashlight Error: ${e.message}")
                isFlashlightOn = false
            }
        }
    }

    LaunchedEffect(isSirenOn) {
        try {
            if (isSirenOn) {
                mediaPlayer?.start()
            } else {
                if (mediaPlayer?.isPlaying == true) {
                    mediaPlayer.pause()
                    mediaPlayer.seekTo(0)
                }
            }
        } catch (e: Exception) {
            Log.e("EmergencyScreen", "Siren Error: ${e.message}")
        }
    }

    // Turn off tools on dispose
    DisposableEffect(Unit) {
        onDispose {
            if (isFlashlightOn && cameraId != null) {
                try {
                    cameraManager.setTorchMode(cameraId, false)
                } catch (e: Exception) {
                    Log.e("EmergencyScreen", "Cleanup Flashlight Error: ${e.message}")
                }
            }
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
            } catch (e: Exception) {
                Log.e("EmergencyScreen", "Cleanup Siren Error: ${e.message}")
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!locationPermissionState.status.isGranted) locationPermissionState.launchPermissionRequest()
        if (!audioPermissionState.status.isGranted) audioPermissionState.launchPermissionRequest()
    }

    val emergencyTypes = listOf(
        "General" to Icons.Default.Warning,
        "Medical" to Icons.Default.MedicalServices,
        "Security" to Icons.Default.Policy,
        "Fire" to Icons.Default.FireTruck,
        "Harassment" to Icons.Default.PanTool
    )

    if (isFakeCallActive) {
        FakeCallOverlay(
            onEndCall = { isFakeCallActive = false }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("AURA SOS", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isEmergencyActive) "TRIGGERING ALERT..." else "SYSTEM READY",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isEmergencyActive) MaterialTheme.colorScheme.error else Color.Gray,
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isEmergencyActive) {
                        CircularProgressIndicator(
                            progress = { (5 - countdown) / 5f },
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 12.dp,
                            color = MaterialTheme.colorScheme.error,
                            trackColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .shadow(20.dp, CircleShape)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(MaterialTheme.colorScheme.error, Color(0xFF991B1B))
                                    )
                                )
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .size(180.dp)
                            .clip(CircleShape)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        if (locationPermissionState.status.isGranted) {
                                            isEmergencyActive = true
                                            countdown = 5
                                            timerJob = scope.launch {
                                                while (countdown > 0) {
                                                    delay(1000)
                                                    countdown--
                                                }
                                                emergencyViewModel.createEmergency(
                                                    type = selectedEmergencyType.lowercase(),
                                                    description = "$selectedEmergencyType SOS triggered by user"
                                                )
                                                isEmergencyActive = false
                                                Toast.makeText(context, "SOS Alert Sent!", Toast.LENGTH_LONG).show()
                                            }
                                            try {
                                                awaitRelease()
                                            } finally {
                                                timerJob?.cancel()
                                                isEmergencyActive = false
                                                countdown = 5
                                            }
                                        } else {
                                            locationPermissionState.launchPermissionRequest()
                                        }
                                    }
                                )
                            },
                        color = Color.Transparent,
                        contentColor = Color.White
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isEmergencyActive) Icons.Default.Timer else Icons.Default.Emergency,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (isEmergencyActive) "$countdown" else "HOLD 5s",
                                fontSize = if (isEmergencyActive) 48.sp else 20.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Incident Type",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 24.dp),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(emergencyTypes) { (type, icon) ->
                            FilterChip(
                                selected = selectedEmergencyType == type,
                                onClick = { selectedEmergencyType = type },
                                label = { Text(type) },
                                leadingIcon = { Icon(icon, null, modifier = Modifier.size(18.dp)) },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.error
                                )
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SafetyToolItem(
                            icon = if (isSirenOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            label = "Siren",
                            active = isSirenOn,
                            onClick = { isSirenOn = !isSirenOn }
                        )
                        SafetyToolItem(
                            icon = Icons.Default.FlashlightOn,
                            label = "Flashlight",
                            active = isFlashlightOn,
                            onClick = { isFlashlightOn = !isFlashlightOn }
                        )
                        SafetyToolItem(
                            icon = Icons.Default.MyLocation,
                            label = "Track Me",
                            active = true,
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=my+location"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Maps app not found", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                        SafetyToolItem(
                            icon = Icons.Default.PhoneInTalk,
                            label = "Fake Call",
                            active = isFakeCallActive,
                            onClick = { isFakeCallActive = true }
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Groups, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Text("Your Trusted Circle", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${currentUser?.trustedFriends?.size ?: 0} friends and campus security will be notified immediately.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        TextButton(
                            onClick = { navController.navigate("profile") },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Manage Contacts", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:100"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Call, null)
                    Spacer(Modifier.width(12.dp))
                    Text("Call Campus Security")
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun FakeCallOverlay(onEndCall: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(48.dp))
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color.Gray),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, null, modifier = Modifier.size(64.dp), tint = Color.White)
                }
                Spacer(Modifier.height(16.dp))
                Text("Incoming Call...", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
                Text("AURA Emergency Deterrent", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.7f))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FakeCallAction(Icons.Default.MicOff, "Mute")
                FakeCallAction(Icons.Default.Dialpad, "Keypad")
                FakeCallAction(Icons.AutoMirrored.Filled.VolumeUp, "Speaker")
            }

            IconButton(
                onClick = onEndCall,
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.Red)
            ) {
                Icon(Icons.Default.CallEnd, null, modifier = Modifier.size(40.dp), tint = Color.White)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun FakeCallAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color.White)
        }
        Spacer(Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White)
    }
}

@Composable
fun SafetyToolItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(if (active) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Icon(
                icon,
                null,
                tint = if (active) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
