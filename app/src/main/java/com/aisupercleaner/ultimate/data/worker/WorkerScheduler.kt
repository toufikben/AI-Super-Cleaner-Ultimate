package com.aisupercleaner.ultimate.data.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.await
import com.aisupercleaner.ultimate.data.preferences.AppPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkerScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: AppPreferences,
) {
    private val wm get() = WorkManager.getInstance(context)

    /** Reconciles persisted settings; safe to call repeatedly, including on application startup. */
    suspend fun scheduleFromPreferences() {
        val autoEnabled = preferences.autoCleanEnabled.first()
        val intervals = resolveIntervals(
            preferences.autoCleanIntervalHours.first(),
            preferences.storageAlertIntervalHours.first(),
        )
        val notificationsEnabled = preferences.notificationsEnabled.first()
        if (autoEnabled) scheduleAutoClean(intervals.autoCleanHours) else cancelAutoClean()
        if (notificationsEnabled) scheduleStorageAlerts(intervals.storageAlertHours) else cancelStorageAlerts()
    }

    fun scheduleAutoClean(intervalHours: Int, requireCharging: Boolean = false) {
        val safeHours = intervalHours.coerceAtLeast(1)
        val constraints = Constraints.Builder().setRequiresBatteryNotLow(true)
            .apply { if (requireCharging) setRequiresCharging(true) }.build()
        val request = PeriodicWorkRequestBuilder<AutoCleanWorker>(safeHours.toLong(), TimeUnit.HOURS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_DELAY_MINUTES, TimeUnit.MINUTES)
            .addTag(AutoCleanWorker.WORK_NAME).build()
        wm.enqueueUniquePeriodicWork(AutoCleanWorker.UNIQUE_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    suspend fun cancelAutoClean() {
        awaitOperation("auto clean", wm.cancelUniqueWork(AutoCleanWorker.UNIQUE_NAME))
    }

    fun scheduleStorageAlerts(intervalHours: Int = DEFAULT_STORAGE_ALERT_INTERVAL_HOURS) {
        val safeHours = intervalHours.coerceAtLeast(1)
        val request = PeriodicWorkRequestBuilder<StorageAlertWorker>(safeHours.toLong(), TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_DELAY_MINUTES, TimeUnit.MINUTES)
            .addTag(StorageAlertWorker.WORK_NAME).build()
        wm.enqueueUniquePeriodicWork(StorageAlertWorker.UNIQUE_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    suspend fun cancelStorageAlerts() {
        awaitOperation("storage alerts", wm.cancelUniqueWork(StorageAlertWorker.UNIQUE_NAME))
    }

    fun runAutoCleanNow() {
        val request = OneTimeWorkRequestBuilder<AutoCleanWorker>()
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_DELAY_MINUTES, TimeUnit.MINUTES)
            .addTag(AutoCleanWorker.WORK_NAME).build()
        wm.enqueueUniqueWork(AutoCleanWorker.MANUAL_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    /** Cancels and prunes without blocking the calling thread; suspend callers can await completion. */
    suspend fun cancelAllWork(): Boolean = try {
        awaitOperation("auto clean", wm.cancelUniqueWork(AutoCleanWorker.UNIQUE_NAME))
        awaitOperation("manual auto clean", wm.cancelUniqueWork(AutoCleanWorker.MANUAL_WORK_NAME))
        awaitOperation("storage alerts", wm.cancelUniqueWork(StorageAlertWorker.UNIQUE_NAME))
        awaitOperation("all work", wm.cancelAllWork())
        awaitOperation("prune work", wm.pruneWork())
        true
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Throwable) {
        false
    }

    private suspend fun awaitOperation(label: String, operation: androidx.work.Operation) {
        try {
            operation.await()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            throw WorkManagerOperationException(label, error)
        }
    }

    companion object {
        const val DEFAULT_STORAGE_ALERT_INTERVAL_HOURS = 6
        private const val BACKOFF_DELAY_MINUTES = 30L

        fun resolveIntervals(autoCleanIntervalHours: Int, storageAlertIntervalHours: Int) =
            WorkerScheduleIntervals(
                autoCleanHours = autoCleanIntervalHours.coerceAtLeast(1),
                storageAlertHours = storageAlertIntervalHours.coerceAtLeast(1),
            )
    }
}

data class WorkerScheduleIntervals(
    val autoCleanHours: Int,
    val storageAlertHours: Int,
)

private class WorkManagerOperationException(label: String, cause: Throwable) :
    IllegalStateException("WorkManager operation failed: $label", cause)
