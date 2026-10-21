package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * BayanClipEntity
 *
 * Local persistence table `bayan_clips` storing authentic short Islamic Bayan reminders
 * sourced from https://www.nooresunnat.com/shortwhatsappbayan.php.
 */
@Entity(tableName = "bayan_clips")
data class BayanClipEntity(
    @PrimaryKey
    val id: String,
    val title: String, // Roman Urdu title, e.g. "Waaldain Ka Adab o Ehtiram"
    val audioUrl: String, // Direct media url from nooresunnat.com
    val durationSec: Int,
    val isPlayed: Boolean = false,
    val rotationCycle: Int = 1,
    val speaker: String = "Maulana Mehboob Elahi Sahab",
    val category: String = "Short WhatsApp Bayan"
)
