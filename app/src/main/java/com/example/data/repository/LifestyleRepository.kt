package com.example.data.repository

import com.example.data.database.LifestyleDao
import com.example.data.model.AgendaTask
import com.example.data.model.DailyPrayerRecord
import com.example.data.model.DiaryEntry
import com.example.data.model.MorningPlan
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class SyncSummary(
    val tasksSynced: Int = 0,
    val diaryEntriesSynced: Int = 0,
    val plansSynced: Int = 0
) {
    val totalSynced: Int get() = tasksSynced + diaryEntriesSynced + plansSynced
}

class LifestyleRepository(
    private val dao: LifestyleDao,
    private val firestoreService: com.example.data.firestore.FirestoreSyncService? = null,
    val moneyRepository: MoneyRepository? = null
) {
    private var tasksListenerRegistration: ListenerRegistration? = null
    private var diaryListenerRegistration: ListenerRegistration? = null

    suspend fun syncAllFromCloud(): SyncSummary {
        if (firestoreService == null) return SyncSummary(0, 0, 0)
        val remoteTasks = firestoreService.fetchAllTasks()
        if (remoteTasks.isNotEmpty()) {
            dao.insertTasks(remoteTasks)
        }
        val remoteDiaryEntries = firestoreService.fetchAllDiaryEntries()
        if (remoteDiaryEntries.isNotEmpty()) {
            dao.insertDiaryEntries(remoteDiaryEntries)
        }
        val remotePlans = firestoreService.fetchAllMorningPlans()
        for (plan in remotePlans) {
            dao.insertMorningPlan(plan)
        }
        return SyncSummary(remoteTasks.size, remoteDiaryEntries.size, remotePlans.size)
    }

    fun startRealtimeCloudSync(scope: CoroutineScope) {
        if (firestoreService == null) return
        stopRealtimeCloudSync()
        tasksListenerRegistration = firestoreService.startTasksSnapshotListener { remoteTasks ->
            if (remoteTasks.isNotEmpty()) {
                scope.launch {
                    dao.insertTasks(remoteTasks)
                }
            }
        }
        diaryListenerRegistration = firestoreService.startDiarySnapshotListener { remoteEntries ->
            if (remoteEntries.isNotEmpty()) {
                scope.launch {
                    dao.insertDiaryEntries(remoteEntries)
                }
            }
        }
    }

    fun stopRealtimeCloudSync() {
        tasksListenerRegistration?.remove()
        tasksListenerRegistration = null
        diaryListenerRegistration?.remove()
        diaryListenerRegistration = null
    }

    fun getTasksForDate(date: String): Flow<List<AgendaTask>> = dao.getTasksForDate(date)

    fun getAllTasks(): Flow<List<AgendaTask>> = dao.getAllTasks()

    fun getDatesWithTasks(): Flow<List<String>> = dao.getDatesWithTasks()

    fun getDiaryEntriesForDate(date: String): Flow<List<DiaryEntry>> = dao.getDiaryEntriesForDate(date)

    fun getAllDiaryEntries(): Flow<List<DiaryEntry>> = dao.getAllDiaryEntries()

    fun getDatesWithDiaryEntries(): Flow<List<String>> = dao.getDatesWithDiaryEntries()

    fun getMorningPlanForDate(date: String): Flow<MorningPlan?> = dao.getMorningPlanForDate(date)

    suspend fun getMorningPlanSync(date: String): MorningPlan? = dao.getMorningPlanSync(date)

    // --- Daily Prayer Records ---
    fun getPrayerRecordForDate(date: String): Flow<DailyPrayerRecord?> = dao.getPrayerRecordForDate(date)

    suspend fun getPrayerRecordSync(date: String): DailyPrayerRecord? = dao.getPrayerRecordSync(date)

    suspend fun savePrayerRecord(record: DailyPrayerRecord) {
        dao.insertPrayerRecord(record)
        firestoreService?.savePrayerRecord(record)
    }

    suspend fun insertTask(task: AgendaTask) {
        dao.insertTask(task)
        firestoreService?.saveTask(task)
        syncDailyProgressToCloud(task.date)
    }

    suspend fun updateTask(task: AgendaTask) {
        dao.updateTask(task)
        firestoreService?.saveTask(task)
        syncDailyProgressToCloud(task.date)
    }

    suspend fun deleteTask(task: AgendaTask) {
        dao.deleteTask(task)
        firestoreService?.deleteTask(task.id)
        syncDailyProgressToCloud(task.date)
    }

    suspend fun deleteTaskById(id: String, date: String = "") {
        dao.deleteTaskById(id)
        firestoreService?.deleteTask(id)
        if (date.isNotBlank()) syncDailyProgressToCloud(date)
    }

    suspend fun toggleTaskCompletion(task: AgendaTask) {
        val newStatus = if (task.isCompleted) "pending" else "completed"
        val updated = task.copy(
            status = newStatus,
            updatedAt = currentIsoTimestamp()
        )
        dao.updateTask(updated)
        firestoreService?.saveTask(updated)
        syncDailyProgressToCloud(task.date)
    }

    suspend fun insertDiaryEntry(entry: DiaryEntry): Boolean {
        dao.insertDiaryEntry(entry)
        val cloudSaved = firestoreService?.saveDiaryEntry(entry) ?: false
        return cloudSaved
    }

    suspend fun updateDiaryEntry(entry: DiaryEntry): Boolean {
        dao.updateDiaryEntry(entry)
        val cloudSaved = firestoreService?.saveDiaryEntry(entry) ?: false
        return cloudSaved
    }

    suspend fun syncRemoteDiaryEntriesForDate(date: String): Int {
        if (firestoreService == null) return 0
        val remoteEntries = firestoreService.fetchDiaryEntriesForDate(date)
        for (entry in remoteEntries) {
            dao.insertDiaryEntry(entry)
        }
        return remoteEntries.size
    }

    suspend fun deleteDiaryEntry(entry: DiaryEntry) {
        dao.deleteDiaryEntry(entry)
        firestoreService?.deleteDiaryEntry(entry.id)
    }

    suspend fun deleteDiaryEntryById(id: String) {
        dao.deleteDiaryEntryById(id)
        firestoreService?.deleteDiaryEntry(id)
    }

    suspend fun saveMorningPlan(plan: MorningPlan) {
        dao.insertMorningPlan(plan)
        firestoreService?.saveMorningPlan(plan)
        syncDailyProgressToCloud(plan.date)
    }

    suspend fun updateWaterGlasses(date: String, glasses: Int) {
        val existing = dao.getMorningPlanSync(date)
        if (existing != null) {
            dao.updateWaterGlasses(date, glasses)
            val updated = existing.copy(waterGlasses = glasses)
            firestoreService?.saveMorningPlan(updated)
        } else {
            val newPlan = MorningPlan(
                id = "plan_$date",
                date = date,
                waterGlasses = glasses,
                createdAt = currentIsoTimestamp(),
                updatedAt = currentIsoTimestamp()
            )
            dao.insertMorningPlan(newPlan)
            firestoreService?.saveMorningPlan(newPlan)
        }
        syncDailyProgressToCloud(date)
    }

    suspend fun syncDailyProgressToCloud(date: String) {
        if (firestoreService == null) return
        try {
            val morningPlan = dao.getMorningPlanSync(date)
            // Note: firestoreService.saveProgress will store progress record in Firestore
            firestoreService.saveProgress(
                date = date,
                completedTasks = 0, // will be updated reactively or from task list
                totalTasks = 0,
                waterGlasses = morningPlan?.waterGlasses ?: 0,
                morningRoutineDone = morningPlan?.completed == true
            )
        } catch (e: Exception) {
            // caught safely
        }
    }

    /**
     * Completes the 15-minute morning routine:
     * 1. Saves completed morning plan.
     * 2. Pushes the Top 3 focus goals as High-Priority Agenda Tasks into the 24-hour schedule.
     * 3. Logs a Morning Mindset reflection into the Diary.
     */
    suspend fun completeMorningRoutineAndPush(plan: MorningPlan) {
        val timestamp = currentIsoTimestamp()
        val completedPlan = plan.copy(
            completed = true,
            completedAt = timestamp,
            updatedAt = timestamp
        )
        dao.insertMorningPlan(completedPlan)

        // 1. Push Top 3 Non-Negotiables to Agenda
        val tasksToInsert = mutableListOf<AgendaTask>()
        val defaultSlots = listOf("09:00", "11:30", "14:30")
        val goals = listOf(plan.focusGoal1, plan.focusGoal2, plan.focusGoal3)

        goals.forEachIndexed { index, goal ->
            if (goal.isNotBlank()) {
                val timeSlot = defaultSlots.getOrElse(index) { "16:00" }
                val task = AgendaTask(
                    id = "task_morn_${plan.date}_${index + 1}_${System.currentTimeMillis() % 10000}",
                    date = plan.date,
                    timeSlot = timeSlot,
                    timeEnd = addHoursToTime(timeSlot, 1, 30),
                    title = "🎯 Top Priority #${index + 1}: ${goal.trim()}",
                    description = "Committed during 15m Morning Planning. Energy level: ${plan.energyLevel}.",
                    category = "Deep Work",
                    status = "pending",
                    priority = if (index == 0) "High" else "Medium",
                    orderIndex = index,
                    createdAt = timestamp,
                    updatedAt = timestamp
                )
                tasksToInsert.add(task)
            }
        }

        if (tasksToInsert.isNotEmpty()) {
            dao.insertTasks(tasksToInsert)
        }

        firestoreService?.saveMorningPlan(completedPlan)
        tasksToInsert.forEach { firestoreService?.saveTask(it) }
        syncDailyProgressToCloud(plan.date)
    }

    /**
     * Seeds sample high-performance template tasks for a given date
     */
    suspend fun seedTemplateDay(date: String) {
        val timestamp = currentIsoTimestamp()
        val templateTasks = listOf(
            AgendaTask(
                id = "task_tmpl_1_$date",
                date = date,
                timeSlot = "06:30",
                timeEnd = "07:15",
                title = "☀️ Morning Routine & 15m Planning",
                description = "Hydration, mindfulness, gratitude triad & daily prioritization.",
                category = "Health",
                status = "completed",
                priority = "High",
                orderIndex = 1,
                createdAt = timestamp,
                updatedAt = timestamp
            ),
            AgendaTask(
                id = "task_tmpl_2_$date",
                date = date,
                timeSlot = "07:30",
                timeEnd = "08:30",
                title = "🏃 Morning Aerobic Workout / Mobility",
                description = "45 mins dynamic cardio & flexibility training.",
                category = "Health",
                status = "completed",
                priority = "Medium",
                orderIndex = 2,
                createdAt = timestamp,
                updatedAt = timestamp
            ),
            AgendaTask(
                id = "task_tmpl_3_$date",
                date = date,
                timeSlot = "09:00",
                timeEnd = "11:30",
                title = "⚡ Deep Work Block #1: Core Deliverable",
                description = "No phone, full focus block on high-impact milestone project.",
                category = "Deep Work",
                status = "completed",
                priority = "High",
                orderIndex = 3,
                createdAt = timestamp,
                updatedAt = timestamp
            ),
            AgendaTask(
                id = "task_tmpl_4_$date",
                date = date,
                timeSlot = "12:00",
                timeEnd = "13:00",
                title = "🥗 Nourishing Lunch & Mindful Walk",
                description = "Nutritious meal, sunlight exposure, 20-min outdoor stroll.",
                category = "Health",
                status = "pending",
                priority = "Medium",
                orderIndex = 4,
                createdAt = timestamp,
                updatedAt = timestamp
            ),
            AgendaTask(
                id = "task_tmpl_5_$date",
                date = date,
                timeSlot = "13:30",
                timeEnd = "15:30",
                title = "📊 Architecture & Strategy Alignment",
                description = "System design review, roadmap synchronization, and reviews.",
                category = "Work",
                status = "pending",
                priority = "Medium",
                orderIndex = 5,
                createdAt = timestamp,
                updatedAt = timestamp
            ),
            AgendaTask(
                id = "task_tmpl_6_$date",
                date = date,
                timeSlot = "16:00",
                timeEnd = "17:00",
                title = "📚 Continuous Learning & Reading",
                description = "Read 2 chapters on distributed architectures and cognitive flow.",
                category = "Learning",
                status = "pending",
                priority = "Low",
                orderIndex = 6,
                createdAt = timestamp,
                updatedAt = timestamp
            ),
            AgendaTask(
                id = "task_tmpl_7_$date",
                date = date,
                timeSlot = "19:00",
                timeEnd = "20:30",
                title = "🍽️ Dinner & Family Quality Time",
                description = "Unplugged evening connection, dinner, and relaxing conversation.",
                category = "Personal",
                status = "pending",
                priority = "Medium",
                orderIndex = 7,
                createdAt = timestamp,
                updatedAt = timestamp
            ),
            AgendaTask(
                id = "task_tmpl_8_$date",
                date = date,
                timeSlot = "21:30",
                timeEnd = "22:15",
                title = "🌙 Evening Reflection & Wind-Down",
                description = "Review daily victories, set out tomorrow's clothes, screen-free reading.",
                category = "Personal",
                status = "pending",
                priority = "High",
                orderIndex = 8,
                createdAt = timestamp,
                updatedAt = timestamp
            )
        )
        dao.insertTasks(templateTasks)

        val templatePlan = MorningPlan(
            id = "plan_$date",
            date = date,
            completed = true,
            completedAt = timestamp,
            affirmation = "I approach each hour with intentionality, clarity, and calm focus.",
            gratitude1 = "Energy and mental clarity to tackle challenging problems.",
            gratitude2 = "The unwavering encouragement of my loved ones.",
            gratitude3 = "The quiet peaceful early morning coffee and headspace.",
            focusGoal1 = "Deliver the high-impact core milestone.",
            focusGoal2 = "Maintain consistent hydration and physical vitality.",
            focusGoal3 = "Read 2 chapters and complete evening reflection.",
            mindsetScore = 9,
            focusScore = 9,
            energyLevel = "Peak Flow",
            waterGlasses = 4,
            notes = "Feeling aligned and energized for the day.",
            createdAt = timestamp,
            updatedAt = timestamp
        )
        dao.insertMorningPlan(templatePlan)
        // Diary entries are intentionally never auto-seeded to keep Diary tab clean and empty
    }

    suspend fun clearTemplateDiaryEntries() {
        dao.deleteTemplateDiaryEntries()
    }

    private fun currentIsoTimestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
    }

    private fun addHoursToTime(time: String, addHours: Int, addMinutes: Int): String {
        return try {
            val parts = time.split(":")
            val h = parts[0].toInt()
            val m = parts[1].toInt()
            val totalM = (h * 60 + m) + (addHours * 60 + addMinutes)
            val newH = (totalM / 60) % 24
            val newM = totalM % 60
            String.format(Locale.US, "%02d:%02d", newH, newM)
        } catch (e: Exception) {
            ""
        }
    }
}
