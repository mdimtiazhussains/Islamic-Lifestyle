package com.example.data.model

import com.google.firebase.Timestamp

data class UserProgress(
    val id: String = "",
    val userId: String = "",
    val date: String = "",
    val completedTasks: Int = 0,
    val totalTasks: Int = 0,
    val waterGlasses: Int = 0,
    val morningRoutineDone: Boolean = false,
    val completionRate: Double = 0.0,
    val updatedAt: Timestamp? = null
)
