package com.aisupercleaner.ultimate.data

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface CleanupResult {
    data class Success(val itemsMoved: Int, val bytesMoved: Long) : CleanupResult
    data class Failure(val message: String) : CleanupResult
}

class CleanupManager(private val resolver: ContentResolver, private val dao: StorageDao) {
    suspend fun moveToTrash(items: List<FileMetadataEntity>): CleanupResult = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext CleanupResult.Failure("No files were selected.")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@withContext CleanupResult.Failure("This Android version does not provide recoverable MediaStore trash. No file was deleted.")
        var moved = 0
        var bytes = 0L
        items.forEach { item ->
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_TRASHED, 1)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) put(MediaStore.MediaColumns.DATE_EXPIRES, (System.currentTimeMillis() / 1000L) + 30L * 24L * 60L * 60L)
            }
            val updated = resolver.update(Uri.parse(item.uri), values, null, null)
            if (updated > 0) {
                dao.insertTrashItem(TrashItemEntity(item.uri, item.displayName, item.sizeBytes, System.currentTimeMillis()))
                moved++
                bytes += item.sizeBytes
            }
        }
        if (moved == 0) CleanupResult.Failure("No selected file could be moved to Trash.") else CleanupResult.Success(moved, bytes)
    }

    suspend fun restore(item: TrashItemEntity): CleanupResult = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@withContext CleanupResult.Failure("Restore is unavailable on this Android version.")
        val values = ContentValues().apply { put(MediaStore.MediaColumns.IS_TRASHED, 0); putNull(MediaStore.MediaColumns.DATE_EXPIRES) }
        val restored = resolver.update(Uri.parse(item.uri), values, null, null)
        if (restored > 0) { dao.removeTrashItem(item.uri); CleanupResult.Success(1, item.sizeBytes) } else CleanupResult.Failure("The item is no longer available to restore.")
    }

    suspend fun deletePermanently(item: TrashItemEntity): CleanupResult = withContext(Dispatchers.IO) {
        val deleted = resolver.delete(Uri.parse(item.uri), null, null)
        if (deleted > 0) { dao.removeTrashItem(item.uri); CleanupResult.Success(1, item.sizeBytes) } else CleanupResult.Failure("The item could not be deleted permanently.")
    }
}
