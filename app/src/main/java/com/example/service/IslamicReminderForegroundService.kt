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
import com.example.util.IslamicReminderAudioPlayer

/**
 * IslamicReminderForegroundService
 *
 * Dedicated Foreground Service for automated Islamic Reminder (Short Bayan) playback.
 * Features:
 * - Maintains audio playback in foreground and background.
 * - Displays active clip title in Roman Urdu and persistent "Stop" tray action.
 * - Automatically terminates and releases all resources as soon as playback concludes or Stop is clicked.
 */
class IslamicReminderForegroundService : Service() {

    companion object {
        private const val TAG = "IslamicReminderService"
        const val FOREGROUND_NOTIFICATION_ID = 3001
        const val FOREGROUND_CHANNEL_ID = "islamic_reminder_playback_channel"

        const val ACTION_START_COUNTDOWN = "com.example.service.action.START_COUNTDOWN"
        const val ACTION_START_REMINDER = "com.example.service.action.START_REMINDER"
        const val ACTION_STOP_REMINDER = "com.example.service.action.STOP_REMINDER"

        const val EXTRA_CLIP_TITLE = "extra_clip_title"
        const val EXTRA_CLIP_ID = "extra_clip_id"
        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
        const val EXTRA_SECONDS_LEFT = "extra_seconds_left"

        fun startCountdown(context: Context, prayerName: String, secondsRemaining: Int = 30) {
            val intent = Intent(context, IslamicReminderForegroundService::class.java).apply {
                action = ACTION_START_COUNTDOWN
                putExtra(EXTRA_PRAYER_NAME, prayerName)
                putExtra(EXTRA_SECONDS_LEFT, secondsRemaining)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start countdown foreground service: ${e.message}", e)
            }
        }

        fun startService(context: Context, clipTitle: String, clipId: String) {
            val intent = Intent(context, IslamicReminderForegroundService::class.java).apply {
                action = ACTION_START_REMINDER
                putExtra(EXTRA_CLIP_TITLE, clipTitle)
                putExtra(EXTRA_CLIP_ID, clipId)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start IslamicReminderForegroundService: ${e.message}", e)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, IslamicReminderForegroundService::class.java).apply {
                action = ACTION_STOP_REMINDER
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop IslamicReminderForegroundService: ${e.message}", e)
            }
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "IslamicReminderForegroundService created")
        initChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_REMINDER -> {
                Log.d(TAG, "Action STOP received in IslamicReminderForegroundService")
                IslamicReminderAudioPlayer.stop(applicationContext)
                shutdown()
                return START_NOT_STICKY
            }
            ACTION_START_COUNTDOWN -> {
                val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: "Prayer"
                val secLeft = intent.getIntExtra(EXTRA_SECONDS_LEFT, 30)
                startForegroundForCountdown(prayerName, secLeft)
            }
            ACTION_START_REMINDER -> {
                val title = intent.getStringExtra(EXTRA_CLIP_TITLE) ?: "Islamic Reminder"
                startForegroundForReminder(title)
            }
            else -> {
                shutdown()
                return START_NOT_STICKY
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundForCountdown(prayerName: String, secLeft: Int) {
        acquireWakeLock()
        val notification = buildCountdownNotification(prayerName, secLeft)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                FOREGROUND_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(FOREGROUND_NOTIFICATION_ID, notification)
        }
    }

    private fun startForegroundForReminder(clipTitle: String) {
        acquireWakeLock()
        val notification = buildNotification(clipTitle)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                FOREGROUND_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(FOREGROUND_NOTIFICATION_ID, notification)
        }
    }

    private fun buildCountdownNotification(prayerName: String, secLeft: Int): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            201,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, IslamicReminderForegroundService::class.java).apply {
            action = ACTION_STOP_REMINDER
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            202,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, FOREGROUND_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🕌 Post-$prayerName Routine Reminder")
            .setContentText("Routine Reminder starting in ${secLeft}s...")
            .setSubText("Noor-e-Sunnat Short Bayan")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setContentIntent(openPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Cancel",
                stopPendingIntent
            )
            .build()
    }

    private fun buildNotification(clipTitle: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            201,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, IslamicReminderForegroundService::class.java).apply {
            action = ACTION_STOP_REMINDER
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            202,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, FOREGROUND_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🕌 Routine Reminder (Short Bayan)")
            .setContentText("Playing: $clipTitle")
            .setSubText("Noor-e-Sunnat Bayan")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setContentIntent(openPendingIntent)
            .addAction(
                android.R.drawable.ic_media_pause,
                "Stop",
                stopPendingIntent
            )
            .build()
    }

    private fun initChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val channel = NotificationChannel(
                FOREGROUND_CHANNEL_ID,
                "Routine Islamic Reminders",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Automated post-Adhan Islamic reminder speech audio"
                setSound(null, null)
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun acquireWakeLock() {
        try {
            if (wakeLock == null) {
                val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
                wakeLock = powerManager?.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "ImtiazLifestyle::IslamicReminderWakeLock"
                )
                wakeLock?.acquire(10 * 60 * 1000L) // Max 10 minutes safety
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to acquire wake lock: ${e.message}")
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to release wake lock: ${e.message}")
        }
        wakeLock = null
    }

    private fun shutdown() {
        try {
            releaseWakeLock()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            stopSelf()
        } catch (e: Exception) {
            Log.w(TAG, "Error during shutdown: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
        Log.d(TAG, "IslamicReminderForegroundService destroyed")
    }
}
