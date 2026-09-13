package com.aisupercleaner.ultimate.data.local.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "duplicate_groups",
    indices = [
        Index("fingerprint", unique = true),
        Index("wastedBytes"),
        Index("scannedAt"),
    ]
)
data class DuplicateGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fingerprint: String,
    val kind: String,
    val wastedBytes: Long,
    val itemCount: Int,
    val scannedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "duplicate_items",
    indices = [
        Index("groupId"),
        Index("path", unique = true),
    ],
    foreignKeys = [
        ForeignKey(
            entity = DuplicateGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE,
        )
    ]
)
data class DuplicateItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val path: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val isImage: Boolean,
)
