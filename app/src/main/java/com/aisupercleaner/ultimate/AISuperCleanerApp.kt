package com.aisupercleaner.ultimate

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.aisupercleaner.ultimate.data.worker.WorkerScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class AISuperCleanerApp : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var workerScheduler: WorkerScheduler

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            runCatching { workerScheduler.scheduleFromPreferences() }
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_AUTO_CLEAN, getString(R.string.channel_auto_clean), NotificationManager.IMPORTANCE_LOW)
                .apply { description = getString(R.string.channel_auto_clean_desc) }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ALERTS, getString(R.string.channel_alerts), NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = getString(R.string.channel_alerts_desc) }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_VAULT, getString(R.string.channel_vault), NotificationManager.IMPORTANCE_HIGH)
                .apply { setShowBadge(false) }
        )
    }

    companion object {
        const val CHANNEL_AUTO_CLEAN = "auto_clean"
        const val CHANNEL_ALERTS = "alerts"
        const val CHANNEL_VAULT = "vault"
    }
}
