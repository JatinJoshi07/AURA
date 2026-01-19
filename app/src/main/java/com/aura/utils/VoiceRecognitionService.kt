package com.aura.utils

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.aura.MainActivity
import com.aura.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class VoiceRecognitionService : Service() {

    @Inject
    lateinit var voiceRecognition: VoiceRecognition

    companion object {
        private const val NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "voice_guard_channel"
        const val ACTION_START_GUARD = "ACTION_START_GUARD"
        const val ACTION_STOP_GUARD = "ACTION_STOP_GUARD"
        const val EXTRA_PANIC_WORD = "extra_panic_word"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_GUARD -> {
                val panicWord = intent.getStringExtra(EXTRA_PANIC_WORD) ?: "HELP"
                val notification = buildNotification("Voice Guard Active: listening for \"$panicWord\"")
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
                
                voiceRecognition.startListening(panicWord, continuous = true)
            }
            ACTION_STOP_GUARD -> {
                voiceRecognition.stopListening()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AURA Voice Guard",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the voice-activated SOS active in the background"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AURA Pink Shield")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        voiceRecognition.cleanup()
        super.onDestroy()
    }
}
