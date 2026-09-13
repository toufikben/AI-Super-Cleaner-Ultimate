package com.aisupercleaner.ultimate.data.local.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cleanup_history",
    indices = [Index("timestamp")],
)
data class CleanupHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val freedBytes: Long,
    val deletedCount: Int,
    val failedCount: Int,
    val source: String,
    val durationMs: Long,
)
