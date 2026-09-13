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
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
    ): CleanupResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        var deleted = 0
        var failed = 0
        var freed = 0L
        val failures = mutableListOf<CleanupResult.Failure>()

        items.forEachIndexed { index, item ->
            val file = File(item.path)
            if (FileUtils.safeDelete(file)) {
                deleted++
                freed += item.sizeBytes
            } else {
                failed++
                failures += CleanupResult.Failure(item.path, "Permission denied or file in use")
            }
            if (index % 10 == 0 || index == items.lastIndex) onProgress(index + 1, items.size)
        }

        val result = CleanupResult(deleted, failed, freed, System.currentTimeMillis() - start, failures)

        // سجّل في History
        historyRepository.record(
            freedBytes = freed,
            deletedCount = deleted,
            failedCount = failed,
            source = HistoryEntry.Source.JUNK,
            durationMs = result.durationMs,
        )
        preferences.recordCleanup(freed)
        storageRepository.refresh()
        result
    }

    suspend fun deleteFiles(paths: List<String>): CleanupResult = deleteJunk(
        paths.map { path ->
            JunkItem(
                path = path,
                sizeBytes = File(path).length().coerceAtLeast(0),
                lastModified = File(path).lastModified(),
                category = JunkCategory.CACHE,
            )
        }
    )
}
