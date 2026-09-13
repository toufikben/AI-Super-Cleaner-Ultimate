package com.aisupercleaner.ultimate.core.util

import java.text.DecimalFormat
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

object Formatter {

    private val sizeUnits = arrayOf("B", "KB", "MB", "GB", "TB")
    private val decimalFormat = DecimalFormat("#,##0.##")

    fun formatBytes(bytes: Long, locale: Locale = Locale.getDefault()): String {
        if (bytes <= 0) return "0 B"
        val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
            .coerceIn(0, sizeUnits.lastIndex)
        val value = bytes / 1024.0.pow(digitGroups.toDouble())
        val formatted = when {
            value >= 100 -> String.format(locale, "%.0f", value)
            value >= 10 -> String.format(locale, "%.1f", value)
            else -> String.format(locale, "%.2f", value)
        }
        return "$formatted ${sizeUnits[digitGroups]}"
    }

    fun formatNumber(value: Long, locale: Locale = Locale.getDefault()): String =
        if (value < 1000) value.toString()
        else decimalFormat.apply { decimalFormatSymbols = java.text.DecimalFormatSymbols(locale) }.format(value)

    fun formatPercent(value: Float): String = "${(value * 100).toInt().coerceIn(0, 100)}%"

    fun formatRelativeTime(timestamp: Long, now: Long = System.currentTimeMillis()): String {
        if (timestamp <= 0) return "—"
        val diff = now - timestamp
        return when {
            diff < 60_000 -> "منذ لحظات"
            diff < 3_600_000 -> "منذ ${diff / 60_000} دقيقة"
            diff < 86_400_000 -> "منذ ${diff / 3_600_000} ساعة"
            diff < 2_592_000_000 -> "منذ ${diff / 86_400_000} يوم"
            else -> "منذ فترة طويلة"
        }
    }

    fun formatEta(seconds: Long): String = when {
        seconds < 60 -> "$seconds ثانية"
        seconds < 3600 -> "${seconds / 60} دقيقة"
        else -> "${seconds / 3600} ساعة"
    }
}
