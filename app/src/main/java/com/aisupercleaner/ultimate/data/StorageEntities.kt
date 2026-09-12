package com.aisupercleaner.ultimate.data

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "file_metadata",
    primaryKeys = ["uri"],
    indices = [Index("mediaType"), Index("sizeBytes"), Index("modifiedEpochSeconds"), Index("lastSeenScanToken")]
)
data class FileMetadataEntity(
    val uri: String,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val modifiedEpochSeconds: Long,
    val mediaType: String,
    val contentHash: String? = null,
    val perceptualHash: String? = null,
    val blurScore: Double? = null,
    val isScreenshot: Boolean = false,
    val durationMillis: Long = 0L,
    val relativePath: String? = null,
    val lastSeenScanToken: Long = 0L
)

@Entity(tableName = "scan_history")
data class ScanHistoryEntity(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAtEpochMillis: Long,
    val completedAtEpochMillis: Long,
    val filesScanned: Int,
    val totalBytes: Long,
    val status: String
)

@Entity(tableName = "trash_items", indices = [Index("trashedAtEpochMillis")])
data class TrashItemEntity(
    @androidx.room.PrimaryKey val uri: String,
    val displayName: String,
    val sizeBytes: Long,
    val trashedAtEpochMillis: Long
)
