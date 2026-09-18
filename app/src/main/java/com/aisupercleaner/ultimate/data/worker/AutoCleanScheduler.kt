package com.aisupercleaner.ultimate.data.worker

import javax.inject.Inject
import javax.inject.Singleton

/** Compatibility facade retained for existing callers. */
@Singleton
class AutoCleanScheduler @Inject constructor(private val scheduler: WorkerScheduler) {
    fun schedule(intervalHours: Int, requireCharging: Boolean = false) = scheduler.scheduleAutoClean(intervalHours, requireCharging)
    suspend fun cancel() = scheduler.cancelAutoClean()
    fun runNow() = scheduler.runAutoCleanNow()
}
