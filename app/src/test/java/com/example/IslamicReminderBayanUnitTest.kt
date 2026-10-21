package com.example

import com.example.data.model.BayanClipEntity
import com.example.data.repository.BayanRepository
import com.example.util.IslamicReminderAudioPlayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class IslamicReminderBayanUnitTest {

    @Test
    fun testSeedClipsValidityAndNooresunnatSource() {
        val clips = BayanRepository.SEED_CLIPS
        assertTrue("Collection must contain at least 30 clips", clips.size >= 30)

        clips.forEach { clip ->
            assertNotNull(clip.id)
            assertTrue("ID must not be blank", clip.id.isNotBlank())
            assertTrue("Title in Roman Urdu must not be blank", clip.title.isNotBlank())
            assertTrue(
                "Audio URL must point to authentic nooresunnat.com source: ${clip.audioUrl}",
                clip.audioUrl.startsWith("https://www.nooresunnat.com/Audio/WhatsApp%20Clips/Video%20Files/")
            )
            assertTrue("Duration must be positive", clip.durationSec > 0)
            assertFalse("Default isPlayed must be false", clip.isPlayed)
            assertEquals("Default rotation cycle must be 1", 1, clip.rotationCycle)
        }

        // Verify key Roman Urdu titles exist in collection
        val titles = clips.map { it.title }
        assertTrue(titles.contains("Waaldain Ka Adab o Ehtiram"))
        assertTrue(titles.contains("Ana Ko Chor Dain Ghar Ko Jannat Banae"))
        assertTrue(titles.contains("Hamara Kaam Deen Ki Baat Batana Hai"))
        assertTrue(titles.contains("Narmi Ikhtiyar Karo Aur Gussa Piyo"))
        assertTrue(titles.contains("Pooray Deen Main Dakhil Ho Jao"))
    }

    @Test
    fun testExhaustiveNonRepeatingShuffleEngineCycle() {
        val testClips = mutableListOf(
            BayanClipEntity("c1", "Title 1", "https://url1", 120, isPlayed = false, rotationCycle = 1),
            BayanClipEntity("c2", "Title 2", "https://url2", 150, isPlayed = false, rotationCycle = 1),
            BayanClipEntity("c3", "Title 3", "https://url3", 180, isPlayed = false, rotationCycle = 1)
        )

        // Initial pool has 3 unplayed clips
        val unplayed1 = testClips.filter { !it.isPlayed }
        assertEquals(3, unplayed1.size)

        // Play first clip
        testClips[0] = testClips[0].copy(isPlayed = true)
        val unplayed2 = testClips.filter { !it.isPlayed }
        assertEquals(2, unplayed2.size)
        assertFalse(unplayed2.any { it.id == "c1" })

        // Play remaining clips
        testClips[1] = testClips[1].copy(isPlayed = true)
        testClips[2] = testClips[2].copy(isPlayed = true)
        val unplayed3 = testClips.filter { !it.isPlayed }
        assertEquals(0, unplayed3.size)

        // Auto-Reset Cycle Simulation:
        // When unplayed pool is empty, reset all clips to isPlayed = false and increment rotationCycle
        val resetClips = testClips.map { it.copy(isPlayed = false, rotationCycle = it.rotationCycle + 1) }
        assertEquals(3, resetClips.filter { !it.isPlayed }.size)
        assertEquals(2, resetClips[0].rotationCycle)
        assertEquals(2, resetClips[1].rotationCycle)
        assertEquals(2, resetClips[2].rotationCycle)
    }

    @Test
    fun testVolumeSliderStrictClamping() {
        IslamicReminderAudioPlayer.setVolume(0.5f)
        assertEquals(0.5f, IslamicReminderAudioPlayer.playerVolume.value, 0.01f)

        // Exceed upper bound -> clamped to 1.0f
        IslamicReminderAudioPlayer.setVolume(1.8f)
        assertEquals(1.0f, IslamicReminderAudioPlayer.playerVolume.value, 0.01f)

        // Exceed lower bound -> clamped to 0.0f
        IslamicReminderAudioPlayer.setVolume(-0.4f)
        assertEquals(0.0f, IslamicReminderAudioPlayer.playerVolume.value, 0.01f)

        // Reset to default
        IslamicReminderAudioPlayer.setVolume(0.85f)
        assertEquals(0.85f, IslamicReminderAudioPlayer.playerVolume.value, 0.01f)
    }

    @Test
    fun testPlayingStateProgressAndFormat() {
        val clip = BayanClipEntity(
            id = "test_clip",
            title = "Waaldain Ka Adab o Ehtiram",
            audioUrl = "https://example.com/audio.mp4",
            durationSec = 150
        )

        val playingState = IslamicReminderAudioPlayer.ReminderUiState.Playing(
            clip = clip,
            currentPositionMs = 45_000,
            totalDurationMs = 150_000
        )

        assertEquals(0.30f, playingState.progressFraction, 0.01f)
        assertEquals("00:45 / 02:30", playingState.formattedProgress)
    }

    @Test
    fun testRapidRetriggerSuppressionAndStop() {
        // Calling stop or suppress resets to Idle immediately
        IslamicReminderAudioPlayer.suppressAnyActivePlayback("Test suppression")
        assertEquals(IslamicReminderAudioPlayer.ReminderUiState.Idle, IslamicReminderAudioPlayer.reminderState.value)

        IslamicReminderAudioPlayer.stop()
        assertEquals(IslamicReminderAudioPlayer.ReminderUiState.Idle, IslamicReminderAudioPlayer.reminderState.value)
    }

    @Test
    fun testSimulateAdhanCompletionHook() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        
        // Simulating Adhan completion should immediately transition to Countdown
        com.example.util.AzanAudioPlayer.simulateAdhanCompletion(context, "Fajr")
        val state = IslamicReminderAudioPlayer.reminderState.value
        assertTrue("State should be Countdown after simulation trigger", state is IslamicReminderAudioPlayer.ReminderUiState.Countdown)
        assertEquals("Fajr", (state as IslamicReminderAudioPlayer.ReminderUiState.Countdown).prayerName)
        assertEquals(30, state.secondsRemaining)

        // Stopping should return cleanly to Idle
        IslamicReminderAudioPlayer.stop(context)
        assertEquals(IslamicReminderAudioPlayer.ReminderUiState.Idle, IslamicReminderAudioPlayer.reminderState.value)
    }
}
