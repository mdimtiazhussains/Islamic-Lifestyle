package com.example

import com.example.data.model.DailyPrayerRecord
import com.example.ui.components.FIVE_PRAYERS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyPrayerTrackerUnitTest {

    @Test
    fun testFivePrayersDefinitionAndAbuDhabiSchedule() {
        assertEquals("Should have exactly 5 daily prayers", 5, FIVE_PRAYERS.size)
        val prayerIds = FIVE_PRAYERS.map { it.id }
        assertTrue("Contains fajr", prayerIds.contains("fajr"))
        assertTrue("Contains dhuhr", prayerIds.contains("dhuhr"))
        assertTrue("Contains asr", prayerIds.contains("asr"))
        assertTrue("Contains maghrib", prayerIds.contains("maghrib"))
        assertTrue("Contains isha", prayerIds.contains("isha"))

        assertEquals("Fajr", FIVE_PRAYERS[0].nameEnglish)
        assertEquals("فجر", FIVE_PRAYERS[0].nameArabic)
        assertEquals("04:53", FIVE_PRAYERS[0].officialAbuDhabiTime)

        assertEquals("Dhuhr", FIVE_PRAYERS[1].nameEnglish)
        assertEquals("12:18", FIVE_PRAYERS[1].officialAbuDhabiTime)

        assertEquals("Asr", FIVE_PRAYERS[2].nameEnglish)
        assertEquals("15:43", FIVE_PRAYERS[2].officialAbuDhabiTime)

        assertEquals("Maghrib", FIVE_PRAYERS[3].nameEnglish)
        assertEquals("18:15", FIVE_PRAYERS[3].officialAbuDhabiTime)

        assertEquals("Isha", FIVE_PRAYERS[4].nameEnglish)
        assertEquals("19:45", FIVE_PRAYERS[4].officialAbuDhabiTime)
    }

    @Test
    fun testDailyPrayerRecordDefaultsAndAbuDhabiTimings() {
        val record = DailyPrayerRecord(date = "2026-10-09")

        assertEquals("2026-10-09", record.date)
        assertFalse(record.fajrCompleted)
        assertFalse(record.dhuhrCompleted)
        assertFalse(record.asrCompleted)
        assertFalse(record.maghribCompleted)
        assertFalse(record.ishaCompleted)

        // Live Abu Dhabi official Awqaf schedule timings
        assertEquals("04:53", record.fajrTime)
        assertEquals("12:18", record.dhuhrTime)
        assertEquals("15:43", record.asrTime)
        assertEquals("18:15", record.maghribTime)
        assertEquals("19:45", record.ishaTime)

        // Automated Azan playback defaults to enabled
        assertTrue(record.fajrAzanEnabled)
        assertTrue(record.dhuhrAzanEnabled)
        assertTrue(record.asrAzanEnabled)
        assertTrue(record.maghribAzanEnabled)
        assertTrue(record.ishaAzanEnabled)

        // Fixed Zikr defaults
        assertEquals(33, record.subhanAllahCount)
        assertEquals(33, record.alhamdulillahCount)
        assertEquals(34, record.allahuAkbarCount)
        assertEquals(0, record.duroodSharifCount)
        assertEquals(100, record.laIlahaIllallahCount)
        assertEquals(100, record.astaghfirullahCount)
    }

    @Test
    fun testPrayerCompletionAndAzanToggles() {
        val record = DailyPrayerRecord(
            date = "2026-10-09",
            fajrCompleted = true,
            dhuhrCompleted = true,
            asrCompleted = true,
            maghribCompleted = false,
            ishaCompleted = false,
            fajrAzanEnabled = true,
            dhuhrAzanEnabled = false, // muted
            asrAzanEnabled = true,
            maghribAzanEnabled = true,
            ishaAzanEnabled = true
        )

        assertTrue(record.fajrCompleted)
        assertTrue(record.dhuhrCompleted)
        assertTrue(record.asrCompleted)
        assertFalse(record.maghribCompleted)
        assertFalse(record.ishaCompleted)

        assertTrue(record.fajrAzanEnabled)
        assertFalse(record.dhuhrAzanEnabled)
        assertTrue(record.asrAzanEnabled)

        val completedCount = listOf(
            record.fajrCompleted,
            record.dhuhrCompleted,
            record.asrCompleted,
            record.maghribCompleted,
            record.ishaCompleted
        ).count { it }
        assertEquals(3, completedCount)
    }

    @Test
    fun testZikrFixedCountsAndRemembrance() {
        val record = DailyPrayerRecord(
            date = "2026-10-09",
            subhanAllahCount = 33,
            alhamdulillahCount = 33,
            allahuAkbarCount = 34,
            duroodSharifCount = 50,
            laIlahaIllallahCount = 100,
            astaghfirullahCount = 100,
            morningAdhkarDone = true,
            eveningAdhkarDone = false
        )

        assertEquals(33, record.subhanAllahCount)
        assertEquals(33, record.alhamdulillahCount)
        assertEquals(34, record.allahuAkbarCount)
        assertEquals(50, record.duroodSharifCount)
        assertEquals(100, record.laIlahaIllallahCount)
        assertEquals(100, record.astaghfirullahCount)
        assertTrue(record.morningAdhkarDone)
        assertFalse(record.eveningAdhkarDone)
    }

    @Test
    fun testQuranPortionAndCounters() {
        val record = DailyPrayerRecord(
            date = "2026-10-09",
            tilawatCompleted = true,
            tilawatSurah = "Surah Al-Kahf",
            tilawatJuz = 15,
            tilawatPages = 10,
            tilawatDurationMinutes = 25
        )

        assertTrue(record.tilawatCompleted)
        assertEquals("Surah Al-Kahf", record.tilawatSurah)
        assertEquals(15, record.tilawatJuz)
        assertEquals(10, record.tilawatPages)
        assertEquals(25, record.tilawatDurationMinutes)
    }

    @Test
    fun testLivePrayerTimesResultModel() {
        val liveResult = com.example.data.service.LivePrayerTimesResult(
            fajr = "05:01",
            dhuhr = "12:12",
            asr = "15:31",
            maghrib = "18:04",
            isha = "19:17",
            cityName = "Abu Dhabi",
            countryName = "United Arab Emirates",
            calculationMethod = "dubai (Awqaf UAE)",
            latitude = 24.4539,
            longitude = 54.3773,
            fetchedAtFormatted = "12:15, Oct 10"
        )

        assertEquals("05:01", liveResult.fajr)
        assertEquals("12:12", liveResult.dhuhr)
        assertEquals("15:31", liveResult.asr)
        assertEquals("18:04", liveResult.maghrib)
        assertEquals("19:17", liveResult.isha)
        assertEquals("Abu Dhabi", liveResult.cityName)
        assertEquals("United Arab Emirates", liveResult.countryName)
        assertEquals(24.4539, liveResult.latitude, 0.001)
        assertEquals(54.3773, liveResult.longitude, 0.001)
    }

    @Test
    fun testLocationPrayerServiceAbuDhabiCoordinates() {
        assertEquals(24.4539, com.example.data.service.LocationPrayerService.ABU_DHABI_LAT, 0.0001)
        assertEquals(54.3773, com.example.data.service.LocationPrayerService.ABU_DHABI_LNG, 0.0001)
    }

    @Test
    fun testWeeklyTimelineCurrentWeekCalculation() {
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val selectedDateStr = "2026-10-10" // Saturday

        val cal = java.util.Calendar.getInstance(java.util.Locale.US)
        cal.firstDayOfWeek = java.util.Calendar.SUNDAY
        cal.time = dateFormat.parse(selectedDateStr) ?: java.util.Date()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)

        val dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK)
        val daysSinceSunday = (dayOfWeek - java.util.Calendar.SUNDAY + 7) % 7
        cal.add(java.util.Calendar.DAY_OF_MONTH, -daysSinceSunday)

        val expectedDates = listOf(
            "2026-10-04", // Sunday
            "2026-10-05", // Monday
            "2026-10-06", // Tuesday
            "2026-10-07", // Wednesday
            "2026-10-08", // Thursday
            "2026-10-09", // Friday
            "2026-10-10"  // Saturday (Current day)
        )

        for (expected in expectedDates) {
            val dStr = dateFormat.format(cal.time)
            assertEquals(expected, dStr)
            cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
    }

    @Test
    fun testUaeAzanPlayerConfig() {
        assertEquals("azan_prayer_channel", com.example.util.AzanAudioPlayer.NOTIFICATION_CHANNEL_ID)
        assertEquals(1001, com.example.util.AzanAudioPlayer.NOTIFICATION_ID)
    }

    @Test
    fun testAzanNotificationReceiverActions() {
        assertEquals("com.example.service.action.STOP_AZAN_NOTIF", com.example.service.AzanNotificationReceiver.ACTION_STOP_AZAN)
        assertEquals("com.example.service.action.DISMISS_AZAN_NOTIF", com.example.service.AzanNotificationReceiver.ACTION_DISMISS_AZAN)
    }

    @Test
    fun testAzanAudioPlayerInitialState() {
        assertFalse(com.example.util.AzanAudioPlayer.isPlaying.value)
        assertEquals(null, com.example.util.AzanAudioPlayer.activePrayer.value)
        assertEquals(0f, com.example.util.AzanAudioPlayer.progressFraction.value, 0.001f)
    }
}
