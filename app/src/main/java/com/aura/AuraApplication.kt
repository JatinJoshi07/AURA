package com.aura

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AuraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize any app-wide configurations here
    }
}