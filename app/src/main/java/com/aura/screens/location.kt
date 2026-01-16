package com.aura.screens

import android.Manifest
import android.content.IntentFilter
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.aura.utils.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun LocationTrackingScreen() {
    val context = LocalContext.current
    val locationManager = remember { LocationServiceManager.getInstance(context) }
    val locationState = remember { LocationState() }

    // Location permissions
    val locationPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )
    } else {
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    val permissionState = rememberMultiplePermissionsState(permissions = locationPermissions)

    // Broadcast receiver for location updates
    val broadcastReceiver = remember {
        LocationBroadcastReceiver(
            onLocationUpdate = { location ->
                // Handle location update
                println("Location updated: ${location.latitude}, ${location.longitude}")
            },
            onLocationError = { error ->
                // Handle location error
                println("Location error: $error")
            }
        )
    }

    // Register/unregister broadcast receiver
    DisposableEffect(Unit) {
        val filter = IntentFilter().apply {
            addAction(LocationService.ACTION_LOCATION_UPDATE)
            addAction(LocationService.ACTION_LOCATION_ERROR)
        }

        LocalBroadcastManager.getInstance(context)
            .registerReceiver(broadcastReceiver, filter)

        onDispose {
            LocalBroadcastManager.getInstance(context)
                .unregisterReceiver(broadcastReceiver)
        }
    }

    // Check permissions on launch
    LaunchedEffect(permissionState.allPermissionsGranted) {
        if (permissionState.allPermissionsGranted) {
            if (!locationManager.isServiceRunning()) {
                locationManager.startLocationService()
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Location Tracking",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Permission status
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (permissionState.allPermissionsGranted) {
                        Color(0xFFE8F5E9)
                    } else {
                        Color(0xFFFFF3CD)
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "Location Permission",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            if (permissionState.allPermissionsGranted)
                                "Granted ✓"
                            else
                                "Required for safety features",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (!permissionState.allPermissionsGranted) {
                        Button(
                            onClick = { permissionState.launchMultiplePermissionRequest() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            )
                        ) {
                            Text("Request")
                        }
                    }
                }
            }

            // Service status
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            if (locationState.isServiceRunning.value)
                                Icons.Default.LocationOn
                            else
                                Icons.Default.LocationOff,
                            contentDescription = "Service Status",
                            tint = if (locationState.isServiceRunning.value)
                                Color(0xFF4CAF50)
                            else
                                Color(0xFFF44336)
                        )
                        Text(
                            "Location Service",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            if (locationState.isServiceRunning.value) "ACTIVE" else "INACTIVE",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = if (locationState.isServiceRunning.value)
                                    Color(0xFF4CAF50)
                                else
                                    Color(0xFFF44336),
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    // Service controls
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                if (permissionState.allPermissionsGranted) {
                                    locationManager.startLocationService()
                                }
                            },
                            enabled = !locationState.isServiceRunning.value &&
                                    permissionState.allPermissionsGranted,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Start")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start Tracking")
                        }

                        Button(
                            onClick = { locationManager.stopLocationService() },
                            enabled = locationState.isServiceRunning.value,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Stop")
                        }
                    }
                }
            }

            // Location info
            if (locationState.isServiceRunning.value) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "Current Location",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )

                        locationState.currentLocation.value?.let { location ->
                            // Map view
                            val cameraPositionState = rememberCameraPositionState {
                                position = CameraPosition.fromLatLngZoom(
                                    LatLng(location.latitude, location.longitude),
                                    15f
                                )
                            }

                            // Update camera when location changes
                            LaunchedEffect(location) {
                                cameraPositionState.position = CameraPosition.fromLatLngZoom(
                                    LatLng(location.latitude, location.longitude),
                                    15f
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                GoogleMap(
                                    modifier = Modifier.fillMaxSize(),
                                    cameraPositionState = cameraPositionState
                                ) {
                                    Marker(
                                        state = MarkerState(
                                            position = LatLng(
                                                location.latitude,
                                                location.longitude
                                            )
                                        ),
                                        title = "Your Location"
                                    )
                                }
                            }

                            // Location details
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                LocationDetailItem(
                                    label = "Coordinates",
                                    value = locationState.getFormattedCoordinates(),
                                    icon = Icons.Default.Place
                                )

                                LocationDetailItem(
                                    label = "Accuracy",
                                    value = locationState.getFormattedAccuracy(),
                                    icon = Icons.Default.MyLocation
                                )

                                LocationDetailItem(
                                    label = "Last Updated",
                                    value = locationState.lastUpdateTime.value,
                                    icon = Icons.Default.Schedule
                                )

                                LocationDetailItem(
                                    label = "Updates",
                                    value = locationState.locationUpdatesCount.value.toString(),
                                    icon = Icons.Default.Refresh
                                )
                            }
                        } ?: run {
                            // No location yet
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    Icons.Default.LocationSearching,
                                    contentDescription = "Searching",
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                )
                                Text(
                                    "Acquiring location...",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Emergency mode toggle
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (locationState.emergencyMode.value)
                        Color(0xFFFFE5E5)
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Report,
                            contentDescription = "Emergency",
                            tint = if (locationState.emergencyMode.value)
                                Color.Red
                            else
                                MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Emergency Tracking",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Text(
                                if (locationState.emergencyMode.value)
                                    "High-frequency location tracking active"
                                else
                                    "Activate for emergency situations",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Switch(
                            checked = locationState.emergencyMode.value,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    locationManager.startEmergencyTracking()
                                } else {
                                    locationManager.stopEmergencyTracking()
                                }
                            },
                            enabled = locationState.isServiceRunning.value
                        )
                    }

                    if (locationState.emergencyMode.value) {
                        Text(
                            "⚠️ Emergency mode: Location updates every 2 seconds for accurate tracking",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color.Red,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun LocationDetailItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}