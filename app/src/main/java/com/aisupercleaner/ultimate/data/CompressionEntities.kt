package com.aisupercleaner.ultimate.data

import androidx.room.Entity

@Entity(tableName = "compression_history")
data class CompressionHistoryEntity(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Long = 0,
    val inputUri: String,
    val outputUri: String?,
    val mediaType: String,
    val preset: String,
    val originalBytes: Long,
    val outputBytes: Long,
    val status: String,
    val createdAtEpochMillis: Long
)
