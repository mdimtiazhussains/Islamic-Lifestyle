package com.example.util

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.data.model.BayanClipEntity
import com.example.data.repository.BayanRepository
import com.example.service.IslamicReminderForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * IslamicReminderAudioPlayer
 *
 * Core engine powering automated, zero-click Islamic Reminder (Short Bayan) speech audio
 * playback following Adhan completion in the Routine tab.
 *
 * Adheres strictly to:
 * 1. Zero-Click Rule: 100% automated trigger after non-blocking 30-second post-Adhan timer.
 * 2. Rapid Re-trigger Suppression: Immediately halts previous jobs if a new Adhan starts.
 * 3. Audio Focus: Speech attributes (CONTENT_TYPE_SPEECH, USAGE_MEDIA); immediately pauses on incoming calls.
 * 4. 10-Second Network Timeout & Local Fallback: Never hangs or drains battery; falls back smoothly to local audio.
 * 5. Player Volume Slider: Bound strictly to player's volume without touching device ringtone/master volume.
 * 6. Background Survival: Holds Foreground Service & WakeLock across 30s countdown and playback.
 */
object IslamicReminderAudioPlayer {

    private const val TAG = "IslamicReminderPlayer"

    sealed interface ReminderUiState {
        data object Idle : ReminderUiState
        data class Countdown(val secondsRemaining: Int, val prayerName: String) : ReminderUiState
        data class Playing(
            val clip: BayanClipEntity,
            val currentPositionMs: Int,
            val totalDurationMs: Int
        ) : ReminderUiState {
            val progressFraction: Float
                get() = if (totalDurationMs > 0) (currentPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f) else 0f

            val formattedProgress: String
                get() {
                    val curSec = currentPositionMs / 1000
                    val totSec = totalDurationMs / 1000
                    return "%02d:%02d / %02d:%02d".format(curSec / 60, curSec % 60, totSec / 60, totSec % 60)
                }
        }
    }

    private val playerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _reminderState = MutableStateFlow<ReminderUiState>(ReminderUiState.Idle)
    val reminderState: StateFlow<ReminderUiState> = _reminderState.asStateFlow()

    private val _playerVolume = MutableStateFlow(0.85f)
    val playerVolume: StateFlow<Float> = _playerVolume.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var countdownJob: Job? = null
    private var progressJob: Job? = null
    private var prefetchJob: Job? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var appContext: Context? = null
    private var nextPreparedClip: BayanClipEntity? = null
    private var failoverRetryCount = 0

    // Audio Focus listener to pause/stop on incoming calls or external alarms
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                Log.i(TAG, "Audio focus lost (e.g. phone call or external media). Stopping bayan.")
                stop()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                mediaPlayer?.setVolume(0.2f, 0.2f)
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                val vol = _playerVolume.value
                mediaPlayer?.setVolume(vol, vol)
            }
        }
    }

    /**
     * Hook called when an authentic Adhan finishes playing naturally.
     * Starts foreground service + WakeLock and triggers the non-blocking 30-second countdown.
     */
    fun onAdhanCompleted(context: Context, prayerName: String) {
        appContext = context.applicationContext
        Log.i(TAG, "Adhan completed for $prayerName. Initiating 30-second post-Adhan countdown...")

        // Cancel any pending countdown or active bayan
        suppressAnyActivePlayback("New post-Adhan trigger for $prayerName")

        // Start Foreground Service immediately so Android Doze does not kill countdown
        IslamicReminderForegroundService.startCountdown(context, prayerName, 30)

        // Pre-fetch next unplayed clip during the 30-second countdown
        prefetchJob = playerScope.launch(Dispatchers.IO) {
            try {
                val repository = BayanRepository.getInstance(context)
                val clip = repository.getNextUnplayedClip()
                nextPreparedClip = clip
                Log.d(TAG, "Selected clip for post-$prayerName: '${clip.title}'")

                // Pre-cache audio file during the 30-second interval
                val safeUrl = clip.audioUrl.replace(" ", "%20")
                val cacheFile = File(context.cacheDir, "bayan_${clip.id}.mp4")
                if (!cacheFile.exists() || cacheFile.length() < 20_000L) {
                    downloadClipToFile(safeUrl, cacheFile)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Pre-fetch during countdown encountered error: ${e.message}")
            }
        }

        countdownJob = playerScope.launch {
            for (sec in 30 downTo 1) {
                _reminderState.value = ReminderUiState.Countdown(secondsRemaining = sec, prayerName = prayerName)
                updateCountdownNotification(context, prayerName, sec)
                delay(1000)
            }

            // 30 seconds elapsed -> Zero-click automated playback starts!
            _reminderState.value = ReminderUiState.Idle
            playNextAutomatedReminder(context.applicationContext)
        }
    }

    private fun updateCountdownNotification(context: Context, prayerName: String, secRemaining: Int) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val openIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            val openPendingIntent = if (openIntent != null) {
                android.app.PendingIntent.getActivity(
                    context,
                    201,
                    openIntent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
            } else null

            val notif = NotificationCompat.Builder(context, IslamicReminderForegroundService.FOREGROUND_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("🕌 Post-$prayerName Routine Reminder")
                .setContentText("Routine Reminder starting in ${secRemaining}s...")
                .setSubText("Noor-e-Sunnat Short Bayan")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setOngoing(true)
                .setContentIntent(openPendingIntent)
                .build()

            nm.notify(IslamicReminderForegroundService.FOREGROUND_NOTIFICATION_ID, notif)
        } catch (_: Exception) {}
    }

    /**
     * Rapid Re-trigger Suppression:
     * If a new Adhan starts or user stops playback, immediately abort countdown & playback.
     */
    fun suppressAnyActivePlayback(reason: String) {
        Log.d(TAG, "Suppressing active playback: $reason")
        countdownJob?.cancel()
        countdownJob = null
        prefetchJob?.cancel()
        prefetchJob = null
        nextPreparedClip = null
        stopInternal(abandonFocus = true)
    }

    /**
     * Automatically picks an unplayed clip from the local Room database, prepares,
     * and starts zero-click audio playback.
     */
    private fun playNextAutomatedReminder(context: Context) {
        playerScope.launch {
            try {
                val clip = nextPreparedClip ?: BayanRepository.getInstance(context).getNextUnplayedClip()
                Log.i(TAG, "Starting zero-click automated playback: '${clip.title}'")
                startPlaybackForClip(context, clip)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start automated bayan: ${e.message}", e)
                fallbackToBundledAudio(context, null)
            }
        }
    }

    /**
     * Starts playback for a given Bayan clip with speech AudioAttributes,
     * audio focus handling, and 10-second timeout.
     */
    private suspend fun startPlaybackForClip(context: Context, clip: BayanClipEntity) {
        appContext = context.applicationContext

        // Request Audio Focus for speech
        if (!requestAudioFocus(context)) {
            Log.w(TAG, "Unable to acquire audio focus for speech playback")
            return
        }

        // Switch foreground notification to playing
        IslamicReminderForegroundService.startService(context, clip.title, clip.id)

        val mediaSuccess = withContext(Dispatchers.IO) {
            setupMediaPlayerForClip(context, clip)
        }

        if (!mediaSuccess) {
            Log.w(TAG, "Network stream/prep failed or timed out (>10s). Trying next clip or fallback.")
            if (failoverRetryCount < 2) {
                failoverRetryCount++
                try {
                    val nextClip = BayanRepository.getInstance(context).getNextUnplayedClip()
                    startPlaybackForClip(context, nextClip)
                } catch (e: Exception) {
                    fallbackToBundledAudio(context, clip)
                }
            } else {
                failoverRetryCount = 0
                fallbackToBundledAudio(context, clip)
            }
        } else {
            failoverRetryCount = 0
        }
    }

    private suspend fun setupMediaPlayerForClip(context: Context, clip: BayanClipEntity): Boolean {
        return try {
            val player = MediaPlayer()
            mediaPlayer = player

            val speechAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()

            player.setAudioAttributes(speechAttributes)
            val currentVol = _playerVolume.value
            player.setVolume(currentVol, currentVol)

            val safeUrl = clip.audioUrl.replace(" ", "%20")
            val cacheFile = File(context.cacheDir, "bayan_${clip.id}.mp4")

            // Check if cached file exists and has reasonable size
            val isLocallyCached = cacheFile.exists() && cacheFile.length() > 20_000L

            if (isLocallyCached) {
                Log.d(TAG, "Playing cached clip from: ${cacheFile.absolutePath} (${cacheFile.length()} bytes)")
                player.setDataSource(cacheFile.absolutePath)
                player.prepare()
            } else {
                // If not yet cached, download via HttpURLConnection with browser headers within 10s
                val downloadOk = withTimeoutOrNull(10_000L) {
                    downloadClipToFile(safeUrl, cacheFile)
                } ?: false

                if (downloadOk && cacheFile.exists() && cacheFile.length() > 20_000L) {
                    player.setDataSource(cacheFile.absolutePath)
                    player.prepare()
                } else {
                    // Try direct streaming with 10-second timeout using HTTP headers required by NooreSunnat
                    val prepOk = withTimeoutOrNull(10_000L) {
                        try {
                            val headers = mapOf(
                                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                                "Accept" to "*/*",
                                "Referer" to "https://www.nooresunnat.com/shortwhatsappbayan.php"
                            )
                            player.setDataSource(context, Uri.parse(safeUrl), headers)
                            player.prepare()
                            true
                        } catch (e: Exception) {
                            Log.w(TAG, "Streaming prepare failed: ${e.message}")
                            false
                        }
                    } ?: false

                    if (!prepOk) {
                        player.release()
                        return false
                    }
                }
            }

            configureAndStartPlayer(context, clip)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Exception preparing MediaPlayer: ${e.message}")
            try { mediaPlayer?.release() } catch (_: Exception) {}
            mediaPlayer = null
            false
        }
    }

    private fun configureAndStartPlayer(context: Context, clip: BayanClipEntity) {
        val player = mediaPlayer ?: return

        player.setOnCompletionListener {
            Log.i(TAG, "Bayan playback finished naturally for: ${clip.title}")
            playerScope.launch {
                try {
                    BayanRepository.getInstance(context).markClipPlayed(clip.id)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to mark clip played: ${e.message}")
                }
                stop(context)
            }
        }

        player.setOnErrorListener { _, what, extra ->
            Log.e(TAG, "MediaPlayer error occurred: what=$what extra=$extra")
            stop(context)
            true
        }

        player.start()
        val duration = if (player.duration > 0) player.duration else (clip.durationSec * 1000)
        _reminderState.value = ReminderUiState.Playing(clip, 0, duration)

        // Progress monitor loop
        progressJob?.cancel()
        progressJob = playerScope.launch {
            while (isActive && mediaPlayer?.isPlaying == true) {
                val curPos = mediaPlayer?.currentPosition ?: 0
                val totDur = mediaPlayer?.duration ?: duration
                _reminderState.value = ReminderUiState.Playing(clip, curPos, totDur)
                delay(300)
            }
        }
    }

    /**
     * Local Cache Fallback:
     * Plays authentic bundled speech audio (`R.raw.bayan_fallback`) when device is offline
     * or connection times out after 10 seconds.
     */
    private fun fallbackToBundledAudio(context: Context, clip: BayanClipEntity?) {
        try {
            stopInternal(abandonFocus = false)

            val player = MediaPlayer.create(context, R.raw.bayan_fallback) ?: return
            mediaPlayer = player

            val speechAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()

            player.setAudioAttributes(speechAttributes)
            val currentVol = _playerVolume.value
            player.setVolume(currentVol, currentVol)

            val effectiveClip = clip ?: BayanClipEntity(
                id = "bayan_fallback",
                title = "Waaldain Ka Adab o Ehtiram",
                audioUrl = "",
                durationSec = 120,
                speaker = "Maulana Mehboob Elahi Sahab",
                category = "Short WhatsApp Bayan"
            )

            IslamicReminderForegroundService.startService(context, effectiveClip.title, effectiveClip.id)
            configureAndStartPlayer(context, effectiveClip)
        } catch (e: Exception) {
            Log.e(TAG, "Error playing fallback audio: ${e.message}", e)
            stop(context)
        }
    }

    private fun downloadClipToFile(audioUrl: String, destFile: File): Boolean {
        return try {
            val url = URL(audioUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            conn.setRequestProperty("Accept", "*/*")
            conn.setRequestProperty("Referer", "https://www.nooresunnat.com/shortwhatsappbayan.php")
            conn.connectTimeout = 8_000
            conn.readTimeout = 12_000
            conn.connect()

            if (conn.responseCode in 200..299) {
                val tempFile = File(destFile.parentFile, "${destFile.name}.tmp")
                conn.inputStream.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
                if (tempFile.length() > 20_000L) {
                    tempFile.renameTo(destFile)
                    Log.d(TAG, "Successfully downloaded clip: ${destFile.name} (${destFile.length()} bytes)")
                    conn.disconnect()
                    return true
                }
            }
            conn.disconnect()
            false
        } catch (e: Exception) {
            Log.w(TAG, "Download clip failed: ${e.message}")
            false
        }
    }

    /**
     * Volume Slider Handler:
     * Adjusts the MediaPlayer volume directly from 0.0f to 1.0f.
     * Strictly modifies the player's internal media volume WITHOUT changing the device
     * ringtone or system volume.
     */
    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        _playerVolume.value = clamped
        try {
            mediaPlayer?.setVolume(clamped, clamped)
        } catch (e: Exception) {
            Log.w(TAG, "Error setting media volume: ${e.message}")
        }
    }

    /**
     * Halts playback immediately, releases resources, abandons audio focus,
     * stops foreground service, and resets state to Idle.
     */
    fun stop(context: Context? = null) {
        countdownJob?.cancel()
        countdownJob = null
        prefetchJob?.cancel()
        prefetchJob = null
        nextPreparedClip = null

        val ctx = context ?: appContext
        if (ctx != null) {
            IslamicReminderForegroundService.stopService(ctx)
        }

        stopInternal(abandonFocus = true)
        _reminderState.value = ReminderUiState.Idle
    }

    private fun stopInternal(abandonFocus: Boolean) {
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

        if (abandonFocus) {
            abandonAudioFocus()
        }
    }

    private fun requestAudioFocus(context: Context): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return true
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val speechAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()

            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(speechAttributes)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            audioFocusRequest = request
            audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        try {
            val ctx = appContext ?: return
            val audioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(audioFocusChangeListener)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error abandoning audio focus: ${e.message}")
        }
        audioFocusRequest = null
    }
}
