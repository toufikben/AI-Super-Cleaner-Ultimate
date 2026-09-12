package com.aisupercleaner.ultimate.privacy

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.aisupercleaner.ultimate.MainActivity
import com.aisupercleaner.ultimate.R

private const val CHANNEL_ID = "cleaner_insights"
private const val COOLDOWN_MILLIS = 24L * 60L * 60L * 1000L

enum class CleanerAlertType(val key: String, val preferenceLabelRes: Int) {
    STORAGE("storage", R.string.notification_alert_storage),
    LARGE_FILES("large_files", R.string.notification_alert_large_files),
    TRASH("trash", R.string.notification_alert_trash),
    RAM("ram", R.string.notification_alert_ram),
    OPPORTUNITY("opportunity", R.string.notification_alert_opportunity)
}

class NotificationCenter(private val context: Context) {
    private val preferences = AppPreferences(context)

    fun notifyIfAllowed(type: CleanerAlertType, title: String, message: String, destination: Int) {
        if (!preferences.notificationsEnabled || !preferences.alertEnabled(type)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val now = System.currentTimeMillis()
        if (now - preferences.lastAlertAt(type) < COOLDOWN_MILLIS) return
        createChannel()
        val intent = Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_DESTINATION, destination).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending = PendingIntent.getActivity(context, type.key.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(type.key.hashCode(), notification)
        preferences.markAlertAt(type, now)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, context.getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_DEFAULT).apply { description = context.getString(R.string.notification_channel_description) })
        }
    }
}
