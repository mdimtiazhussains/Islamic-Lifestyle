package com.example.util

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

/**
 * VoiceJournalRecorder
 *
 * Handles recording audio for voice-to-text diary reflections and playback of recorded clips.
 */
class VoiceJournalRecorder(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    companion object {
        private const val TAG = "VoiceJournalRecorder"
    }

    sealed class RecordingState {
        object Idle : RecordingState()
        data class Recording(val durationSeconds: Int, val maxAmplitude: Int = 0) : RecordingState()
        data class Completed(val file: File, val durationSeconds: Int) : RecordingState()
        data class Error(val message: String) : RecordingState()
    }

    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private val _isPlayingAudio = MutableStateFlow(false)
    val isPlayingAudio: StateFlow<Boolean> = _isPlayingAudio.asStateFlow()

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentOutputFile: File? = null
    private var timerJob: Job? = null
    private var recordingStartTimeMs: Long = 0L

    /**
     * Starts recording audio to a temporary file in cacheDir.
     */
    fun startRecording(): Boolean {
        try {
            stopPlayback()
            cleanupRecorder()

            val outputDir = File(context.cacheDir, "voice_reflections")
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            val audioFile = File(outputDir, "diary_voice_${System.currentTimeMillis()}.m4a")
            currentOutputFile = audioFile

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioEncodingBitRate(64000)
            recorder.setAudioSamplingRate(44100)
            recorder.setOutputFile(audioFile.absolutePath)

            recorder.prepare()
            recorder.start()
            mediaRecorder = recorder
            recordingStartTimeMs = System.currentTimeMillis()

            _recordingState.value = RecordingState.Recording(0)

            timerJob?.cancel()
            timerJob = coroutineScope.launch(Dispatchers.Main) {
                while (isActive && mediaRecorder != null) {
                    val elapsedSeconds = ((System.currentTimeMillis() - recordingStartTimeMs) / 1000).toInt()
                    val amplitude = try {
                        mediaRecorder?.maxAmplitude ?: 0
                    } catch (e: Exception) {
                        0
                    }
                    _recordingState.value = RecordingState.Recording(elapsedSeconds, amplitude)
                    delay(500)
                }
            }

            Log.i(TAG, "Audio recording started at: ${audioFile.absolutePath}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio recording", e)
            _recordingState.value = RecordingState.Error("Failed to start recording: ${e.localizedMessage ?: "Unknown error"}")
            cleanupRecorder()
            return false
        }
    }

    /**
     * Stops the active recording and returns the recorded File.
     */
    fun stopRecording(): File? {
        timerJob?.cancel()
        timerJob = null

        val recorder = mediaRecorder
        val file = currentOutputFile

        if (recorder == null || file == null) {
            _recordingState.value = RecordingState.Idle
            return null
        }

        return try {
            recorder.stop()
            recorder.release()
            mediaRecorder = null

            val totalDurationSeconds = ((System.currentTimeMillis() - recordingStartTimeMs) / 1000).toInt().coerceAtLeast(1)
            Log.i(TAG, "Recording stopped successfully. File size: ${file.length()} bytes, duration: $totalDurationSeconds s")

            if (file.exists() && file.length() > 0) {
                _recordingState.value = RecordingState.Completed(file, totalDurationSeconds)
                file
            } else {
                _recordingState.value = RecordingState.Error("Recorded audio file is empty")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping MediaRecorder", e)
            cleanupRecorder()
            _recordingState.value = RecordingState.Error("Failed to stop recording cleanly: ${e.message}")
            null
        }
    }

    /**
     * Cancels the current recording and deletes temporary file.
     */
    fun cancelRecording() {
        timerJob?.cancel()
        timerJob = null
        cleanupRecorder()
        currentOutputFile?.let {
            if (it.exists()) it.delete()
        }
        currentOutputFile = null
        _recordingState.value = RecordingState.Idle
    }

    /**
     * Resets recording state back to Idle.
     */
    fun resetState() {
        stopPlayback()
        _recordingState.value = RecordingState.Idle
    }

    /**
     * Plays the recorded audio file for user review.
     */
    fun playRecording(onCompleted: () -> Unit = {}) {
        val file = currentOutputFile ?: return
        if (!file.exists()) return

        try {
            stopPlayback()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                setOnCompletionListener {
                    _isPlayingAudio.value = false
                    onCompleted()
                }
                start()
            }
            _isPlayingAudio.value = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play recorded audio", e)
            _isPlayingAudio.value = false
        }
    }

    /**
     * Stops preview playback.
     */
    fun stopPlayback() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping media player", e)
        } finally {
            mediaPlayer = null
            _isPlayingAudio.value = false
        }
    }

    private fun cleanupRecorder() {
        try {
            mediaRecorder?.reset()
            mediaRecorder?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing mediaRecorder", e)
        } finally {
            mediaRecorder = null
        }
    }
}
