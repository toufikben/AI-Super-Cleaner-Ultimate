package com.aisupercleaner.ultimate.data

import android.content.ContentResolver
import android.net.Uri
import android.provider.MediaStore
import android.os.Bundle

sealed interface RevalidationResult {
    data class Current(val metadata: FileMetadataEntity) : RevalidationResult
    data class Changed(val metadata: FileMetadataEntity) : RevalidationResult
    data object Missing : RevalidationResult
    data class Failed(val reason: String) : RevalidationResult
}

/** Reads the provider again immediately before a destructive operation. */
class StorageRevalidator(private val resolver: ContentResolver) {
    fun readCurrent(expected: FileMetadataEntity, includeTrashed: Boolean = false): RevalidationResult {
        val projection = arrayOf(
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.RELATIVE_PATH
        )
        return try {
            val queryArgs = if (includeTrashed && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) Bundle().apply { putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, 1) } else null
            resolver.query(Uri.parse(expected.uri), projection, queryArgs, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return RevalidationResult.Missing
                val name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)) ?: "Unnamed file"
                val mime = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)) ?: "application/octet-stream"
                val size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)).coerceAtLeast(0L)
                val modified = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED))
                val pathIndex = cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
                val path = if (pathIndex >= 0 && !cursor.isNull(pathIndex)) cursor.getString(pathIndex) else null
                val current = expected.copy(displayName = name, mimeType = mime, sizeBytes = size, modifiedEpochSeconds = modified, relativePath = path)
                if (current.displayName == expected.displayName && current.mimeType == expected.mimeType && current.sizeBytes == expected.sizeBytes && current.modifiedEpochSeconds == expected.modifiedEpochSeconds && current.relativePath == expected.relativePath) RevalidationResult.Current(current) else RevalidationResult.Changed(current)
            } ?: RevalidationResult.Failed("Provider returned no cursor")
        } catch (_: SecurityException) {
            RevalidationResult.Failed("Permission to revalidate this URI is no longer available")
        } catch (_: IllegalArgumentException) {
            RevalidationResult.Failed("The file URI is no longer valid")
        }
    }

    fun readTrashCurrent(item: TrashItemEntity): RevalidationResult = readCurrent(
        FileMetadataEntity(item.uri, item.displayName, "application/octet-stream", item.sizeBytes, 0L, "other")
    , includeTrashed = true)
}
