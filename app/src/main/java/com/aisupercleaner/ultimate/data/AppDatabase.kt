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

    @Query("UPDATE file_metadata SET lastSeenScanToken = :scanToken WHERE uri IN (:uris)")
    suspend fun markFilesSeen(uris: List<String>, scanToken: Long)

    @Query("DELETE FROM file_metadata WHERE lastSeenScanToken != :scanToken")
    suspend fun removeFilesNotSeenInScan(scanToken: Long): Int

    @Query("SELECT * FROM file_metadata ORDER BY sizeBytes DESC")
    suspend fun allFiles(): List<FileMetadataEntity>

    @Query("SELECT * FROM file_metadata WHERE uri = :uri LIMIT 1")
    suspend fun findFile(uri: String): FileMetadataEntity?

    @Query("SELECT * FROM file_metadata WHERE contentHash IS NOT NULL ORDER BY contentHash, sizeBytes DESC")
    fun observeDuplicateCandidates(): Flow<List<FileMetadataEntity>>

    @Query("SELECT * FROM file_metadata WHERE sizeBytes >= :minimumBytes ORDER BY sizeBytes DESC")
    fun observeLargeFiles(minimumBytes: Long): Flow<List<FileMetadataEntity>>

    @Query("SELECT * FROM file_metadata WHERE relativePath LIKE '%Download%' OR LOWER(displayName) LIKE '%.apk' ORDER BY modifiedEpochSeconds ASC")
    fun observeDownloads(): Flow<List<FileMetadataEntity>>

    @Query("UPDATE file_metadata SET contentHash = :contentHash, perceptualHash = :perceptualHash, blurScore = :blurScore, isScreenshot = :isScreenshot WHERE uri = :uri")
    suspend fun updateAnalysis(uri: String, contentHash: String?, perceptualHash: String?, blurScore: Double?, isScreenshot: Boolean)

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrashItem(item: TrashItemEntity)

    @Query("SELECT * FROM trash_items ORDER BY trashedAtEpochMillis DESC")
    fun observeTrash(): Flow<List<TrashItemEntity>>

    @Query("DELETE FROM trash_items WHERE uri = :uri")
    suspend fun removeTrashItem(uri: String)

    @Query("DELETE FROM trash_items")
    suspend fun clearTrash()

    @Insert
    suspend fun insertCompressionHistory(item: CompressionHistoryEntity)

    @Query("SELECT * FROM compression_history ORDER BY createdAtEpochMillis DESC LIMIT 50")
    fun observeCompressionHistory(): Flow<List<CompressionHistoryEntity>>
}

@Database(entities = [FileMetadataEntity::class, ScanHistoryEntity::class, TrashItemEntity::class, CompressionHistoryEntity::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun storageDao(): StorageDao

    companion object {
        @Volatile private var instance: AppDatabase? = null
        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "storage_intelligence.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                .build().also { instance = it }
        }

        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE file_metadata ADD COLUMN contentHash TEXT")
                db.execSQL("ALTER TABLE file_metadata ADD COLUMN perceptualHash TEXT")
                db.execSQL("ALTER TABLE file_metadata ADD COLUMN blurScore REAL")
                db.execSQL("ALTER TABLE file_metadata ADD COLUMN isScreenshot INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS trash_items (uri TEXT NOT NULL, displayName TEXT NOT NULL, sizeBytes INTEGER NOT NULL, trashedAtEpochMillis INTEGER NOT NULL, PRIMARY KEY(uri))")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_trash_items_trashedAtEpochMillis ON trash_items(trashedAtEpochMillis)")
            }
        }

        private val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE file_metadata ADD COLUMN durationMillis INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE file_metadata ADD COLUMN relativePath TEXT")
            }
        }

        private val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS compression_history (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, inputUri TEXT NOT NULL, outputUri TEXT, mediaType TEXT NOT NULL, preset TEXT NOT NULL, originalBytes INTEGER NOT NULL, outputBytes INTEGER NOT NULL, status TEXT NOT NULL, createdAtEpochMillis INTEGER NOT NULL)")
            }
        }

        private val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE file_metadata ADD COLUMN lastSeenScanToken INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_file_metadata_lastSeenScanToken ON file_metadata(lastSeenScanToken)")
            }
        }
    }
}
