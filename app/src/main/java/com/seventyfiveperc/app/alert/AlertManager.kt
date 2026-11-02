package com.seventyfiveperc.app.alert

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.seventyfiveperc.app.MainActivity
import com.seventyfiveperc.app.R
import com.seventyfiveperc.app.SeventyFivePercApp

/**
 * AlertManager — Fires high-priority notifications and vibration
 * when the user's name/roll number is detected.
 */
class AlertManager(private val context: Context) {

    companion object {
        private const val ALERT_NOTIFICATION_ID = 7500
        private const val LISTENING_NOTIFICATION_ID = 7501
    }

    /**
     * Fire a high-priority heads-up notification + vibration.
     * This will appear even over other apps like Instagram.
     */
    fun fireAttendanceAlert(matchedKeyword: String, confidence: Float, isPreAlert: Boolean = false) {
        // Vibrate based on alert type
        if (isPreAlert) {
            vibrateGentle()
        } else {
            vibrateUrgent()
        }

        // Build the notification
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val confidencePercent = (confidence * 100).toInt()
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val title = if (isPreAlert) "⚠️ NEXT PERSON CALLED!" else "🚨 YOUR ATTENDANCE! 🚨"
        val message = if (isPreAlert) {
            "Roll number $matchedKeyword was called. You're next! ⚡"
        } else {
            "\"$matchedKeyword\" was called! ($confidencePercent% match)"
        }

        val notification = NotificationCompat.Builder(context, SeventyFivePercApp.CHANNEL_ALERT)
            .setSmallIcon(if (isPreAlert) R.drawable.ic_warning else R.drawable.ic_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .apply {
                if (!isPreAlert) {
                    setSound(alarmSound)
                    setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
                    setFullScreenIntent(pendingIntent, true)
                } else {
                    setVibrate(longArrayOf(0, 300, 100, 300))
                }
            }
            .build()

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(ALERT_NOTIFICATION_ID, notification)
    }

    /**
     * Create the persistent notification for the foreground listening service.
     */
    fun createListeningNotification(): Notification {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, SeventyFivePercApp.CHANNEL_LISTENING)
            .setSmallIcon(R.drawable.ic_mic_listening)
            .setContentTitle("Attend.ai is listening...")
            .setContentText("Waiting for your name to be called")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setSilent(true)
            .build()
    }

    fun getListeningNotificationId(): Int = LISTENING_NOTIFICATION_ID

    /**
     * Dismiss the attendance alert notification.
     */
    fun dismissAlert() {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.cancel(ALERT_NOTIFICATION_ID)
    }

    private fun vibrateUrgent() {
        val vibrator = getVibrator()
        val effect = VibrationEffect.createWaveform(
            longArrayOf(0, 500, 200, 500, 200, 500, 200, 1000),
            -1  // don't repeat
        )
        vibrator.vibrate(effect)
    }

    private fun vibrateGentle() {
        val vibrator = getVibrator()
        val effect = VibrationEffect.createWaveform(
            longArrayOf(0, 300, 100, 300),
            -1
        )
        vibrator.vibrate(effect)
    }

    private fun getVibrator(): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(VibratorManager::class.java)
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
}
