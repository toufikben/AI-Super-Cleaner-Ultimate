package com.aisupercleaner.ultimate.data.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutoCleanScheduler @Inject constructor(@ApplicationContext private val context: Context) {

    private val wm get() = WorkManager.getInstance(context)

    fun schedule(intervalHours: Int, requireCharging: Boolean = false) {
        val safeHours = intervalHours.coerceAtLeast(1)
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .apply { if (requireCharging) setRequiresCharging(true) }
            .build()

        val request = PeriodicWorkRequestBuilder<AutoCleanWorker>(
            safeHours.toLong(), TimeUnit.HOURS,
            (safeHours / 4).coerceAtLeast(1).toLong(), TimeUnit.HOURS,
        )
            .setConstraints(constraints)
            .addTag(AutoCleanWorker.WORK_NAME)
            .build()

        wm.enqueueUniquePeriodicWork(AutoCleanWorker.UNIQUE_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun cancel() { wm.cancelUniqueWork(AutoCleanWorker.UNIQUE_NAME) }

    fun runNow() {
        val request = androidx.work.OneTimeWorkRequestBuilder<AutoCleanWorker>()
            .addTag(AutoCleanWorker.WORK_NAME)
            .build()
        wm.enqueue(request)
    }
}
