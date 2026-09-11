package com.aisupercleaner.ultimate.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

data class StorageAggregate(val fileCount: Int, val totalBytes: Long)

@Dao
interface StorageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFiles(files: List<FileMetadataEntity>)

    @Query("DELETE FROM file_metadata")
    suspend fun clearFiles()

    @Query("SELECT COUNT(*) FROM file_metadata")
    fun observeFileCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(sizeBytes), 0) FROM file_metadata")
    fun observeTotalBytes(): Flow<Long>

    @Query("SELECT COUNT(*) FROM file_metadata WHERE mediaType = :mediaType")
    fun observeCountByType(mediaType: String): Flow<Int>

    @Query("SELECT COUNT(*) AS fileCount, COALESCE(SUM(sizeBytes), 0) AS totalBytes FROM file_metadata WHERE mediaType = :mediaType AND sizeBytes >= :minimumBytes")
    suspend fun aggregateLargeFiles(mediaType: String, minimumBytes: Long): StorageAggregate

    @Query("SELECT COUNT(*) AS fileCount, COALESCE(SUM(sizeBytes), 0) AS totalBytes FROM file_metadata WHERE mediaType = 'image' AND (LOWER(displayName) LIKE '%screenshot%' OR LOWER(displayName) LIKE '%screen_shot%' OR LOWER(displayName) LIKE '%screen-shot%' OR LOWER(displayName) LIKE '%screen shot%')")
    suspend fun aggregateScreenshots(): StorageAggregate

    @Insert
    suspend fun insertScanHistory(history: ScanHistoryEntity)

    @Query("SELECT * FROM scan_history ORDER BY completedAtEpochMillis DESC LIMIT 1")
    suspend fun latestScan(): ScanHistoryEntity?
}

@Database(entities = [FileMetadataEntity::class, ScanHistoryEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun storageDao(): StorageDao

    companion object {
        @Volatile private var instance: AppDatabase? = null
        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "storage_intelligence.db").build().also { instance = it }
        }
    }
}
