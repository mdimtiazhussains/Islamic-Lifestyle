package com.example.util

import android.content.Context
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Available Quran Live Radio Streams from qurango.net
 */
enum class QuranRadioStream(
    val id: String,
    val title: String,
    val subtitle: String,
    val arabicName: String,
    val streamUrl: String
) {
    MIX(
        id = "mix",
        title = "Radio Mix",
        subtitle = "Recitations, translations & reflections",
        arabicName = "إذاعة القرآن العامة (ميكس)",
        streamUrl = "https://qurango.net/radio/mix"
    ),
    TARATEEL(
        id = "tarateel",
        title = "Radio Tarateel",
        subtitle = "Continuous peaceful Murattal recitations",
        arabicName = "إذاعة تراتيل القرآن الكريم",
        streamUrl = "https://qurango.net/radio/tarateel"
    )
}

/**
 * Singleton Live Radio Player utilizing AndroidX Media3 ExoPlayer and MediaSession.
 * Supports background live streaming, stream switching, volume/mute, and error handling.
 */
object QuranRadioManager {
    private const val TAG = "QuranRadioManager"

    private var exoPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    private val _activeStream = MutableStateFlow(QuranRadioStream.MIX)
    val activeStream: StateFlow<QuranRadioStream> = _activeStream.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _volume = MutableStateFlow(1.0f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private fun getOrCreatePlayer(context: Context): ExoPlayer {
        exoPlayer?.let { return it }

        val appContext = context.applicationContext

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val player = ExoPlayer.Builder(appContext)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                _isPlaying.value = playing
                if (playing) {
                    _isBuffering.value = false
                    _errorMessage.value = null
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        _isBuffering.value = true
                    }
                    Player.STATE_READY -> {
                        _isBuffering.value = false
                        _isPlaying.value = player.playWhenReady
                        _errorMessage.value = null
                    }
                    Player.STATE_ENDED -> {
                        _isBuffering.value = false
                        _isPlaying.value = false
                    }
                    Player.STATE_IDLE -> {
                        _isBuffering.value = false
                        _isPlaying.value = false
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "ExoPlayer playback error: ${error.errorCodeName} - ${error.message}", error)
                _isBuffering.value = false
                _isPlaying.value = false
                _errorMessage.value = "Failed to connect to ${_activeStream.value.title}. Check network connection."
            }
        })

        // Apply saved volume/mute
        player.volume = if (_isMuted.value) 0f else _volume.value

        exoPlayer = player

        // Build Media3 MediaSession
        try {
            mediaSession = MediaSession.Builder(appContext, player)
                .setId("QuranLiveRadioMediaSession")
                .build()
        } catch (e: Exception) {
            Log.w(TAG, "Unable to create MediaSession: ${e.message}")
        }

        return player
    }

    /**
     * Start playing the specified stream, or resume if already loaded.
     */
    fun playStream(context: Context, stream: QuranRadioStream) {
        try {
            _activeStream.value = stream
            _errorMessage.value = null
            _isBuffering.value = true

            val player = getOrCreatePlayer(context)

            val metadata = MediaMetadata.Builder()
                .setTitle(stream.title)
                .setArtist("Quran Live")
                .setDisplayTitle(stream.title)
                .setSubtitle(stream.subtitle)
                .setDescription(stream.arabicName)
                .build()

            val mediaItem = MediaItem.Builder()
                .setUri(stream.streamUrl)
                .setMediaMetadata(metadata)
                .build()

            player.setMediaItem(mediaItem)
            player.prepare()
            player.playWhenReady = true
            player.play()
        } catch (e: Exception) {
            Log.e(TAG, "Error starting live stream ${stream.streamUrl}: ${e.message}", e)
            _isBuffering.value = false
            _isPlaying.value = false
            _errorMessage.value = "Unable to start stream: ${e.localizedMessage}"
        }
    }

    /**
     * Switch to a new stream.
     */
    fun selectStream(context: Context, stream: QuranRadioStream, autoPlay: Boolean = true) {
        if (_activeStream.value == stream && _isPlaying.value) {
            return
        }
        _activeStream.value = stream
        _errorMessage.value = null
        if (autoPlay || _isPlaying.value) {
            playStream(context, stream)
        } else {
            // Just update media item without starting playback
            val player = getOrCreatePlayer(context)
            val metadata = MediaMetadata.Builder()
                .setTitle(stream.title)
                .setArtist("Quran Live")
                .setDisplayTitle(stream.title)
                .setSubtitle(stream.subtitle)
                .setDescription(stream.arabicName)
                .build()
            val mediaItem = MediaItem.Builder()
                .setUri(stream.streamUrl)
                .setMediaMetadata(metadata)
                .build()
            player.setMediaItem(mediaItem)
            player.prepare()
        }
    }

    /**
     * Toggle between Play and Pause.
     */
    fun togglePlayPause(context: Context) {
        val player = getOrCreatePlayer(context)
        if (player.isPlaying) {
            player.pause()
            _isPlaying.value = false
            _isBuffering.value = false
        } else {
            if (player.playbackState == Player.STATE_IDLE || player.currentMediaItem == null || _errorMessage.value != null) {
                playStream(context, _activeStream.value)
            } else {
                player.play()
            }
        }
    }

    /**
     * Stop streaming completely.
     */
    fun stop() {
        try {
            exoPlayer?.stop()
            _isPlaying.value = false
            _isBuffering.value = false
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping ExoPlayer: ${e.message}")
        }
    }

    /**
     * Retry connecting to the active stream.
     */
    fun retry(context: Context) {
        playStream(context, _activeStream.value)
    }

    /**
     * Set playback volume (0.0 to 1.0).
     */
    fun setVolume(vol: Float) {
        val clamped = vol.coerceIn(0f, 1f)
        _volume.value = clamped
        if (!_isMuted.value) {
            exoPlayer?.volume = clamped
        }
    }

    /**
     * Toggle mute on/off.
     */
    fun toggleMute() {
        val newMute = !_isMuted.value
        _isMuted.value = newMute
        exoPlayer?.volume = if (newMute) 0f else _volume.value
    }

    /**
     * Clean release of player and media session when application destroys.
     */
    fun release() {
        try {
            mediaSession?.release()
            mediaSession = null
            exoPlayer?.release()
            exoPlayer = null
            _isPlaying.value = false
            _isBuffering.value = false
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing ExoPlayer: ${e.message}")
        }
    }
}
