package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.service.AzanNotificationReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * AzanAudioPlayer
 *
 * Manages authentic Azan audio playback using the authentic Azan audio
 * for all Azan prayer notifications and interactive audio previews.
 *
 * Fixes:
 * 1. Single coordinated trigger: Deduplicates notification posting and audio playback
 *    to guarantee only a single instance of notification and audio plays.
 * 2. Overlapping audio prevention: Notification channel has no ringtone sound attached,
 *    allowing MediaPlayer to be the single source of truth for audio.
 * 3. Tray actions: Provides persistent "Stop" and "Dismiss" action buttons directly in the notification tray.
 * 4. Dismissal: Notification can be swiped away (not ongoing) with a deleteIntent that cancels audio,
 *    or is automatically dismissed once playback concludes.
 */
object AzanAudioPlayer {

    private const val TAG = "AzanAudioPlayer"
    const val NOTIFICATION_CHANNEL_ID = "azan_prayer_channel"
    const val NOTIFICATION_ID = 1001

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _activePrayer = MutableStateFlow<String?>(null)
    val activePrayer: StateFlow<String?> = _activePrayer.asStateFlow()

    private val _progressFraction = MutableStateFlow(0f)
    val progressFraction: StateFlow<Float> = _progressFraction.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private var appContext: Context? = null

    // Deduplication state to prevent double notifications and overlapping audio
    private var lastTriggeredPrayerKey: String? = null
    private var lastTriggeredTimestamp: Long = 0L

    /**
     * Initializes the Android Notification Channel.
     * Note: Sound on the channel is intentionally null so that AzanAudioPlayer (MediaPlayer)
     * has exclusive, controllable playback—preventing the system notification ringtone
     * and MediaPlayer from playing simultaneously (which causes echo/overlapping audio).
     */
    fun initNotificationChannel(context: Context) {
        appContext = context.applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Azan Prayer Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Automated call to prayer notifications"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500)
                // Omit ringtone sound so only MediaPlayer plays the Azan
                setSound(null, null)
            }

            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Coordinated single entry-point for triggering Azan prayer notifications and playback.
     * Uses synchronization and time-based deduplication to prevent double notifications
     * and overlapping audio playback across background service and UI loops.
     */
    @Synchronized
    fun triggerPrayerCall(
        context: Context,
        scope: CoroutineScope,
        prayerName: String,
        prayerTime: String,
        locationName: String
    ) {
        val key = "$prayerName-$prayerTime"
        val now = System.currentTimeMillis()
        if (key == lastTriggeredPrayerKey && (now - lastTriggeredTimestamp) < 65_000L) {
            Log.d(TAG, "Prayer call for $key already triggered within last 65 seconds, skipping.")
            return
        }
        lastTriggeredPrayerKey = key
        lastTriggeredTimestamp = now

        Log.i(TAG, "Triggering single coordinated Azan prayer call: $prayerName at $prayerTime")

        // 1. Post single notification with Stop/Dismiss actions & swipe-to-dismiss deleteIntent
        postAzanNotification(context, prayerName, prayerTime, locationName)

        // 2. Play single authentic Azan audio instance via MediaPlayer
        playAzan(context, scope, prayerName)
    }

    /**
     * Plays the authentic UAE Azan audio for the specified prayer.
     * Ensures any previous playback is stopped and released first.
     */
    fun playAzan(context: Context, scope: CoroutineScope, prayerName: String = "Prayer") {
        appContext = context.applicationContext

        // Rapid Re-trigger Suppression: Cancel any active reminder countdown or speech playback immediately
        IslamicReminderAudioPlayer.suppressAnyActivePlayback("New Adhan triggered for $prayerName")

        if (_isPlaying.value && _activePrayer.value == prayerName) {
            stop(context)
            return
        }

        stop(context)

        try {
            val player = MediaPlayer.create(context.applicationContext, R.raw.uae_azan)
            if (player == null) {
                Log.e(TAG, "Failed to create MediaPlayer with R.raw.uae_azan")
                return
            }

            mediaPlayer = player
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )

            // When Azan concludes naturally, automatically dismiss notification, reset state,
            // and trigger the 30-second post-Adhan Islamic reminder countdown hook.
            player.setOnCompletionListener {
                Log.i(TAG, "Azan playback completed naturally; dismissing notification and stopping")
                val completedPrayer = _activePrayer.value ?: prayerName
                dismissNotification(context)
                stop(context)

                // Hook into Adhan completion listener for Islamic Reminder (Short Bayan)
                IslamicReminderAudioPlayer.onAdhanCompleted(context, completedPrayer)
            }

            player.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                dismissNotification(context)
                stop(context)
                true
            }

            player.start()
            _isPlaying.value = true
            _activePrayer.value = prayerName

            // Monitor playback progress
            progressJob = scope.launch(Dispatchers.Main) {
                while (isActive && _isPlaying.value) {
                    val current = mediaPlayer?.currentPosition ?: 0
                    val total = mediaPlayer?.duration ?: 1
                    if (total > 0) {
                        _progressFraction.value = (current.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                    }
                    delay(300)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception while playing Azan audio: ${e.message}", e)
            dismissNotification(context)
            stop(context)
        }
    }

    /**
     * Preview Azan audio.
     */
    fun playPreview(context: Context, scope: CoroutineScope) {
        playAzan(context, scope, "Azan Audio")
    }

    /**
     * Dismisses the Azan notification from the notification tray.
     */
    fun dismissNotification(context: Context? = null) {
        try {
            val ctx = context ?: appContext ?: return
            val notificationManager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.cancel(NOTIFICATION_ID)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to dismiss Azan notification: ${e.message}")
        }
    }

    /**
     * Simulation Helper:
     * Fast-forwards the active Azan audio to 5 seconds before completion
     * so that the natural onCompletionListener -> 30s countdown -> zero-click Bayan
     * flow can be observed and verified in seconds without waiting 3.5 minutes.
     */
    fun skipToLastFiveSeconds() {
        try {
            val player = mediaPlayer ?: return
            if (_isPlaying.value && player.duration > 6_000) {
                player.seekTo(player.duration - 5_000)
                Log.i(TAG, "Fast-forwarded Azan to last 5 seconds (Testing Simulation)")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to skip Azan to last 5 seconds: ${e.message}")
        }
    }

    /**
     * Direct Simulation Helper:
     * Immediately finishes Azan and triggers the 30-second post-Adhan countdown hook.
     */
    fun simulateAdhanCompletion(context: Context, prayerName: String = "Fajr") {
        Log.i(TAG, "Directly simulating Adhan completion for: $prayerName")
        stop(context)
        IslamicReminderAudioPlayer.onAdhanCompleted(context, prayerName)
    }

    /**
     * Stops currently playing Azan audio, resets state, and dismisses the notification.
     */
    fun stop(context: Context? = null) {
        progressJob?.cancel()
        progressJob = null
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing MediaPlayer: ${e.message}")
        }
        mediaPlayer = null
        _isPlaying.value = false
        _activePrayer.value = null
        _progressFraction.value = 0f

        dismissNotification(context)
    }

    /**
     * Posts a system notification for the prayer.
     * Features:
     * - Persistent "Stop" and "Dismiss" action buttons directly in the notification tray
     *   allowing immediate cancellation without opening the app.
     * - Swipe-to-dismiss support: autoCancel=true and deleteIntent ensure swiping cancels the audio.
     * - Sound is null on the notification builder to prevent double/overlapping audio playback.
     */
    fun postAzanNotification(context: Context, prayerName: String, prayerTime: String, locationName: String) {
        try {
            appContext = context.applicationContext
            initNotificationChannel(context)

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Direct Action 1: "Stop" Action Button in notification tray
            val stopIntent = Intent(context, AzanNotificationReceiver::class.java).apply {
                action = AzanNotificationReceiver.ACTION_STOP_AZAN
            }
            val stopPendingIntent = PendingIntent.getBroadcast(
                context,
                101,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Direct Action 2: "Dismiss" Action Button in notification tray
            val dismissIntent = Intent(context, AzanNotificationReceiver::class.java).apply {
                action = AzanNotificationReceiver.ACTION_DISMISS_AZAN
            }
            val dismissPendingIntent = PendingIntent.getBroadcast(
                context,
                102,
                dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val locationSnippet = if (locationName.isNotBlank() && !locationName.contains("assabile", ignoreCase = true)) " • $locationName" else ""

            val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("🕌 Azan: It is time for $prayerName Prayer ($prayerTime)")
                .setContentText("Call to Prayer ($prayerTime)$locationSnippet")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setOngoing(false) // Allows swiping away!
                .setContentIntent(pendingIntent)
                .setDeleteIntent(dismissPendingIntent) // Stops audio if swiped away!
                .setVibrate(longArrayOf(0, 500, 250, 500))
                .addAction(
                    android.R.drawable.ic_media_pause,
                    "Stop",
                    stopPendingIntent
                )
                .addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    "Dismiss",
                    dismissPendingIntent
                )
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    Log.w(TAG, "Cannot post notification: POST_NOTIFICATIONS permission not granted")
                    return
                }
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post Azan notification: ${e.message}", e)
        }
    }
}
