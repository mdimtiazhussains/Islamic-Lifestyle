package com.example

import com.example.data.model.AgendaTask
import com.example.data.model.DiaryEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiaryDeduplicationUnitTest {

    private val existingEntries = listOf(
        DiaryEntry(
            id = "diary_101",
            date = "2026-10-18",
            time = "09:00",
            title = "Pur-Aman Sham Aur Wazahat",
            content = "Aaj ka din bohot behtareen raha. Subah time par Fajr ada ki.",
            mood = "Calm",
            energy = 4,
            tags = "#clarity"
        ),
        DiaryEntry(
            id = "diary_102",
            date = "2026-10-18",
            time = "14:30",
            title = "Afternoon Architecture Breakthrough",
            content = "Completed the core Firestore repository interface and local Room migrations.",
            mood = "Focused",
            energy = 5,
            tags = "#deepwork"
        )
    )

    /**
     * Replicates the intelligent deduplication matching algorithm used in DailyDiaryInputComponent
     * and DiaryDialog:
     * - Matches by explicit selected ID
     * - Matches by non-generic exact/case-insensitive title
     * - Matches by content equivalence or significant prefix (>= 25 characters)
     */
    private fun findMatchingEntry(
        title: String,
        content: String,
        selectedId: String? = null,
        forceNew: Boolean = false,
        entries: List<DiaryEntry> = existingEntries
    ): DiaryEntry? {
        if (forceNew) return null
        if (selectedId != null) {
            return entries.firstOrNull { it.id == selectedId }
        }

        val trimmedTitle = title.trim()
        val trimmedContent = content.trim()

        return entries.firstOrNull { existing ->
            val isGenericTitle = trimmedTitle.equals("Daily Mindset Reflection", ignoreCase = true) ||
                    trimmedTitle.equals("Mindset Reflection", ignoreCase = true) ||
                    trimmedTitle.equals("Daily Reflection", ignoreCase = true) ||
                    trimmedTitle.equals("Voice Reflection", ignoreCase = true)

            val titleMatches = trimmedTitle.isNotBlank() && !isGenericTitle &&
                    existing.title.trim().equals(trimmedTitle, ignoreCase = true)

            val contentMatches = trimmedContent.isNotBlank() &&
                    (existing.content.trim().equals(trimmedContent, ignoreCase = true) ||
                            (trimmedContent.length >= 25 && existing.content.trim().contains(trimmedContent.take(35))))

            titleMatches || contentMatches
        }
    }

    @Test
    fun voiceJournalWithSameTitle_matchesExistingEntryForOverwrite() {
        // Voice transcription returns the same subject title
        val match = findMatchingEntry(
            title = "Pur-Aman Sham Aur Wazahat",
            content = "Different content transcribed from voice"
        )

        assertNotNull(match)
        assertEquals("diary_101", match?.id)
    }

    @Test
    fun voiceJournalWithSameContent_matchesExistingEntryForOverwrite() {
        // Voice transcription returns a generic title but matching content prefix
        val match = findMatchingEntry(
            title = "Daily Mindset Reflection", // Generic title should not trigger false match
            content = "Completed the core Firestore repository interface and local Room migrations."
        )

        assertNotNull(match)
        assertEquals("diary_102", match?.id)
    }

    @Test
    fun forceNewEntry_overridesDeduplicationAndCreatesNew() {
        val match = findMatchingEntry(
            title = "Pur-Aman Sham Aur Wazahat",
            content = "Aaj ka din bohot behtareen raha.",
            forceNew = true
        )

        assertNull("Forcing new entry should bypass deduplication match", match)
    }

    @Test
    fun completelyDifferentEntry_doesNotMatchExisting() {
        val match = findMatchingEntry(
            title = "Evening Reflections on Quran",
            content = "Listened to Surah Yasin on the live radio and felt profound tranquility."
        )

        assertNull("Unique subject and content should not match existing entries", match)
    }

    @Test
    fun agenda24hTimeline_slotNormalizationAnd1HourBlock() {
        fun normalizeTimeSlot(slot: String): String {
            val parts = slot.trim().split(":")
            if (parts.size == 2) {
                val h = parts[0].toIntOrNull() ?: 0
                val m = parts[1].toIntOrNull() ?: 0
                return String.format(java.util.Locale.US, "%02d:%02d", h.coerceIn(0, 23), m.coerceIn(0, 59))
            }
            return slot
        }

        fun isOneHourTask(task: AgendaTask): Boolean {
            if (task.timeEnd.isBlank()) return false
            val startParts = task.timeSlot.split(":")
            val endParts = task.timeEnd.split(":")
            if (startParts.size == 2 && endParts.size == 2) {
                val startH = startParts[0].toIntOrNull() ?: return false
                val startM = startParts[1].toIntOrNull() ?: return false
                val endH = endParts[0].toIntOrNull() ?: return false
                val endM = endParts[1].toIntOrNull() ?: return false
                val startMinutes = startH * 60 + startM
                val endMinutes = endH * 60 + endM
                val duration = if (endMinutes >= startMinutes) endMinutes - startMinutes else (endMinutes + 1440) - startMinutes
                return duration in 45..75
            }
            return false
        }

        assertEquals("09:00", normalizeTimeSlot("9:00"))
        assertEquals("14:15", normalizeTimeSlot("14:15"))

        val task1Hour = AgendaTask(
            id = "t1",
            date = "2026-10-18",
            title = "Deep Work Block",
            timeSlot = "09:00",
            timeEnd = "10:00",
            category = "Deep Work"
        )
        assertTrue(isOneHourTask(task1Hour))

        val task15m = AgendaTask(
            id = "t2",
            date = "2026-10-18",
            title = "Quick Standup",
            timeSlot = "10:00",
            timeEnd = "10:15",
            category = "Work"
        )
        assertFalse(isOneHourTask(task15m))
    }
}
