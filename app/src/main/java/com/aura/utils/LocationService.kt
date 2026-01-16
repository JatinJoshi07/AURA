package com.aura.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.aura.MainActivity
import com.aura.R
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class LocationService : Service() {

    companion object {
        private const val TAG = "LocationService"
        private const val NOTIFICATION_CHANNEL_ID = "aura_location_channel"
        private const val NOTIFICATION_ID = 1001
        private const val LOCATION_UPDATE_INTERVAL = 5000L // 5 seconds
        private const val FASTEST_LOCATION_UPDATE_INTERVAL = 3000L // 3 seconds
        private const val EMERGENCY_UPDATE_INTERVAL = 2000L // 2 seconds
        private const val LOCATION_REQUEST_MIN_DISTANCE = 10f // 10 meters
        private const val MAX_LOCATION_AGE = 30000L // 30 seconds

        // Actions
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_UPDATE_INTERVAL = "ACTION_UPDATE_INTERVAL"
        const val ACTION_GET_CURRENT_LOCATION = "ACTION_GET_CURRENT_LOCATION"
        const val ACTION_START_EMERGENCY = "ACTION_START_EMERGENCY"
        const val ACTION_STOP_EMERGENCY = "ACTION_STOP_EMERGENCY"

        // Broadcast actions
        const val ACTION_LOCATION_UPDATE = "com.aura.LOCATION_UPDATE"
        const val ACTION_LOCATION_ERROR = "com.aura.LOCATION_ERROR"

        // Extras
        const val EXTRA_LOCATION = "extra_location"
        const val EXTRA_ERROR_MESSAGE = "extra_error_message"
        const val EXTRA_UPDATE_INTERVAL = "extra_update_interval"

        // Shared state (for UI updates)
        val currentLocation: MutableState<Location?> = mutableStateOf(null)
        val isServiceRunning: MutableState<Boolean> = mutableStateOf(false)
        val locationAccuracy: MutableState<Float> = mutableStateOf(0f)
        val locationUpdatesCount: MutableState<Int> = mutableStateOf(0)
        val lastUpdateTime: MutableState<String> = mutableStateOf("Never")

        // For emergency location sharing
        val emergencyMode: MutableState<Boolean> = mutableStateOf(false)
        val emergencyLocationUpdates: MutableList<Location> = mutableListOf()
    }

    private lateinit var notificationManager: NotificationManager
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private var locationRequest: LocationRequest? = null
    private var cancellationTokenSource: CancellationTokenSource? = null
    private var serviceJob: Job? = null
    private var currentLatitude = 0.0
    private var currentLongitude = 0.0
    private var isFirstLocation = true

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "LocationService onCreate")

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        initializeLocationRequest()
        isServiceRunning.value = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ${intent?.action}")

        when (intent?.action) {
            ACTION_START -> {
                startForegroundService()
                startLocationUpdates()
            }

            ACTION_STOP -> {
                stopLocationUpdates()
                stopSelf()
            }

            ACTION_UPDATE_INTERVAL -> {
                val interval = intent.getLongExtra(EXTRA_UPDATE_INTERVAL, LOCATION_UPDATE_INTERVAL)
                updateLocationInterval(interval)
            }

            ACTION_GET_CURRENT_LOCATION -> {
                getCurrentLocation()
            }

            ACTION_START_EMERGENCY -> {
                emergencyMode.value = true
                emergencyLocationUpdates.clear()
                updateLocationInterval(EMERGENCY_UPDATE_INTERVAL)
            }

            ACTION_STOP_EMERGENCY -> {
                emergencyMode.value = false
                updateLocationInterval(LOCATION_UPDATE_INTERVAL)
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(TAG, "LocationService onDestroy")
        stopLocationUpdates()
        isServiceRunning.value = false
        emergencyMode.value = false
        emergencyLocationUpdates.clear()
        super.onDestroy()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "AURA Location Tracking",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Tracks your location for safety features"
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        notificationManager.createNotificationChannel(channel)
    }

    @SuppressLint("MissingPermission")
    private fun initializeLocationRequest() {
        val interval = if (emergencyMode.value) EMERGENCY_UPDATE_INTERVAL else LOCATION_UPDATE_INTERVAL
        
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, interval)
            .setMinUpdateIntervalMillis(FASTEST_LOCATION_UPDATE_INTERVAL)
            .setMinUpdateDistanceMeters(LOCATION_REQUEST_MIN_DISTANCE)
            .setMaxUpdateDelayMillis(interval * 2)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                locationResult.lastLocation?.let { location ->
                    handleNewLocation(location)
                }
            }

            override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                super.onLocationAvailability(locationAvailability)
                if (!locationAvailability.isLocationAvailable) {
                    broadcastLocationError("Location services unavailable")
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (fusedLocationClient == null || locationCallback == null || locationRequest == null) {
            Log.e(TAG, "Location components not initialized")
            return
        }

        if (!hasLocationPermission()) {
            Log.e(TAG, "Location permission not granted")
            broadcastLocationError("Location permission required")
            return
        }

        try {
            fusedLocationClient!!.requestLocationUpdates(
                locationRequest!!,
                locationCallback!!,
                Looper.getMainLooper()
            )

            Log.d(TAG, "Location updates started")
            updateNotification("Tracking location...")

            // Start periodic location quality checks
            startLocationQualityChecks()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start location updates: ${e.message}")
            broadcastLocationError("Failed to start location tracking")
        }
    }

    private fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient?.removeLocationUpdates(it)
            Log.d(TAG, "Location updates stopped")
        }

        cancellationTokenSource?.cancel()
        serviceJob?.cancel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        if (!hasLocationPermission()) {
            broadcastLocationError("Location permission required")
            return
        }

        cancellationTokenSource = CancellationTokenSource()

        fusedLocationClient?.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource?.token
        )?.addOnSuccessListener { location ->
            location?.let {
                handleNewLocation(it)
            } ?: run {
                broadcastLocationError("Unable to get current location")
            }
        }?.addOnFailureListener { exception ->
            Log.e(TAG, "Failed to get current location: ${exception.message}")
            broadcastLocationError("Location request failed")
        }
    }

    private fun handleNewLocation(location: Location) {
        // Validate location
        if (!isValidLocation(location)) {
            Log.w(TAG, "Invalid location received: ${location.latitude}, ${location.longitude}")
            return
        }

        // Update current location
        currentLocation.value = location
        currentLatitude = location.latitude
        currentLongitude = location.longitude
        locationAccuracy.value = location.accuracy
        locationUpdatesCount.value++

        // Format update time
        val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
        lastUpdateTime.value = timeFormat.format(Date())

        // Broadcast location update
        broadcastLocationUpdate(location)

        // Update notification with new location
        val locationText = String.format(
            Locale.getDefault(),
            "Lat: %.6f, Lng: %.6f",
            location.latitude,
            location.longitude
        )
        updateNotification(locationText)

        // Handle emergency mode
        if (emergencyMode.value) {
            emergencyLocationUpdates.add(location)
            Log.d(TAG, "Emergency location recorded: $locationText")

            // Limit emergency locations to last 10
            if (emergencyLocationUpdates.size > 10) {
                emergencyLocationUpdates.removeAt(0)
            }
        }

        // Log first location
        if (isFirstLocation) {
            Log.d(TAG, "First location received: $locationText")
            isFirstLocation = false
        }
    }

    private fun broadcastLocationUpdate(location: Location) {
        val intent = Intent(ACTION_LOCATION_UPDATE).apply {
            putExtra(EXTRA_LOCATION, location)
            `package` = packageName
        }
        sendBroadcast(intent)
    }

    private fun broadcastLocationError(errorMessage: String) {
        val intent = Intent(ACTION_LOCATION_ERROR).apply {
            putExtra(EXTRA_ERROR_MESSAGE, errorMessage)
            `package` = packageName
        }
        sendBroadcast(intent)

        Log.e(TAG, "Location error: $errorMessage")
    }

    private fun updateLocationInterval(newInterval: Long) {
        // Build new request with updated interval
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, newInterval)
            .setMinUpdateIntervalMillis(newInterval / 2)
            .setMinUpdateDistanceMeters(LOCATION_REQUEST_MIN_DISTANCE)
            .build()

        // Restart updates with new interval
        locationCallback?.let {
            fusedLocationClient?.removeLocationUpdates(it)
            startLocationUpdates()
        }

        Log.d(TAG, "Location update interval changed to ${newInterval}ms")
    }

    private fun startLocationQualityChecks() {
        serviceJob?.cancel()
        serviceJob = CoroutineScope(Dispatchers.IO).launch {
            while (isServiceRunning.value) {
                delay(30000) // Check every 30 seconds

                val location = currentLocation.value
                if (location != null) {
                    val age = System.currentTimeMillis() - location.time
                    val accuracy = location.accuracy

                    if (age > MAX_LOCATION_AGE) {
                        Log.w(TAG, "Location data is stale: ${age}ms old")
                        // Request fresh location
                        getCurrentLocation()
                    }

                    if (accuracy > 50) { // Accuracy worse than 50 meters
                        Log.w(TAG, "Location accuracy is poor: ${accuracy}m")
                    }
                }
            }
        }
    }

    private fun startForegroundService() {
        val notification = buildNotification("Starting location tracking...")
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun updateNotification(contentText: String) {
        val notification = buildNotification(contentText)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(contentText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("AURA Location Tracking")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                R.drawable.ic_location_on,
                "Stop Tracking",
                getStopActionPendingIntent()
            )
            .build()
    }

    private fun getStopActionPendingIntent(): PendingIntent {
        val stopIntent = Intent(this, LocationService::class.java).apply {
            action = ACTION_STOP
        }

        return PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    // Utility functions
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isValidLocation(location: Location): Boolean {
        return location.latitude != 0.0 &&
                location.longitude != 0.0 &&
                location.accuracy > 0 &&
                location.time > 0
    }
}

class LocationServiceManager private constructor(private val context: Context) {
    companion object {
        @Volatile
        private var instance: LocationServiceManager? = null

        fun getInstance(context: Context): LocationServiceManager {
            return instance ?: synchronized(this) {
                instance ?: LocationServiceManager(context.applicationContext).also { instance = it }
            }
        }
    }

    fun startLocationService() {
        val intent = Intent(context, LocationService::class.java).apply {
            action = LocationService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stopLocationService() {
        val intent = Intent(context, LocationService::class.java).apply {
            action = LocationService.ACTION_STOP
        }
        context.startService(intent)
    }

    fun startEmergencyTracking() {
        val intent = Intent(context, LocationService::class.java).apply {
            action = LocationService.ACTION_START_EMERGENCY
        }
        context.startService(intent)
    }

    fun stopEmergencyTracking() {
        val intent = Intent(context, LocationService::class.java).apply {
            action = LocationService.ACTION_STOP_EMERGENCY
        }
        context.startService(intent)
    }

    fun isServiceRunning(): Boolean = LocationService.isServiceRunning.value
}

class LocationState {
    val currentLocation = LocationService.currentLocation
    val isServiceRunning = LocationService.isServiceRunning
    val locationAccuracy = LocationService.locationAccuracy
    val locationUpdatesCount = LocationService.locationUpdatesCount
    val lastUpdateTime = LocationService.lastUpdateTime
    val emergencyMode = LocationService.emergencyMode

    fun getFormattedCoordinates(): String {
        return currentLocation.value?.let { 
            String.format(Locale.getDefault(), "%.6f, %.6f", it.latitude, it.longitude)
        } ?: "Unknown"
    }

    fun getFormattedAccuracy(): String {
        return if (locationAccuracy.value > 0) {
            String.format(Locale.getDefault(), "%.1f meters", locationAccuracy.value)
        } else {
            "Unknown"
        }
    }
}
