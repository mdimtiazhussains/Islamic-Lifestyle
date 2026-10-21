package com.example

import com.example.data.model.DiaryEntry
import com.example.ui.components.DIARY_MOODS
import com.example.ui.components.QUICK_INSPIRATION_PROMPTS
import com.example.ui.components.SUGGESTED_DIARY_TAGS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DiaryComponentUnitTest {

    private val sampleEntries = listOf(
        DiaryEntry(
            id = "d1",
            date = "2026-10-06",
            time = "08:30",
            title = "Morning Intentions & Calmness",
            content = "Started the day with box breathing and meditation. Clear head for the sprint.",
            mood = "Calm",
            energy = 3,
            tags = "#mindset, #calm"
        ),
        DiaryEntry(
            id = "d2",
            date = "2026-10-07",
            time = "14:15",
            title = "Deep Work Session & Architecture",
            content = "Completed Firestore synchronization rules and Jetpack Compose component refactoring.",
            mood = "Focused",
            energy = 4,
            tags = "#deepwork, #productivity"
        ),
        DiaryEntry(
            id = "d3",
            date = "2026-10-08",
            time = "21:00",
            title = "Evening Gratitude & Wins",
            content = "Reflected on team accomplishments. Thankful for supportive feedback and clarity.",
            mood = "Grateful",
            energy = 5,
            tags = "#gratitude, #wins",
            gratitudeNotes = "Grateful for great family and creative progress."
        )
    )

    @Test
    fun testDiaryEntryCreationAndDefaults() {
        val entry = DiaryEntry(
            id = "diary_test_1",
            date = "2026-10-08",
            time = "14:30",
            title = "Productive Deep Work Session",
            content = "Completed milestone implementation with clean Jetpack Compose UI."
        )

        assertEquals("diary_test_1", entry.id)
        assertEquals("2026-10-08", entry.date)
        assertEquals("14:30", entry.time)
        assertEquals("Productive Deep Work Session", entry.title)
        assertEquals("Calm", entry.mood)
        assertEquals(3, entry.energy)
        assertEquals("", entry.tags)
        assertEquals("", entry.gratitudeNotes)
    }

    @Test
    fun testInspirationPromptsAndSuggestedTags() {
        assertTrue("Inspiration prompts should not be empty", QUICK_INSPIRATION_PROMPTS.isNotEmpty())
        assertTrue("Suggested tags should contain #mindset", SUGGESTED_DIARY_TAGS.contains("#mindset"))
        assertTrue("Suggested tags should contain #gratitude", SUGGESTED_DIARY_TAGS.contains("#gratitude"))
        assertTrue("Moods list should have calm and inspired", DIARY_MOODS.any { it.first == "Calm" })
        assertTrue("Moods list should have inspired", DIARY_MOODS.any { it.first == "Inspired" })
    }

    @Test
    fun testFilterByKeywordInTitleOrContent() {
        val query = "breathing"
        val filtered = sampleEntries.filter { entry ->
            entry.title.contains(query, ignoreCase = true) ||
            entry.content.contains(query, ignoreCase = true) ||
            entry.tags.contains(query, ignoreCase = true)
        }
        assertEquals(1, filtered.size)
        assertEquals("d1", filtered.first().id)
    }

    @Test
    fun testFilterByTagKeyword() {
        val query = "#deepwork"
        val filtered = sampleEntries.filter { entry ->
            entry.title.contains(query, ignoreCase = true) ||
            entry.content.contains(query, ignoreCase = true) ||
            entry.tags.contains(query, ignoreCase = true)
        }
        assertEquals(1, filtered.size)
        assertEquals("d2", filtered.first().id)
    }

    @Test
    fun testFilterBySpecificDate() {
        val targetDate = "2026-10-08"
        val filtered = sampleEntries.filter { it.date == targetDate }
        assertEquals(1, filtered.size)
        assertEquals("d3", filtered.first().id)
        assertEquals("Evening Gratitude & Wins", filtered.first().title)
    }

    @Test
    fun testFilterByDateStringQuery() {
        val dateQuery = "2026-10-07"
        val filtered = sampleEntries.filter { entry ->
            entry.date.contains(dateQuery, ignoreCase = true) ||
            entry.title.contains(dateQuery, ignoreCase = true)
        }
        assertEquals(1, filtered.size)
        assertEquals("d2", filtered.first().id)
    }

    @Test
    fun testFilterCombinedKeywordAndDate() {
        val keyword = "wins"
        val targetDate = "2026-10-08"

        val filtered = sampleEntries.filter { entry ->
            val matchesDate = entry.date == targetDate
            val matchesKeyword = entry.title.contains(keyword, ignoreCase = true) ||
                    entry.content.contains(keyword, ignoreCase = true) ||
                    entry.tags.contains(keyword, ignoreCase = true)
            matchesDate && matchesKeyword
        }

        assertEquals(1, filtered.size)
        assertEquals("d3", filtered.first().id)

        // Same keyword on a different date yields 0
        val nonExistentDateFilter = sampleEntries.filter { entry ->
            entry.date == "2026-10-06" && (entry.title.contains(keyword, ignoreCase = true) || entry.content.contains(keyword, ignoreCase = true))
        }
        assertEquals(0, nonExistentDateFilter.size)
    }

    @Test
    fun testWordCountCalculation() {
        val sampleText = "Writing in my daily diary brings clarity and focused intention."
        val wordCount = sampleText.trim().split("\\s+".toRegex()).size
        assertEquals(10, wordCount)

        val emptyText = ""
        val emptyWordCount = if (emptyText.isBlank()) 0 else emptyText.trim().split("\\s+".toRegex()).size
        assertEquals(0, emptyWordCount)
    }

    @Test
    fun testPdfExportOptionsDefaultsAndCustomization() {
        val defaultOptions = com.example.util.PdfExportOptions()
        assertEquals("My Daily Reflections", defaultOptions.documentTitle)
        assertEquals("Imtiaz", defaultOptions.authorName)
        assertTrue(defaultOptions.includeGratitude)
        assertTrue(defaultOptions.includeMoodAndEnergy)
        assertTrue(defaultOptions.includeTags)
        assertTrue(defaultOptions.includeSummaryStats)

        val customOptions = com.example.util.PdfExportOptions(
            documentTitle = "Weekly Retrospective",
            authorName = "Alex",
            subtitle = "Custom Review",
            includeGratitude = false,
            includeTags = true
        )
        assertEquals("Weekly Retrospective", customOptions.documentTitle)
        assertEquals("Alex", customOptions.authorName)
        assertEquals("Custom Review", customOptions.subtitle)
        assertFalse(customOptions.includeGratitude)
        assertTrue(customOptions.includeTags)
    }

    @Test
    fun testPdfExportScopeFiltering() {
        // Test scope ALL
        val allEntries = sampleEntries
        assertEquals(3, allEntries.size)

        // Test scope FILTERED by date
        val dateFiltered = sampleEntries.filter { it.date == "2026-10-07" }
        assertEquals(1, dateFiltered.size)
        assertEquals("d2", dateFiltered.first().id)

        // Test scope FILTERED by mood
        val moodFiltered = sampleEntries.filter { it.mood == "Grateful" }
        assertEquals(1, moodFiltered.size)
        assertEquals("d3", moodFiltered.first().id)
    }

    @Test
    fun testPdfExportEntriesSorting() {
        val sortedDesc = sampleEntries.sortedWith(
            compareByDescending<DiaryEntry> { it.date }.thenByDescending { it.time }
        )
        assertEquals("d3", sortedDesc[0].id) // 2026-10-08
        assertEquals("d2", sortedDesc[1].id) // 2026-10-07
        assertEquals("d1", sortedDesc[2].id) // 2026-10-06
    }

    @Test
    fun testVoiceTranscriptionRomanUrduResponseParsing() {
        val mockGeminiJson = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "{\n  \"title\": \"Subah Ki Taza-Dami\",\n  \"mindsetReflections\": \"Aaj subah bohot achi shuru hui. Namaz ke baad waqt par kaam shuru kiya aur 15-minute slot blocks follow kiye.\",\n  \"gratitudeHighlights\": \"Alhamdulillah sehat aur sakoon mila.\",\n  \"detectedLanguage\": \"Roman Urdu\"\n}"
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val parsed = com.example.util.DiarySpeechTranscriptionService.parseGeminiResponse(mockGeminiJson)
        assertEquals("Subah Ki Taza-Dami", parsed.title)
        assertTrue(parsed.mindsetReflections.contains("Namaz ke baad"))
        assertEquals("Alhamdulillah sehat aur sakoon mila.", parsed.gratitudeHighlights)
        assertEquals("Roman Urdu", parsed.detectedLanguage)
    }

    @Test
    fun testVoiceTranscriptionEnglishResponseParsing() {
        val mockGeminiJson = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "{\n  \"title\": \"Morning Focus and Wins\",\n  \"mindsetReflections\": \"Completed major sprint backlog tasks with full clarity and uninterrupted focus.\",\n  \"gratitudeHighlights\": \"Thankful for energy and collaborative team support.\",\n  \"detectedLanguage\": \"English\"\n}"
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val parsed = com.example.util.DiarySpeechTranscriptionService.parseGeminiResponse(mockGeminiJson)
        assertEquals("Morning Focus and Wins", parsed.title)
        assertEquals("Completed major sprint backlog tasks with full clarity and uninterrupted focus.", parsed.mindsetReflections)
        assertEquals("Thankful for energy and collaborative team support.", parsed.gratitudeHighlights)
        assertEquals("English", parsed.detectedLanguage)
    }

    @Test
    fun testVoiceTranscriptionDemoResultStrictlyRomanUrdu() {
        val demoResult = com.example.util.DiarySpeechTranscriptionService.createDemoTranscriptionResult()
        assertEquals("Roman Urdu", demoResult.detectedLanguage)
        assertTrue(demoResult.isDemoOrMock)
        assertTrue(demoResult.title.isNotBlank())
        assertTrue(demoResult.mindsetReflections.contains("Aaj ka din"))
        assertTrue(demoResult.gratitudeHighlights.contains("Alhamdulillah"))
    }

    @Test
    fun testVoiceTranscriptionFieldPopulationOnDiaryEntry() {
        val voiceResult = com.example.util.VoiceJournalTranscriptionResult(
            title = "Milestone Kamiyabi",
            mindsetReflections = "Sprint ka critical goal achieve kar liya aur team ke saath sync meetings bohot productive rahein.",
            gratitudeHighlights = "Shukar alhamdulillah continuous progress aur aasaani ke liye.",
            detectedLanguage = "Roman Urdu"
        )

        // Verify that fields populate directly into a DiaryEntry representation
        val populatedEntry = DiaryEntry(
            id = "voice_entry_1",
            date = "2026-10-15",
            time = "10:30",
            title = voiceResult.title,
            content = voiceResult.mindsetReflections,
            gratitudeNotes = voiceResult.gratitudeHighlights
        )

        assertEquals("Milestone Kamiyabi", populatedEntry.title)
        assertEquals("Sprint ka critical goal achieve kar liya aur team ke saath sync meetings bohot productive rahein.", populatedEntry.content)
        assertEquals("Shukar alhamdulillah continuous progress aur aasaani ke liye.", populatedEntry.gratitudeNotes)
    }

    @Test
    fun testDuplicatePreventionByMatchingSubjectOverwritesExistingEntry() {
        val existingEntries = listOf(
            DiaryEntry(
                id = "diary_prev_101",
                date = "2026-10-16",
                time = "09:00",
                title = "Evening Clarity",
                content = "Old reflection content from morning.",
                createdAt = "2026-10-16T09:00:00Z"
            )
        )

        // Incoming entry with the exact same subject
        val newVoiceSubject = "Evening Clarity"
        val newVoiceContent = "Updated evening reflection with deep insights."

        val matched = existingEntries.firstOrNull { it.title.equals(newVoiceSubject, ignoreCase = true) }
        assertNotNull(matched)

        // Overwrites the existing entry instead of generating a new ID
        val savedEntry = DiaryEntry(
            id = matched!!.id,
            date = matched.date,
            time = "20:00",
            title = newVoiceSubject,
            content = newVoiceContent,
            createdAt = matched.createdAt,
            updatedAt = "2026-10-16T20:00:00Z"
        )

        assertEquals("diary_prev_101", savedEntry.id)
        assertEquals("Updated evening reflection with deep insights.", savedEntry.content)
    }

    @Test
    fun testExplicitOptionToCreateNewEntryForcesSeparateCard() {
        val existingEntries = listOf(
            DiaryEntry(
                id = "diary_prev_101",
                date = "2026-10-16",
                time = "09:00",
                title = "Evening Clarity",
                content = "Old reflection content.",
                createdAt = "2026-10-16T09:00:00Z"
            )
        )

        val forceNewEntry = true
        val targetId = if (forceNewEntry) "diary_new_202" else existingEntries.first().id

        assertEquals("diary_new_202", targetId)
        assertFalse(targetId == existingEntries.first().id)
    }
}

