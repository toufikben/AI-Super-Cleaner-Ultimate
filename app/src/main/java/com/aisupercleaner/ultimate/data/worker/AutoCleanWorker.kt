package com.aisupercleaner.ultimate.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aisupercleaner.ultimate.data.cleanup.CleanupManager
import com.aisupercleaner.ultimate.data.history.HistoryEntry
import com.aisupercleaner.ultimate.data.notifications.NotificationHelper
import com.aisupercleaner.ultimate.data.preferences.AppPreferences
import com.aisupercleaner.ultimate.data.scanner.JunkScanner
import com.aisupercleaner.ultimate.data.scanner.ScanResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.io.IOException
import java.util.concurrent.TimeoutException

@HiltWorker
class AutoCleanWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val scanner: JunkScanner,
    private val cleanupManager: CleanupManager,
    private val preferences: AppPreferences,
    private val notificationHelper: NotificationHelper,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            val enabled = preferences.autoCleanEnabled.first()
            if (!enabled) return Result.success()
            var completed: ScanResult.Completed? = null
            scanner.scan().collect { event -> if (event is ScanResult.Completed) completed = event }
            val result = completed ?: return Result.success()
            val safeItems = result.groups.filter { it.category.isSafeToDelete }.flatMap { it.items }
            if (safeItems.isEmpty()) return Result.success()
            val cleanup = cleanupManager.deleteJunk(safeItems, HistoryEntry.Source.AUTO)
            val notificationsEnabled = preferences.notificationsEnabled.first()
            if (notificationsEnabled && cleanup.freedBytes > 0) notificationHelper.showAutoCleanComplete(cleanup.freedBytes, cleanup.deletedCount)
            Result.success()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            if (error is IOException || error is TimeoutException) Result.retry() else Result.failure()
        }
    }
    companion object {
        const val WORK_NAME = "auto_clean_worker"
        const val UNIQUE_NAME = "auto_clean_unique"
        const val MANUAL_WORK_NAME = "auto_clean_manual"
    }
}
