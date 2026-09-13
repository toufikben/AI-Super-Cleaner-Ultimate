package com.aisupercleaner.ultimate.data.notifications

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.aisupercleaner.ultimate.AISuperCleanerApp
import com.aisupercleaner.ultimate.R
import com.aisupercleaner.ultimate.core.util.Formatter
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(@ApplicationContext private val context: Context) {

    private val manager = NotificationManagerCompat.from(context)
    private var idCounter = 1000

    fun showAutoCleanComplete(freedBytes: Long, deletedCount: Int) {
        if (!canNotify()) return
        val n = NotificationCompat.Builder(context, AISuperCleanerApp.CHANNEL_AUTO_CLEAN)
            .setSmallIcon(android.R.drawable.ic_menu_delete)
            .setContentTitle(context.getString(R.string.notif_auto_clean_title))
            .setContentText(context.getString(R.string.notif_auto_clean_text, Formatter.formatBytes(freedBytes), deletedCount))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()
        notifySafely(n)
    }

    fun showStorageAlert(usedPercent: Int, freeBytes: Long) {
        if (!canNotify()) return
        val n = NotificationCompat.Builder(context, AISuperCleanerApp.CHANNEL_ALERTS)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(context.getString(R.string.notif_storage_title))
            .setContentText(context.getString(R.string.notif_storage_text, usedPercent, Formatter.formatBytes(freeBytes)))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        notifySafely(n)
    }

    fun showLargeFilesFound(count: Int, bytes: Long) {
        if (!canNotify()) return
        val n = NotificationCompat.Builder(context, AISuperCleanerApp.CHANNEL_ALERTS)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle(context.getString(R.string.notif_large_files_title))
            .setContentText(context.getString(R.string.notif_large_files_text, count, Formatter.formatBytes(bytes)))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()
        notifySafely(n)
    }

    @SuppressLint("MissingPermission")
    private fun notifySafely(notification: android.app.Notification) {
        if (!canNotify()) return
        runCatching { manager.notify(idCounter++, notification) }
    }

    private fun canNotify(): Boolean {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            return androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}
