package com.aisupercleaner.ultimate.data.cleanup

import com.aisupercleaner.ultimate.core.util.FileUtils
import com.aisupercleaner.ultimate.data.history.HistoryEntry
import com.aisupercleaner.ultimate.data.history.HistoryRepository
import com.aisupercleaner.ultimate.data.preferences.AppPreferences
import com.aisupercleaner.ultimate.data.scanner.JunkCategory
import com.aisupercleaner.ultimate.data.scanner.JunkItem
import com.aisupercleaner.ultimate.data.storage.StorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CleanupManager @Inject constructor(
    private val storageRepository: StorageRepository,
    private val preferences: AppPreferences,
    private val historyRepository: HistoryRepository,
) {

    // ✅ FIX: يسجّل في History
    suspend fun deleteJunk(
        items: List<JunkItem>,
        source: HistoryEntry.Source,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
    ): CleanupResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        var freed = 0L
        val successfulPaths = mutableListOf<String>()
        val failedPaths = mutableListOf<String>()
        val failures = mutableListOf<CleanupResult.Failure>()

        items.forEachIndexed { index, item ->
            val file = File(item.path)
            val sizeBeforeDelete = FileUtils.safeLength(file)
            if (FileUtils.safeDelete(file)) {
                successfulPaths += item.path
                freed += sizeBeforeDelete
            } else {
                failedPaths += item.path
                failures += CleanupResult.Failure(item.path, "Permission denied or file in use")
            }
            if (index % 10 == 0 || index == items.lastIndex) onProgress(index + 1, items.size)
        }

        val result = CleanupResult(
            deletedCount = successfulPaths.size,
            failedCount = failedPaths.size,
            freedBytes = freed,
            durationMs = System.currentTimeMillis() - start,
            successfulPaths = successfulPaths,
            failedPaths = failedPaths,
            failures = failures,
        )

        // Empty and failure-only runs are not cleanup sessions and must not
        // create misleading history rows or update cleanup totals.
        if (result.deletedCount > 0) {
            historyRepository.record(
                freedBytes = result.freedBytes,
                deletedCount = result.deletedCount,
                failedCount = result.failedCount,
                source = source,
                durationMs = result.durationMs,
            )
            preferences.recordCleanup(result.freedBytes)
            storageRepository.refresh()
        }
        result
    }

    suspend fun deleteFiles(paths: List<String>, source: HistoryEntry.Source): CleanupResult = deleteJunk(
        items = paths.map { path ->
            JunkItem(
                path = path,
                sizeBytes = File(path).length().coerceAtLeast(0),
                lastModified = File(path).lastModified(),
                category = JunkCategory.CACHE,
            )
        },
        source = source,
    )
}
