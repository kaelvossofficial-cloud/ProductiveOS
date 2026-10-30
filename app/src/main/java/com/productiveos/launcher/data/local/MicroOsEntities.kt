package com.productiveos.launcher.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "clips")
data class ClipEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val timestamp: Long
)

@Entity(tableName = "micro_logs")
data class MicroLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val thought: String,
    val timestamp: Long
)

@Entity(tableName = "path_check_logs")
data class PathCheckLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val appName: String,
    val intentReason: String, // "Boredom", "Need Info", "Habit"
    val proceeded: Boolean,
    val timestamp: Long
)

@Dao
interface MicroOsDao {

    // Quantum Clip (Clipboard Vault)
    @Query("SELECT * FROM clips ORDER BY timestamp DESC LIMIT 50")
    fun observeRecentClips(): Flow<List<ClipEntity>>

    @Query("DELETE FROM clips WHERE content = :content")
    suspend fun deleteDuplicateClip(content: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClip(clip: ClipEntity)

    @Query("DELETE FROM clips WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteClipsOlderThan(cutoffTimestamp: Long)

    @Query("SELECT id FROM clips ORDER BY timestamp DESC LIMIT 50")
    suspend fun getTop50ClipIds(): List<Long>

    @Query("DELETE FROM clips WHERE id NOT IN (:keepIds)")
    suspend fun deleteClipsExcept(keepIds: List<Long>)

    @Query("DELETE FROM clips WHERE id = :id")
    suspend fun deleteClipById(id: Long)

    @Query("DELETE FROM clips")
    suspend fun clearAllClips()

    // Micro-Log (Second-Brain Capture)
    @Query("SELECT * FROM micro_logs ORDER BY timestamp DESC LIMIT 100")
    fun observeMicroLogs(): Flow<List<MicroLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMicroLog(log: MicroLogEntity)

    @Query("DELETE FROM micro_logs WHERE id = :id")
    suspend fun deleteMicroLogById(id: Long)

    // PathCheck (Mindful Awareness Analytics)
    @Query("SELECT * FROM path_check_logs WHERE timestamp >= :sinceTimestamp ORDER BY timestamp DESC")
    fun observePathCheckLogsSince(sinceTimestamp: Long): Flow<List<PathCheckLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPathCheckLog(entry: PathCheckLogEntity)
}

