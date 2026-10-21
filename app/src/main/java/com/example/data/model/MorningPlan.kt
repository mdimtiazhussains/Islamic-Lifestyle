package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "morning_plans")
data class MorningPlan(
    @PrimaryKey
    val id: String,                 // "plan_YYYY-MM-DD"
    val date: String,               // YYYY-MM-DD
    val completed: Boolean = false,
    val completedAt: String = "",
    val affirmation: String = "I approach each hour with intentionality, clarity, and calm focus.",
    val gratitude1: String = "",
    val gratitude2: String = "",
    val gratitude3: String = "",
    val focusGoal1: String = "",
    val focusGoal2: String = "",
    val focusGoal3: String = "",
    val mindsetScore: Int = 8,      // 1 to 10
    val focusScore: Int = 8,        // 1 to 10
    val energyLevel: String = "High", // Peak Flow, High, Balanced, Calm, Low
    val waterGlasses: Int = 0,      // 0 to 8
    val hydrationTarget: Int = 8,
    val notes: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
)
