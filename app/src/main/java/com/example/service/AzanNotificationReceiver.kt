package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.firestore.FirestoreSyncService
import com.example.util.AzanAudioPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * AzanNotificationReceiver
 *
 * BroadcastReceiver activated strictly during prayer / Azan times:
 * 1. ACTION_PRAYER_ALARM: Woken by AlarmManager at exact prayer time.
 *    Activates Azan audio foreground playback and runs background cloud sync.
 *    Once the audio finishes or stops, the service completely deactivates.
 * 2. ACTION_STOP_AZAN / ACTION_DISMISS_AZAN: Stops audio immediately and shuts down the service.
 */
class AzanNotificationReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AzanNotifReceiver"
        const val ACTION_PRAYER_ALARM = "com.example.service.action.PRAYER_ALARM"
        const val ACTION_STOP_AZAN = "com.example.service.action.STOP_AZAN_NOTIF"
        const val ACTION_DISMISS_AZAN = "com.example.service.action.DISMISS_AZAN_NOTIF"

        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
        const val EXTRA_PRAYER_TIME = "extra_prayer_time"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        Log.i(TAG, "AzanNotificationReceiver received action: $action")

        when (action) {
            ACTION_PRAYER_ALARM -> {
                val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: "Prayer"
                val prayerTime = intent.getStringExtra(EXTRA_PRAYER_TIME) ?: ""
                Log.i(TAG, "Prayer alarm fired for $prayerName ($prayerTime). Activating audio service and sync.")

                // 1. Activate Azan audio playback service strictly for this prayer event
                AzanForegroundService.playAzan(context.applicationContext, prayerName, prayerTime)

                // 2. Perform background cloud sync only during this prayer interval
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val syncService = FirestoreSyncService(context.applicationContext)
                        if (syncService.currentUserId != null) {
                            Log.d(TAG, "Executing background sync during prayer time interval")
                            syncService.fetchAllTasks()
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Background sync during prayer interval encountered: ${e.message}")
                    }
                }

                // 3. Reschedule upcoming prayer alarms
                PrayerAlarmScheduler.schedulePrayerAlarms(context.applicationContext)
            }

            ACTION_STOP_AZAN, ACTION_DISMISS_AZAN -> {
                Log.i(TAG, "Stopping Azan playback and terminating service")
                AzanAudioPlayer.stop(context.applicationContext)
                AzanForegroundService.stopService(context.applicationContext)
            }
        }
    }
}
