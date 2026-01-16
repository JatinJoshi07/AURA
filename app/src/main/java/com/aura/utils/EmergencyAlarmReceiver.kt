package com.aura.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class EmergencyAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("EmergencyAlarmReceiver", "Alarm triggered")
        // Handle emergency alarm logic
    }
}
