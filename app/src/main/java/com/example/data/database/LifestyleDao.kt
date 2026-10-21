package com.example.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AgendaTask
import com.example.data.model.DailyPrayerRecord
import com.example.data.model.DiaryEntry
import com.example.data.model.MorningPlan
import kotlinx.coroutines.flow.Flow

@Dao
interface LifestyleDao {

    // --- Agenda Tasks ---
    @Query("SELECT * FROM agenda_tasks WHERE date = :date ORDER BY timeSlot ASC, orderIndex ASC")
    fun getTasksForDate(date: String): Flow<List<AgendaTask>>

    @Query("SELECT * FROM agenda_tasks ORDER BY date DESC, timeSlot ASC")
    fun getAllTasks(): Flow<List<AgendaTask>>

    @Query("SELECT DISTINCT date FROM agenda_tasks")
    fun getDatesWithTasks(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: AgendaTask)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<AgendaTask>)

    @Update
    suspend fun updateTask(task: AgendaTask)

    @Delete
    suspend fun deleteTask(task: AgendaTask)

    @Query("DELETE FROM agenda_tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: String)

    @Query("DELETE FROM agenda_tasks WHERE date = :date")
    suspend fun deleteTasksForDate(date: String)

    // --- Diary Entries ---
    @Query("SELECT * FROM diary_entries WHERE date = :date ORDER BY time DESC")
    fun getDiaryEntriesForDate(date: String): Flow<List<DiaryEntry>>

    @Query("SELECT * FROM diary_entries ORDER BY date DESC, time DESC")
    fun getAllDiaryEntries(): Flow<List<DiaryEntry>>

    @Query("SELECT DISTINCT date FROM diary_entries")
    fun getDatesWithDiaryEntries(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiaryEntry(entry: DiaryEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiaryEntries(entries: List<DiaryEntry>)

    @Update
    suspend fun updateDiaryEntry(entry: DiaryEntry)

    @Delete
    suspend fun deleteDiaryEntry(entry: DiaryEntry)

    @Query("DELETE FROM diary_entries WHERE id = :entryId")
    suspend fun deleteDiaryEntryById(entryId: String)

    @Query("DELETE FROM diary_entries WHERE id LIKE 'diary_tmpl_%'")
    suspend fun deleteTemplateDiaryEntries()

    // --- Morning Plans ---
    @Query("SELECT * FROM morning_plans WHERE date = :date LIMIT 1")
    fun getMorningPlanForDate(date: String): Flow<MorningPlan?>

    @Query("SELECT * FROM morning_plans WHERE date = :date LIMIT 1")
    suspend fun getMorningPlanSync(date: String): MorningPlan?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMorningPlan(plan: MorningPlan)

    @Update
    suspend fun updateMorningPlan(plan: MorningPlan)

    @Query("UPDATE morning_plans SET waterGlasses = :glasses WHERE date = :date")
    suspend fun updateWaterGlasses(date: String, glasses: Int)

    // --- Daily Prayer Records ---
    @Query("SELECT * FROM daily_prayer_records WHERE date = :date LIMIT 1")
    fun getPrayerRecordForDate(date: String): Flow<DailyPrayerRecord?>

    @Query("SELECT * FROM daily_prayer_records WHERE date = :date LIMIT 1")
    suspend fun getPrayerRecordSync(date: String): DailyPrayerRecord?

    @Query("SELECT * FROM daily_prayer_records ORDER BY date DESC")
    fun getAllPrayerRecords(): Flow<List<DailyPrayerRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrayerRecord(record: DailyPrayerRecord)

    @Update
    suspend fun updatePrayerRecord(record: DailyPrayerRecord)

    @Delete
    suspend fun deletePrayerRecord(record: DailyPrayerRecord)
}
