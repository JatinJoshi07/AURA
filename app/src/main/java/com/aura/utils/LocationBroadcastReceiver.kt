package com.aura.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build

class LocationBroadcastReceiver(
    private val onLocationUpdate: (Location) -> Unit,
    private val onLocationError: (String) -> Unit
) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            LocationService.ACTION_LOCATION_UPDATE -> {
                val location = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(LocationService.EXTRA_LOCATION, Location::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(LocationService.EXTRA_LOCATION) as? Location
                }
                location?.let { onLocationUpdate(it) }
            }
            LocationService.ACTION_LOCATION_ERROR -> {
                val error = intent.getStringExtra(LocationService.EXTRA_ERROR_MESSAGE) ?: "Unknown error"
                onLocationError(error)
            }
        }
    }
}
