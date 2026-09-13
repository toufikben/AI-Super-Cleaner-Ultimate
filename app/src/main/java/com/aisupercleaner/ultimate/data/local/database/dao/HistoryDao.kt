package com.aisupercleaner.ultimate.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.aisupercleaner.ultimate.data.local.database.entity.CleanupHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM cleanup_history ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<CleanupHistoryEntity>>

    @Query("SELECT * FROM cleanup_history ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<CleanupHistoryEntity>>

    @Insert
    suspend fun insert(entity: CleanupHistoryEntity): Long

    @Query("DELETE FROM cleanup_history")
    suspend fun clearAll()

    @Query("SELECT SUM(freedBytes) FROM cleanup_history WHERE timestamp >= :since")
    suspend fun sumFreedSince(since: Long): Long?

    @Query("SELECT * FROM cleanup_history WHERE timestamp >= :since ORDER BY timestamp ASC")
    suspend fun entriesSince(since: Long): List<CleanupHistoryEntity>
}
