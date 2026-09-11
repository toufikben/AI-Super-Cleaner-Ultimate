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

    companion object {
        private const val KEY_PRIVACY_CENTER_SEEN = "privacy_center_seen"
        private const val KEY_SCAN_EXPLANATION_SEEN = "scan_explanation_seen"
        private const val KEY_ANALYTICS_ENABLED = "analytics_enabled"
    }
}
