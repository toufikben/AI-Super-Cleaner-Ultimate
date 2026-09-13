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
class WorkerScheduler @Inject constructor(@ApplicationContext private val context: Context) {
    private val wm get() = WorkManager.getInstance(context)

    fun scheduleStorageAlerts() {
        val request = PeriodicWorkRequestBuilder<StorageAlertWorker>(6, TimeUnit.HOURS, 1, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .build()
        wm.enqueueUniquePeriodicWork(StorageAlertWorker.UNIQUE_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun cancelStorageAlerts() { wm.cancelUniqueWork(StorageAlertWorker.UNIQUE_NAME) }
}
