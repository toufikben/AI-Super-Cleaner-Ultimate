package com.aisupercleaner.ultimate.data

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

sealed interface CleanupResult {
    data class Success(val itemsMoved: Int, val bytesMoved: Long) : CleanupResult
    data class PartialSuccess(val itemsMoved: Int, val bytesMoved: Long, val failedItems: Int, val message: String) : CleanupResult
    data class Failure(val message: String) : CleanupResult
}

class CleanupManager(private val resolver: ContentResolver, private val dao: StorageDao) {
    private val revalidator = StorageRevalidator(resolver)

    suspend fun moveToTrash(items: List<FileMetadataEntity>): CleanupResult = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext CleanupResult.Failure("No files were selected.")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@withContext CleanupResult.Failure("This Android version does not provide recoverable MediaStore trash. No file was deleted.")
        var moved = 0
        var bytes = 0L
        var failed = 0
        var stale = 0
        items.distinctBy { it.uri }.forEach { expected ->
            coroutineContext.ensureActive()
            val current = when (val result = revalidator.readCurrent(expected)) {
                is RevalidationResult.Current -> result.metadata
                is RevalidationResult.Changed -> { stale++; null }
                is RevalidationResult.Missing -> { stale++; null }
                is RevalidationResult.Failed -> { failed++; null }
            } ?: return@forEach
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_TRASHED, 1)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) put(MediaStore.MediaColumns.DATE_EXPIRES, (System.currentTimeMillis() / 1000L) + 30L * 24L * 60L * 60L)
            }
            try {
                val updated = resolver.update(Uri.parse(current.uri), values, null, null)
                if (updated > 0) {
                    try {
                        dao.insertTrashItem(TrashItemEntity(current.uri, current.displayName, current.sizeBytes, System.currentTimeMillis()))
                        dao.deleteFile(current.uri)
                        moved++
                        bytes += current.sizeBytes
                    } catch (_: Exception) {
                        resolver.update(Uri.parse(current.uri), ContentValues().apply { put(MediaStore.MediaColumns.IS_TRASHED, 0) }, null, null)
                        failed++
                    }
                } else failed++
            } catch (_: SecurityException) { failed++ }
            catch (_: IllegalArgumentException) { failed++ }
        }
        val skipped = stale + failed
        when {
            moved == 0 -> CleanupResult.Failure(if (stale > 0) "Selected results changed or disappeared; nothing was moved. Please scan again." else "No selected file could be moved to Trash.")
            skipped == 0 -> CleanupResult.Success(moved, bytes)
            else -> CleanupResult.PartialSuccess(moved, bytes, skipped, "$moved items moved to Trash; $stale stale results and $failed failed operations were skipped. Please scan again.")
        }
    }

    suspend fun restore(item: TrashItemEntity): CleanupResult = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@withContext CleanupResult.Failure("Restore is unavailable on this Android version.")
        coroutineContext.ensureActive()
        when (val before = revalidator.readTrashCurrent(item)) {
            is RevalidationResult.Missing -> return@withContext CleanupResult.Failure("The Trash item is no longer available to restore.")
            is RevalidationResult.Failed -> return@withContext CleanupResult.Failure(before.reason)
            else -> Unit
        }
        val values = ContentValues().apply { put(MediaStore.MediaColumns.IS_TRASHED, 0); putNull(MediaStore.MediaColumns.DATE_EXPIRES) }
        try {
            val restored = resolver.update(Uri.parse(item.uri), values, null, null)
            if (restored > 0) {
                dao.removeTrashItem(item.uri)
                val after = revalidator.readTrashCurrent(item)
                if (after is RevalidationResult.Current || after is RevalidationResult.Changed) {
                    val metadata = (after as? RevalidationResult.Current)?.metadata ?: (after as RevalidationResult.Changed).metadata
                    dao.upsertFiles(listOf(metadata.copy(contentHash = null, perceptualHash = null, blurScore = null, isScreenshot = false, analysisVersion = 0, lastSeenScanToken = 0)))
                }
                CleanupResult.Success(1, item.sizeBytes)
            } else CleanupResult.Failure("The item is no longer available to restore.")
        } catch (_: SecurityException) { CleanupResult.Failure("Permission to restore this item is no longer available.") }
        catch (_: IllegalArgumentException) { CleanupResult.Failure("The item URI is no longer valid.") }
    }

    suspend fun deletePermanently(item: TrashItemEntity): CleanupResult = withContext(Dispatchers.IO) {
        coroutineContext.ensureActive()
        when (val before = revalidator.readTrashCurrent(item)) {
            is RevalidationResult.Missing -> {
                dao.removeTrashItem(item.uri)
                dao.deleteFile(item.uri)
                return@withContext CleanupResult.Success(1, item.sizeBytes)
            }
            is RevalidationResult.Failed -> return@withContext CleanupResult.Failure(before.reason)
            else -> Unit
        }
        try {
            val deleted = resolver.delete(Uri.parse(item.uri), null, null)
            if (deleted > 0) {
                dao.removeTrashItem(item.uri)
                dao.deleteFile(item.uri)
                CleanupResult.Success(1, item.sizeBytes)
            } else CleanupResult.Failure("The item could not be deleted permanently.")
        } catch (_: SecurityException) { CleanupResult.Failure("Permission to delete this item is no longer available.") }
        catch (_: IllegalArgumentException) { CleanupResult.Failure("The item URI is no longer valid.") }
    }
}
