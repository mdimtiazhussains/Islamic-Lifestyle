package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diary_entries")
data class DiaryEntry(
    @PrimaryKey
    val id: String,
    val date: String,          // YYYY-MM-DD
    val time: String,          // HH:mm
    val title: String,
    val content: String,
    val mood: String = "Calm", // Inspired, Grateful, Calm, Reflective, Energized, Tired, Focused
    val energy: Int = 3,       // 1 to 5
    val tags: String = "",     // Comma-separated or space-separated e.g. "#mindset, #clarity"
    val gratitudeNotes: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
)
