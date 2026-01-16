package com.aura.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class EmergencyBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("EmergencyReceiver", "Received: ${intent?.action}")
        // Handle emergency actions
    }
}
