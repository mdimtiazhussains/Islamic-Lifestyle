package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.util.AzanAudioPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * AzanForegroundService
 *
 * Battery-optimized Audio Service:
 * Activates ONLY during prayer / Azan times to play the call to prayer audio and show media controls.
 * As soon as playback concludes, is cancelled, or reaches safety timeout (4 minutes),
 * it immediately stops foreground state, releases resources, and terminates (stopSelf()).
 * Remains completely inactive outside of prayer intervals.
 */
class AzanForegroundService : Service() {

    companion object {
        private const val TAG = "AzanForegroundService"
        const val FOREGROUND_NOTIFICATION_ID = 2001
        const val FOREGROUND_CHANNEL_ID = "azan_playback_service_channel"

        const val ACTION_PLAY_AZAN = "com.example.service.action.PLAY_AZAN"
        const val ACTION_STOP_AZAN = "com.example.service.action.STOP_AZAN"

        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
        const val EXTRA_PRAYER_TIME = "extra_prayer_time"

        fun playAzan(context: Context, prayerName: String, prayerTime: String) {
            val intent = Intent(context, AzanForegroundService::class.java).apply {
                action = ACTION_PLAY_AZAN
                putExtra(EXTRA_PRAYER_NAME, prayerName)
                putExtra(EXTRA_PRAYER_TIME, prayerTime)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start AzanForegroundService: ${e.message}", e)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, AzanForegroundService::class.java).apply {
                action = ACTION_STOP_AZAN
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop AzanForegroundService: ${e.message}", e)
            }
        }

        // Backward compatibility hook: schedules alarms and avoids 24/7 foreground drain
        fun startService(context: Context) {
            PrayerAlarmScheduler.schedulePrayerAlarms(context)
        }
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var wakeLock: PowerManager.WakeLock? = null
    private var playbackWatcherJob: Job? = null
    private var safetyTimeoutJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AzanForegroundService created for prayer interval")
        initChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_AZAN -> {
                Log.d(TAG, "Stopping Azan playback and terminating service")
                shutdownService()
                return START_NOT_STICKY
            }
            ACTION_PLAY_AZAN -> {
                val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: "Prayer"
                val prayerTime = intent.getStringExtra(EXTRA_PRAYER_TIME) ?: ""
                startForegroundForPlayback(prayerName, prayerTime)
            }
            else -> {
                // If started with no explicit action, perform safety shutdown after scheduling
                PrayerAlarmScheduler.schedulePrayerAlarms(applicationContext)
                shutdownService()
                return START_NOT_STICKY
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "AzanForegroundService destroyed. Complete inactivity outside prayer time.")
        playbackWatcherJob?.cancel()
        safetyTimeoutJob?.cancel()
        serviceScope.cancel()
        releaseWakeLock()
    }

    private fun startForegroundForPlayback(prayerName: String, prayerTime: String) {
        acquireTemporaryWakeLock()

        val notification = buildForegroundNotification(prayerName, prayerTime)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                FOREGROUND_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(FOREGROUND_NOTIFICATION_ID, notification)
        }

        // Trigger the Azan audio
        AzanAudioPlayer.triggerPrayerCall(
            context = applicationContext,
            scope = serviceScope,
            prayerName = prayerName,
            prayerTime = prayerTime,
            locationName = "Abu Dhabi"
        )

        // Monitor playback: when audio finishes or stops, immediately terminate this service!
        playbackWatcherJob?.cancel()
        playbackWatcherJob = serviceScope.launch {
            // Wait slightly for audio to start
            delay(1000)
            AzanAudioPlayer.isPlaying.collectLatest { playing ->
                if (!playing) {
                    Log.i(TAG, "Azan audio playback completed or stopped. Deactivating service to conserve battery.")
                    shutdownService()
                }
            }
        }

        // Safety timeout: Maximum 4 minutes playback limit ensures service NEVER runs indefinitely
        safetyTimeoutJob?.cancel()
        safetyTimeoutJob = serviceScope.launch {
            delay(240_000L) // 4 minutes
            Log.i(TAG, "Azan playback safety timeout reached. Deactivating service.")
            shutdownService()
        }
    }

    private fun shutdownService() {
        try {
            AzanAudioPlayer.stop(applicationContext)
            AzanAudioPlayer.dismissNotification(applicationContext)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping foreground: ${e.message}")
        } finally {
            stopSelf()
        }
    }

    private fun acquireTemporaryWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "ImtiazLifestyle:AzanPlaybackWakeLock"
            )?.apply {
                setReferenceCounted(false)
                acquire(4 * 60 * 1000L) // Holds wake lock ONLY during 4 min audio window
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not acquire temporary wake lock: ${e.message}")
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing wake lock: ${e.message}")
        }
        wakeLock = null
    }

    private fun initChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val fgChannel = NotificationChannel(
                FOREGROUND_CHANNEL_ID,
                "Azan Prayer Call Playback",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Active strictly during automated Azan prayer calls"
                setShowBadge(true)
            }
            manager.createNotificationChannel(fgChannel)

            AzanAudioPlayer.initNotificationChannel(this)
        }
    }

    private fun buildForegroundNotification(prayerName: String, prayerTime: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AzanNotificationReceiver::class.java).apply {
            action = AzanNotificationReceiver.ACTION_STOP_AZAN
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeLabel = if (prayerTime.isNotBlank()) " ($prayerTime)" else ""
        return NotificationCompat.Builder(this, FOREGROUND_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🕌 $prayerName Prayer Call Active$timeLabel")
            .setContentText("Playing Azan. Tap 'Stop' to dismiss audio.")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(
                android.R.drawable.ic_media_pause,
                "Stop Azan",
                stopPendingIntent
            )
            .build()
    }
}
