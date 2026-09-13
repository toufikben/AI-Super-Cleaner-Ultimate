package com.aisupercleaner.ultimate.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aisupercleaner.ultimate.presentation.theme.AccentStyle
import com.aisupercleaner.ultimate.presentation.theme.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_super_cleaner_prefs")

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_STYLE = stringPreferencesKey("accent_style")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val IS_PREMIUM = booleanPreferencesKey("is_premium")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val AUTO_CLEAN_ENABLED = booleanPreferencesKey("auto_clean_enabled")
        val AUTO_CLEAN_INTERVAL_HOURS = intPreferencesKey("auto_clean_interval_hours")
        val LAST_CLEAN_TIMESTAMP = longPreferencesKey("last_clean_timestamp")
        val TOTAL_CLEANED_BYTES = longPreferencesKey("total_cleaned_bytes")
        val CLEANUP_COUNT = intPreferencesKey("cleanup_count")
        val STORAGE_ALERT_THRESHOLD_PERCENT = intPreferencesKey("storage_alert_threshold")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        prefs[Keys.THEME_MODE]?.let { stored -> runCatching { ThemeMode.valueOf(stored) }.getOrNull() } ?: ThemeMode.SYSTEM
    }

    val accentStyle: Flow<AccentStyle> = context.dataStore.data.map { prefs ->
        prefs[Keys.ACCENT_STYLE]?.let { stored -> runCatching { AccentStyle.valueOf(stored) }.getOrNull() } ?: AccentStyle.OCEAN_BLUE
    }

    val dynamicColor: Flow<Boolean> = context.dataStore.data.map { it[Keys.DYNAMIC_COLOR] ?: true }
    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { it[Keys.ONBOARDING_COMPLETED] ?: false }
    val isPremium: Flow<Boolean> = context.dataStore.data.map { it[Keys.IS_PREMIUM] ?: false }
    val appLockEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.APP_LOCK_ENABLED] ?: false }
    val autoCleanEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTO_CLEAN_ENABLED] ?: false }
    val autoCleanIntervalHours: Flow<Int> = context.dataStore.data.map { it[Keys.AUTO_CLEAN_INTERVAL_HOURS] ?: 24 }
    val lastCleanTimestamp: Flow<Long> = context.dataStore.data.map { it[Keys.LAST_CLEAN_TIMESTAMP] ?: 0L }
    val totalCleanedBytes: Flow<Long> = context.dataStore.data.map { it[Keys.TOTAL_CLEANED_BYTES] ?: 0L }
    val cleanupCount: Flow<Int> = context.dataStore.data.map { it[Keys.CLEANUP_COUNT] ?: 0 }
    val storageAlertThresholdPercent: Flow<Int> = context.dataStore.data.map { it[Keys.STORAGE_ALERT_THRESHOLD_PERCENT] ?: 85 }
    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.NOTIFICATIONS_ENABLED] ?: true }

    suspend fun setThemeMode(mode: ThemeMode) = context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    suspend fun setAccentStyle(style: AccentStyle) = context.dataStore.edit { it[Keys.ACCENT_STYLE] = style.name }
    suspend fun setDynamicColor(enabled: Boolean) = context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    suspend fun setOnboardingCompleted(value: Boolean) = context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = value }
    suspend fun setPremium(value: Boolean) = context.dataStore.edit { it[Keys.IS_PREMIUM] = value }
    suspend fun setAppLockEnabled(value: Boolean) = context.dataStore.edit { it[Keys.APP_LOCK_ENABLED] = value }
    suspend fun setAutoCleanEnabled(value: Boolean) = context.dataStore.edit { it[Keys.AUTO_CLEAN_ENABLED] = value }
    suspend fun setAutoCleanIntervalHours(hours: Int) = context.dataStore.edit { it[Keys.AUTO_CLEAN_INTERVAL_HOURS] = hours }
    suspend fun setStorageAlertThresholdPercent(value: Int) = context.dataStore.edit { it[Keys.STORAGE_ALERT_THRESHOLD_PERCENT] = value }
    suspend fun setNotificationsEnabled(value: Boolean) = context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = value }

    suspend fun recordCleanup(bytesFreed: Long) = context.dataStore.edit { prefs ->
        prefs[Keys.LAST_CLEAN_TIMESTAMP] = System.currentTimeMillis()
        prefs[Keys.TOTAL_CLEANED_BYTES] = (prefs[Keys.TOTAL_CLEANED_BYTES] ?: 0L) + bytesFreed
        prefs[Keys.CLEANUP_COUNT] = (prefs[Keys.CLEANUP_COUNT] ?: 0) + 1
    }
}
