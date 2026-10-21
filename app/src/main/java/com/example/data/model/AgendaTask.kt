package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "agenda_tasks")
data class AgendaTask(
    @PrimaryKey
    val id: String,
    val date: String,          // YYYY-MM-DD
    val timeSlot: String,      // HH:mm (e.g. "09:00")
    val timeEnd: String = "",  // HH:mm (e.g. "10:30")
    val title: String,
    val description: String = "",
    val category: String = "Work",   // Work, Health, Personal, Deep Work, Learning
    val status: String = "pending",  // pending, completed, deferred
    val priority: String = "Medium", // High, Medium, Low
    val orderIndex: Int = 0,
    val createdAt: String = "",
    val updatedAt: String = ""
) {
    val isCompleted: Boolean get() = status.equals("completed", ignoreCase = true)
}
