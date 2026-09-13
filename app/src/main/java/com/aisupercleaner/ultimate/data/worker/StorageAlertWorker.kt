package com.aisupercleaner.ultimate.data.worker

import android.content.Context
import android.os.Environment
import android.os.StatFs
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aisupercleaner.ultimate.data.notifications.NotificationHelper
import com.aisupercleaner.ultimate.data.preferences.AppPreferences
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class StorageAlertWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val preferences: AppPreferences,
    private val notifications: NotificationHelper,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val threshold = runCatching { preferences.storageAlertThresholdPercent.first() }.getOrDefault(85)
        val enabled = runCatching { preferences.notificationsEnabled.first() }.getOrDefault(true)
        if (!enabled) return Result.success()

        return runCatching {
            val stat = StatFs(Environment.getDataDirectory().path)
            val total = stat.blockCountLong * stat.blockSizeLong
            val free = stat.availableBlocksLong * stat.blockSizeLong
            val usedPercent = ((total - free).toFloat() / total.toFloat() * 100).toInt()
            if (usedPercent >= threshold) notifications.showStorageAlert(usedPercent, free)
            Result.success()
        }.getOrElse { Result.retry() }
    }

    companion object { const val UNIQUE_NAME = "storage_alert_unique" }
}
