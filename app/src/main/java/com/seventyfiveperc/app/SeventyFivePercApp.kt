package com.seventyfiveperc.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

import com.seventyfiveperc.app.data.local.AppDatabase

class SeventyFivePercApp : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }

    companion object {
        const val CHANNEL_LISTENING = "listening_channel"
        const val CHANNEL_ALERT = "alert_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        // Channel for the persistent "Listening..." foreground service notification
        val listeningChannel = NotificationChannel(
            CHANNEL_LISTENING,
            "Listening Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows when Attend.ai is actively listening for your name"
            setShowBadge(false)
        }

        // Channel for the ALERT when your name/number is detected
        val alertChannel = NotificationChannel(
            CHANNEL_ALERT,
            "Attendance Alert",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts you when your name or roll number is called"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
            setBypassDnd(true)
            
            val alarmSound = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
            setSound(alarmSound, android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build())
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(listeningChannel)
        manager.createNotificationChannel(alertChannel)
    }
}
