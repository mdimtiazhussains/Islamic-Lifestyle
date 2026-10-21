package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.BayanClipEntity
import kotlinx.coroutines.flow.Flow

/**
 * BayanDao
 *
 * Data Access Object for non-repeating shuffle engine and local persistence of bayan clips.
 */
@Dao
interface BayanDao {

    @Query("SELECT * FROM bayan_clips ORDER BY id ASC")
    fun getAllClips(): Flow<List<BayanClipEntity>>

    @Query("SELECT * FROM bayan_clips WHERE isPlayed = 0")
    suspend fun getUnplayedClips(): List<BayanClipEntity>

    @Query("SELECT * FROM bayan_clips WHERE id = :id LIMIT 1")
    suspend fun getClipById(id: String): BayanClipEntity?

    @Query("SELECT COUNT(*) FROM bayan_clips")
    suspend fun getClipCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertClips(clips: List<BayanClipEntity>)

    @Query("UPDATE bayan_clips SET isPlayed = :isPlayed WHERE id = :id")
    suspend fun markClipPlayed(id: String, isPlayed: Boolean = true)

    @Query("UPDATE bayan_clips SET isPlayed = 0, rotationCycle = rotationCycle + 1")
    suspend fun resetAllClips()

    @Query("SELECT MAX(rotationCycle) FROM bayan_clips")
    suspend fun getCurrentRotationCycle(): Int?
}
