package com.aisupercleaner.ultimate.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.os.StatFs
import android.widget.RemoteViews
import com.aisupercleaner.ultimate.MainActivity
import com.aisupercleaner.ultimate.R
import com.aisupercleaner.ultimate.core.util.Formatter

class StorageWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id -> updateWidget(context, appWidgetManager, id) }
    }

    companion object {
        fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_storage)

            val stat = StatFs(Environment.getDataDirectory().path)
            val total = stat.blockCountLong * stat.blockSizeLong
            val free = stat.availableBlocksLong * stat.blockSizeLong
            val used = total - free
            val percent = if (total > 0) (used * 100 / total).toInt() else 0

            views.setTextViewText(R.id.widget_used, Formatter.formatBytes(used))
            views.setTextViewText(R.id.widget_free, context.getString(R.string.widget_free, Formatter.formatBytes(free)))
            views.setTextViewText(R.id.widget_percent, "$percent%")
            views.setProgressBar(R.id.widget_progress, 100, percent, false)

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pending = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_root, pending)

            manager.updateAppWidget(widgetId, views)
        }
    }
}
