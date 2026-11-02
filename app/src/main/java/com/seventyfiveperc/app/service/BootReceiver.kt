package com.seventyfiveperc.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.seventyfiveperc.app.data.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = AppPreferences(context)
            CoroutineScope(Dispatchers.IO).launch {
                // If the user had a profile set, we might want to resume listening 
                // but usually it's better to let the user start it manually or 
                // check if it was running before. For now, let's just log.
                if (prefs.isProfileSet.first()) {
                    // Logic to restart service if needed could go here
                }
            }
        }
    }
}
