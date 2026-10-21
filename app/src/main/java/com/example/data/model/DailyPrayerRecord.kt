package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * DailyPrayerRecord
 *
 * Entity representing the 5 daily prayers (Fajr, Dhuhr, Asr, Maghrib, Isha)
 * synchronized with the official Abu Dhabi (Awqaf UAE) schedule,
 * automated Azan playback settings,
 * Zikr (fixed counts for 33 SubhanAllah, 33 Alhamdulillah, 34 Allahu Akbar,
 * Durood-e-Sharif, 100 La ilaha illallah, and 100 Astaghfirullah),
 * and Tilawat portion and counters (Pages, Juz, Duration).
 */
@Entity(tableName = "daily_prayer_records")
data class DailyPrayerRecord(
    @PrimaryKey
    val date: String, // Format: YYYY-MM-DD

    // --- Prayer Completion Status ---
    val fajrCompleted: Boolean = false,
    val dhuhrCompleted: Boolean = false,
    val asrCompleted: Boolean = false,
    val maghribCompleted: Boolean = false,
    val ishaCompleted: Boolean = false,

    // --- Live Prayer Timings (Official Abu Dhabi UAE Awqaf Schedule) ---
    val fajrTime: String = "04:53",
    val dhuhrTime: String = "12:18",
    val asrTime: String = "15:43",
    val maghribTime: String = "18:15",
    val ishaTime: String = "19:45",

    // --- Automated Azan Playback Toggles ---
    val fajrAzanEnabled: Boolean = true,
    val dhuhrAzanEnabled: Boolean = true,
    val asrAzanEnabled: Boolean = true,
    val maghribAzanEnabled: Boolean = true,
    val ishaAzanEnabled: Boolean = true,

    // --- Zikr & Remembrance (Fixed Counts & Sacred Presets) ---
    val subhanAllahCount: Int = 33,
    val alhamdulillahCount: Int = 33,
    val allahuAkbarCount: Int = 34,
    val duroodSharifCount: Int = 0,
    val laIlahaIllallahCount: Int = 100,
    val astaghfirullahCount: Int = 100,
    val customZikrCount: Int = 0,
    val customZikrLabel: String = "Durood-e-Sharif",
    val morningAdhkarDone: Boolean = false,
    val eveningAdhkarDone: Boolean = false,

    // --- Tilawat (Quran Recitation - Portions & Counters) ---
    val tilawatCompleted: Boolean = false,
    val tilawatSurah: String = "Surah Al-Mulk",
    val tilawatJuz: Int = 1,
    val tilawatPages: Int = 4,
    val tilawatDurationMinutes: Int = 15,
    val tilawatNotes: String = "",

    val createdAt: String = "",
    val updatedAt: String = ""
)
