package com.example.data.firestore

import android.content.Context
import com.example.R
import com.example.data.model.AgendaTask
import com.example.data.model.DailyPrayerRecord
import com.example.data.model.DiaryEntry
import com.example.data.model.MorningPlan
import com.example.data.model.UserProgress
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class FirestoreSyncService(private val context: Context) {

    // MANDATORY: Always initialize using the named database ID from firebase_applet_config.xml
    private val firestore: FirebaseFirestore by lazy {
        val dbId = context.getString(R.string.firestore_database_id)
        FirebaseFirestore.getInstance(dbId)
    }

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    val currentUserId: String? get() = auth.currentUser?.uid

    private fun requireUserId(): String {
        return currentUserId ?: throw IllegalStateException("User must be signed in with Google to access Cloud Firestore.")
    }

    suspend fun saveTask(task: AgendaTask) {
        val uid = currentUserId ?: return
        val docRef = firestore.collection("users").document(uid).collection("tasks").document(task.id)
        val payload = mapOf(
            "id" to task.id,
            "userId" to uid,
            "date" to task.date,
            "timeSlot" to task.timeSlot,
            "timeEnd" to task.timeEnd,
            "title" to task.title,
            "description" to task.description,
            "category" to task.category,
            "status" to task.status,
            "priority" to task.priority,
            "orderIndex" to task.orderIndex,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload, SetOptions.merge()).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
        }
    }

    suspend fun fetchAllTasks(): List<AgendaTask> {
        val uid = currentUserId ?: return emptyList()
        val colRef = firestore.collection("users").document(uid).collection("tasks")
        return try {
            val snapshot = colRef.get().await()
            snapshot.documents.mapNotNull { doc ->
                val id = doc.getString("id") ?: doc.id
                val date = doc.getString("date") ?: return@mapNotNull null
                val timeSlot = doc.getString("timeSlot") ?: "09:00"
                val timeEnd = doc.getString("timeEnd") ?: ""
                val title = doc.getString("title") ?: return@mapNotNull null
                val description = doc.getString("description") ?: ""
                val category = doc.getString("category") ?: "Work"
                val status = doc.getString("status") ?: "pending"
                val priority = doc.getString("priority") ?: "Medium"
                val orderIndex = doc.getLong("orderIndex")?.toInt() ?: 0
                AgendaTask(
                    id = id,
                    date = date,
                    timeSlot = timeSlot,
                    timeEnd = timeEnd,
                    title = title,
                    description = description,
                    category = category,
                    status = status,
                    priority = priority,
                    orderIndex = orderIndex
                )
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, colRef.path)
            emptyList()
        }
    }

    fun startTasksSnapshotListener(onChange: (List<AgendaTask>) -> Unit): ListenerRegistration? {
        val uid = currentUserId ?: return null
        val colRef = firestore.collection("users").document(uid).collection("tasks")
        return colRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LISTEN, colRef.path)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    val id = doc.getString("id") ?: doc.id
                    val date = doc.getString("date") ?: return@mapNotNull null
                    val timeSlot = doc.getString("timeSlot") ?: "09:00"
                    val timeEnd = doc.getString("timeEnd") ?: ""
                    val title = doc.getString("title") ?: return@mapNotNull null
                    val description = doc.getString("description") ?: ""
                    val category = doc.getString("category") ?: "Work"
                    val status = doc.getString("status") ?: "pending"
                    val priority = doc.getString("priority") ?: "Medium"
                    val orderIndex = doc.getLong("orderIndex")?.toInt() ?: 0
                    AgendaTask(
                        id = id,
                        date = date,
                        timeSlot = timeSlot,
                        timeEnd = timeEnd,
                        title = title,
                        description = description,
                        category = category,
                        status = status,
                        priority = priority,
                        orderIndex = orderIndex
                    )
                }
                onChange(list)
            }
        }
    }

    suspend fun deleteTask(taskId: String) {
        val uid = currentUserId ?: return
        val docRef = firestore.collection("users").document(uid).collection("tasks").document(taskId)
        try {
            docRef.delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
        }
    }

    suspend fun saveDiaryEntry(entry: DiaryEntry): Boolean {
        val uid = currentUserId ?: return false
        val docRef = firestore.collection("users").document(uid).collection("diary_entries").document(entry.id)
        val payload = mapOf(
            "id" to entry.id,
            "userId" to uid,
            "date" to entry.date,
            "time" to entry.time,
            "title" to entry.title,
            "content" to entry.content,
            "mood" to entry.mood,
            "energy" to entry.energy,
            "tags" to entry.tags,
            "gratitudeNotes" to entry.gratitudeNotes,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload, SetOptions.merge()).await()
            true
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            false
        }
    }

    suspend fun fetchDiaryEntriesForDate(date: String): List<DiaryEntry> {
        val uid = currentUserId ?: return emptyList()
        return try {
            val querySnapshot = firestore.collection("users").document(uid)
                .collection("diary_entries")
                .whereEqualTo("date", date)
                .get()
                .await()
            querySnapshot.documents.mapNotNull { doc ->
                val id = doc.getString("id") ?: doc.id
                val d = doc.getString("date") ?: date
                val time = doc.getString("time") ?: "12:00"
                val title = doc.getString("title") ?: ""
                val content = doc.getString("content") ?: ""
                val mood = doc.getString("mood") ?: "Calm"
                val energy = doc.getLong("energy")?.toInt() ?: 3
                val tags = doc.getString("tags") ?: ""
                val gratitude = doc.getString("gratitudeNotes") ?: ""
                DiaryEntry(
                    id = id,
                    date = d,
                    time = time,
                    title = title,
                    content = content,
                    mood = mood,
                    energy = energy,
                    tags = tags,
                    gratitudeNotes = gratitude
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun fetchAllDiaryEntries(): List<DiaryEntry> {
        val uid = currentUserId ?: return emptyList()
        val colRef = firestore.collection("users").document(uid).collection("diary_entries")
        return try {
            val snapshot = colRef.get().await()
            snapshot.documents.mapNotNull { doc ->
                val id = doc.getString("id") ?: doc.id
                val d = doc.getString("date") ?: return@mapNotNull null
                val time = doc.getString("time") ?: "12:00"
                val title = doc.getString("title") ?: ""
                val content = doc.getString("content") ?: return@mapNotNull null
                val mood = doc.getString("mood") ?: "Calm"
                val energy = doc.getLong("energy")?.toInt() ?: 3
                val tags = doc.getString("tags") ?: ""
                val gratitude = doc.getString("gratitudeNotes") ?: ""
                DiaryEntry(
                    id = id,
                    date = d,
                    time = time,
                    title = title,
                    content = content,
                    mood = mood,
                    energy = energy,
                    tags = tags,
                    gratitudeNotes = gratitude
                )
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, colRef.path)
            emptyList()
        }
    }

    fun startDiarySnapshotListener(onChange: (List<DiaryEntry>) -> Unit): ListenerRegistration? {
        val uid = currentUserId ?: return null
        val colRef = firestore.collection("users").document(uid).collection("diary_entries")
        return colRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LISTEN, colRef.path)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.documents.mapNotNull { doc ->
                    val id = doc.getString("id") ?: doc.id
                    val d = doc.getString("date") ?: return@mapNotNull null
                    val time = doc.getString("time") ?: "12:00"
                    val title = doc.getString("title") ?: ""
                    val content = doc.getString("content") ?: return@mapNotNull null
                    val mood = doc.getString("mood") ?: "Calm"
                    val energy = doc.getLong("energy")?.toInt() ?: 3
                    val tags = doc.getString("tags") ?: ""
                    val gratitude = doc.getString("gratitudeNotes") ?: ""
                    DiaryEntry(
                        id = id,
                        date = d,
                        time = time,
                        title = title,
                        content = content,
                        mood = mood,
                        energy = energy,
                        tags = tags,
                        gratitudeNotes = gratitude
                    )
                }
                onChange(list)
            }
        }
    }

    suspend fun deleteDiaryEntry(entryId: String) {
        val uid = currentUserId ?: return
        val docRef = firestore.collection("users").document(uid).collection("diary_entries").document(entryId)
        try {
            docRef.delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
        }
    }

    suspend fun saveMorningPlan(plan: MorningPlan) {
        val uid = currentUserId ?: return
        val docRef = firestore.collection("users").document(uid).collection("morning_plans").document(plan.id)
        val payload = mapOf(
            "id" to plan.id,
            "userId" to uid,
            "date" to plan.date,
            "completed" to plan.completed,
            "completedAt" to plan.completedAt,
            "affirmation" to plan.affirmation,
            "gratitude1" to plan.gratitude1,
            "gratitude2" to plan.gratitude2,
            "gratitude3" to plan.gratitude3,
            "focusGoal1" to plan.focusGoal1,
            "focusGoal2" to plan.focusGoal2,
            "focusGoal3" to plan.focusGoal3,
            "mindsetScore" to plan.mindsetScore,
            "focusScore" to plan.focusScore,
            "energyLevel" to plan.energyLevel,
            "waterGlasses" to plan.waterGlasses,
            "hydrationTarget" to plan.hydrationTarget,
            "notes" to plan.notes,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload, SetOptions.merge()).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
        }
    }

    suspend fun fetchAllMorningPlans(): List<MorningPlan> {
        val uid = currentUserId ?: return emptyList()
        val colRef = firestore.collection("users").document(uid).collection("morning_plans")
        return try {
            val snapshot = colRef.get().await()
            snapshot.documents.mapNotNull { doc ->
                val id = doc.getString("id") ?: doc.id
                val date = doc.getString("date") ?: return@mapNotNull null
                val completed = doc.getBoolean("completed") ?: false
                val completedAt = doc.getString("completedAt") ?: ""
                val affirmation = doc.getString("affirmation") ?: ""
                val gratitude1 = doc.getString("gratitude1") ?: ""
                val gratitude2 = doc.getString("gratitude2") ?: ""
                val gratitude3 = doc.getString("gratitude3") ?: ""
                val focusGoal1 = doc.getString("focusGoal1") ?: ""
                val focusGoal2 = doc.getString("focusGoal2") ?: ""
                val focusGoal3 = doc.getString("focusGoal3") ?: ""
                val mindsetScore = doc.getLong("mindsetScore")?.toInt() ?: 8
                val focusScore = doc.getLong("focusScore")?.toInt() ?: 8
                val energyLevel = doc.getString("energyLevel") ?: "Optimal"
                val waterGlasses = doc.getLong("waterGlasses")?.toInt() ?: 0
                val hydrationTarget = doc.getLong("hydrationTarget")?.toInt() ?: 8
                val notes = doc.getString("notes") ?: ""
                MorningPlan(
                    id = id,
                    date = date,
                    completed = completed,
                    completedAt = completedAt,
                    affirmation = affirmation,
                    gratitude1 = gratitude1,
                    gratitude2 = gratitude2,
                    gratitude3 = gratitude3,
                    focusGoal1 = focusGoal1,
                    focusGoal2 = focusGoal2,
                    focusGoal3 = focusGoal3,
                    mindsetScore = mindsetScore,
                    focusScore = focusScore,
                    energyLevel = energyLevel,
                    waterGlasses = waterGlasses,
                    hydrationTarget = hydrationTarget,
                    notes = notes
                )
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, colRef.path)
            emptyList()
        }
    }

    suspend fun saveProgress(
        date: String,
        completedTasks: Int,
        totalTasks: Int,
        waterGlasses: Int,
        morningRoutineDone: Boolean
    ) {
        val uid = currentUserId ?: return
        val docRef = firestore.collection("users").document(uid).collection("progress").document("progress_$date")
        val rate = if (totalTasks > 0) completedTasks.toDouble() / totalTasks.toDouble() else 0.0
        val payload = mapOf(
            "id" to "progress_$date",
            "userId" to uid,
            "date" to date,
            "completedTasks" to completedTasks,
            "totalTasks" to totalTasks,
            "waterGlasses" to waterGlasses,
            "morningRoutineDone" to morningRoutineDone,
            "completionRate" to rate,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload, SetOptions.merge()).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
        }
    }

    suspend fun savePrayerRecord(record: DailyPrayerRecord) {
        val uid = currentUserId ?: return
        val docRef = firestore.collection("users").document(uid).collection("prayer_records").document(record.date)
        val payload = mapOf(
            "date" to record.date,
            "userId" to uid,
            "fajrCompleted" to record.fajrCompleted,
            "dhuhrCompleted" to record.dhuhrCompleted,
            "asrCompleted" to record.asrCompleted,
            "maghribCompleted" to record.maghribCompleted,
            "ishaCompleted" to record.ishaCompleted,
            "fajrTime" to record.fajrTime,
            "dhuhrTime" to record.dhuhrTime,
            "asrTime" to record.asrTime,
            "maghribTime" to record.maghribTime,
            "ishaTime" to record.ishaTime,
            "fajrAzanEnabled" to record.fajrAzanEnabled,
            "dhuhrAzanEnabled" to record.dhuhrAzanEnabled,
            "asrAzanEnabled" to record.asrAzanEnabled,
            "maghribAzanEnabled" to record.maghribAzanEnabled,
            "ishaAzanEnabled" to record.ishaAzanEnabled,
            "subhanAllahCount" to record.subhanAllahCount,
            "alhamdulillahCount" to record.alhamdulillahCount,
            "allahuAkbarCount" to record.allahuAkbarCount,
            "duroodSharifCount" to record.duroodSharifCount,
            "laIlahaIllallahCount" to record.laIlahaIllallahCount,
            "astaghfirullahCount" to record.astaghfirullahCount,
            "customZikrCount" to record.customZikrCount,
            "customZikrLabel" to record.customZikrLabel,
            "morningAdhkarDone" to record.morningAdhkarDone,
            "eveningAdhkarDone" to record.eveningAdhkarDone,
            "tilawatCompleted" to record.tilawatCompleted,
            "tilawatSurah" to record.tilawatSurah,
            "tilawatJuz" to record.tilawatJuz,
            "tilawatPages" to record.tilawatPages,
            "tilawatDurationMinutes" to record.tilawatDurationMinutes,
            "tilawatNotes" to record.tilawatNotes,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload, SetOptions.merge()).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
        }
    }

    suspend fun fetchPrayerRecord(date: String): DailyPrayerRecord? {
        val uid = currentUserId ?: return null
        val docRef = firestore.collection("users").document(uid).collection("prayer_records").document(date)
        return try {
            val doc = docRef.get().await()
            if (!doc.exists()) return null
            DailyPrayerRecord(
                date = doc.getString("date") ?: date,
                fajrCompleted = doc.getBoolean("fajrCompleted") ?: false,
                dhuhrCompleted = doc.getBoolean("dhuhrCompleted") ?: false,
                asrCompleted = doc.getBoolean("asrCompleted") ?: false,
                maghribCompleted = doc.getBoolean("maghribCompleted") ?: false,
                ishaCompleted = doc.getBoolean("ishaCompleted") ?: false,
                fajrTime = doc.getString("fajrTime") ?: "04:53",
                dhuhrTime = doc.getString("dhuhrTime") ?: "12:18",
                asrTime = doc.getString("asrTime") ?: "15:43",
                maghribTime = doc.getString("maghribTime") ?: "18:15",
                ishaTime = doc.getString("ishaTime") ?: "19:45",
                fajrAzanEnabled = doc.getBoolean("fajrAzanEnabled") ?: true,
                dhuhrAzanEnabled = doc.getBoolean("dhuhrAzanEnabled") ?: true,
                asrAzanEnabled = doc.getBoolean("asrAzanEnabled") ?: true,
                maghribAzanEnabled = doc.getBoolean("maghribAzanEnabled") ?: true,
                ishaAzanEnabled = doc.getBoolean("ishaAzanEnabled") ?: true,
                subhanAllahCount = doc.getLong("subhanAllahCount")?.toInt() ?: 33,
                alhamdulillahCount = doc.getLong("alhamdulillahCount")?.toInt() ?: 33,
                allahuAkbarCount = doc.getLong("allahuAkbarCount")?.toInt() ?: 34,
                duroodSharifCount = doc.getLong("duroodSharifCount")?.toInt() ?: 0,
                laIlahaIllallahCount = doc.getLong("laIlahaIllallahCount")?.toInt() ?: 100,
                astaghfirullahCount = doc.getLong("astaghfirullahCount")?.toInt() ?: 100,
                customZikrCount = doc.getLong("customZikrCount")?.toInt() ?: 0,
                customZikrLabel = doc.getString("customZikrLabel") ?: "Durood-e-Sharif",
                morningAdhkarDone = doc.getBoolean("morningAdhkarDone") ?: false,
                eveningAdhkarDone = doc.getBoolean("eveningAdhkarDone") ?: false,
                tilawatCompleted = doc.getBoolean("tilawatCompleted") ?: false,
                tilawatSurah = doc.getString("tilawatSurah") ?: "Surah Al-Mulk",
                tilawatJuz = doc.getLong("tilawatJuz")?.toInt() ?: 1,
                tilawatPages = doc.getLong("tilawatPages")?.toInt() ?: 4,
                tilawatDurationMinutes = doc.getLong("tilawatDurationMinutes")?.toInt() ?: 15,
                tilawatNotes = doc.getString("tilawatNotes") ?: ""
            )
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            null
        }
    }
}
