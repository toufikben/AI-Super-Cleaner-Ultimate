package com.aisupercleaner.ultimate.data

import android.content.ContentResolver
import android.content.ContentUris
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.coroutineContext

data class ScanProgress(val stage: String, val scannedFiles: Int, val discoveredBytes: Long, val cacheHits: Int = 0)
data class ScanResult(val filesScanned: Int, val totalBytes: Long, val cacheHits: Int, val cacheMisses: Int)

class StorageScanner(private val resolver: ContentResolver, private val dao: StorageDao) {
    suspend fun scan(onProgress: (ScanProgress) -> Unit): ScanResult = withContext(Dispatchers.IO) {
        val startedAt = System.currentTimeMillis()
        var totalFiles = 0
        var totalBytes = 0L
        var cacheHits = 0
        var cacheMisses = 0
        try {
            val sources = listOf(
                Triple(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image", "Scanning photos"),
                Triple(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "video", "Scanning videos"),
                Triple(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "audio", "Scanning audio")
            )
            for ((collection, mediaType, stage) in sources) {
                coroutineContext.ensureActive()
                val batch = ArrayList<FileMetadataEntity>(200)
                val projection = arrayOf(
                    MediaStore.MediaColumns._ID,
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    MediaStore.MediaColumns.MIME_TYPE,
                    MediaStore.MediaColumns.SIZE,
                    MediaStore.MediaColumns.DATE_MODIFIED,
                    MediaStore.MediaColumns.DURATION,
                    MediaStore.MediaColumns.RELATIVE_PATH
                )
                resolver.query(collection, projection, null, null, null)?.use { cursor ->
                    val idIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                    val mimeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                    val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                    val modifiedIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                    val durationIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DURATION)
                    val pathIndex = cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
                    while (cursor.moveToNext()) {
                        coroutineContext.ensureActive()
                        val id = cursor.getLong(idIndex)
                        val size = cursor.getLong(sizeIndex).coerceAtLeast(0L)
                        val metadata = FileMetadataEntity(
                            uri = ContentUris.withAppendedId(collection, id).toString(),
                            displayName = cursor.getString(nameIndex) ?: "Unnamed file",
                            mimeType = cursor.getString(mimeIndex) ?: "application/octet-stream",
                            sizeBytes = size,
                            modifiedEpochSeconds = cursor.getLong(modifiedIndex),
                            mediaType = mediaType,
                            durationMillis = if (durationIndex >= 0 && !cursor.isNull(durationIndex)) cursor.getLong(durationIndex) else 0L,
                            relativePath = if (pathIndex >= 0 && !cursor.isNull(pathIndex)) cursor.getString(pathIndex) else null
                        )
                        val cached = dao.findFile(metadata.uri)
                        if (ScanCachePolicy.isUnchanged(cached, metadata)) cacheHits++ else { batch += metadata; cacheMisses++ }
                        totalFiles++
                        totalBytes += size
                        if (batch.size == 200) {
                            dao.upsertFiles(batch.toList())
                            batch.clear()
                            onProgress(ScanProgress(stage, totalFiles, totalBytes, cacheHits))
                        }
                    }
                }
                if (batch.isNotEmpty()) dao.upsertFiles(batch)
                onProgress(ScanProgress(stage, totalFiles, totalBytes, cacheHits))
            }
            dao.insertScanHistory(ScanHistoryEntity(startedAtEpochMillis = startedAt, completedAtEpochMillis = System.currentTimeMillis(), filesScanned = totalFiles, totalBytes = totalBytes, status = "completed"))
            ScanResult(totalFiles, totalBytes, cacheHits, cacheMisses)
        } catch (e: SecurityException) {
            dao.insertScanHistory(ScanHistoryEntity(startedAtEpochMillis = startedAt, completedAtEpochMillis = System.currentTimeMillis(), filesScanned = totalFiles, totalBytes = totalBytes, status = "permission_denied"))
            throw e
        } catch (e: IOException) {
            dao.insertScanHistory(ScanHistoryEntity(startedAtEpochMillis = startedAt, completedAtEpochMillis = System.currentTimeMillis(), filesScanned = totalFiles, totalBytes = totalBytes, status = "io_error"))
            throw e
        }
    }
}
