package com.aisupercleaner.ultimate.privacy

import android.content.Context

/** Stores only app preferences and consent metadata; never stores media content or file names. */
class AppPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)

    var privacyCenterSeen: Boolean
        get() = preferences.getBoolean(KEY_PRIVACY_CENTER_SEEN, false)
        set(value) = preferences.edit().putBoolean(KEY_PRIVACY_CENTER_SEEN, value).apply()

    var scanExplanationSeen: Boolean
        get() = preferences.getBoolean(KEY_SCAN_EXPLANATION_SEEN, false)
        set(value) = preferences.edit().putBoolean(KEY_SCAN_EXPLANATION_SEEN, value).apply()

    var analyticsEnabled: Boolean
        get() = preferences.getBoolean(KEY_ANALYTICS_ENABLED, false)
        set(value) = preferences.edit().putBoolean(KEY_ANALYTICS_ENABLED, value).apply()

    var recoveredBytes: Long
        get() = preferences.getLong(KEY_RECOVERED_BYTES, 0L)
        set(value) = preferences.edit().putLong(KEY_RECOVERED_BYTES, value.coerceAtLeast(0L)).apply()

    var notificationsEnabled: Boolean
        get() = preferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, false)
        set(value) = preferences.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, value).apply()

    var darkTheme: Boolean
        get() = preferences.getBoolean(KEY_DARK_THEME, false)
        set(value) = preferences.edit().putBoolean(KEY_DARK_THEME, value).apply()

    var accentStyle: String
        get() = preferences.getString(KEY_ACCENT_STYLE, "teal") ?: "teal"
        set(value) = preferences.edit().putString(KEY_ACCENT_STYLE, value).apply()

    fun alertEnabled(type: CleanerAlertType): Boolean = preferences.getBoolean("alert_enabled_${type.key}", true)

    fun setAlertEnabled(type: CleanerAlertType, enabled: Boolean) {
        preferences.edit().putBoolean("alert_enabled_${type.key}", enabled).apply()
    }

    fun lastAlertAt(type: CleanerAlertType): Long = preferences.getLong("alert_last_${type.key}", 0L)

    fun markAlertAt(type: CleanerAlertType, timestamp: Long) {
        preferences.edit().putLong("alert_last_${type.key}", timestamp).apply()
    }

    companion object {
        private const val KEY_PRIVACY_CENTER_SEEN = "privacy_center_seen"
        private const val KEY_SCAN_EXPLANATION_SEEN = "scan_explanation_seen"
        private const val KEY_ANALYTICS_ENABLED = "analytics_enabled"
        private const val KEY_RECOVERED_BYTES = "recovered_bytes"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_DARK_THEME = "dark_theme"
        private const val KEY_ACCENT_STYLE = "accent_style"
    }
}
