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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.io.IOException
import java.util.concurrent.TimeoutException

@HiltWorker
class StorageAlertWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val preferences: AppPreferences,
    private val notifications: NotificationHelper,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            val threshold = preferences.storageAlertThresholdPercent.first()
            val enabled = preferences.notificationsEnabled.first()
            if (!enabled) return Result.success()
            val stat = StatFs(Environment.getDataDirectory().path)
            val total = stat.blockCountLong * stat.blockSizeLong
            val free = stat.availableBlocksLong * stat.blockSizeLong
            if (total <= 0L) {
                Result.success()
            } else {
                val usedPercent = ((total - free).toFloat() / total.toFloat() * 100).toInt()
                if (usedPercent >= threshold.coerceIn(1, 100)) notifications.showStorageAlert(usedPercent, free)
                Result.success()
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            when (error) {
                is IOException, is TimeoutException -> Result.retry()
                else -> Result.failure()
            }
        }
    }
    companion object {
        const val WORK_NAME = "storage_alert_worker"
        const val UNIQUE_NAME = "storage_alert_unique"
    }
}
