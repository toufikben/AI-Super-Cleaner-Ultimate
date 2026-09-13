package com.aisupercleaner.ultimate.data.local.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "vault_files",
    indices = [
        Index("encryptedName", unique = true),
        Index("importedAt"),
    ]
)
data class VaultFileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val originalName: String,
    val encryptedName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val importedAt: Long,
    val isImage: Boolean,
    val isVideo: Boolean,
)
