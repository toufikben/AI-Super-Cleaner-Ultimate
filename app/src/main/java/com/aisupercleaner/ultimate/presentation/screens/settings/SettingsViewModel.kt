package com.aisupercleaner.ultimate.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aisupercleaner.ultimate.data.local.database.AppDatabase
import com.aisupercleaner.ultimate.data.preferences.AppPreferences
import com.aisupercleaner.ultimate.data.vault.VaultAuthManager
import com.aisupercleaner.ultimate.data.worker.AutoCleanScheduler
import com.aisupercleaner.ultimate.data.worker.WorkerScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SettingsUiState(
    val notificationsEnabled: Boolean = true,
    val autoCleanEnabled: Boolean = false,
    val storageAlertThreshold: Int = 85,
    val isPremium: Boolean = false,
    val vaultPinSet: Boolean = false,
    val biometricEnabled: Boolean = true,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: AppPreferences,
    private val autoCleanScheduler: AutoCleanScheduler,
    private val workerScheduler: WorkerScheduler,
    private val vaultAuthManager: VaultAuthManager,
    private val database: AppDatabase,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        preferences.notificationsEnabled,
        preferences.autoCleanEnabled,
        preferences.storageAlertThresholdPercent,
        preferences.isPremium,
    ) { notif, auto, threshold, premium ->
        SettingsUiState(
            notificationsEnabled = notif,
            autoCleanEnabled = auto,
            storageAlertThreshold = threshold,
            isPremium = premium,
            vaultPinSet = vaultAuthManager.isPinSet(),
            biometricEnabled = vaultAuthManager.isBiometricEnabled(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setNotifications(enabled: Boolean) = viewModelScope.launch {
        preferences.setNotificationsEnabled(enabled)
        if (enabled) workerScheduler.scheduleStorageAlerts() else workerScheduler.cancelStorageAlerts()
    }

    fun setAutoClean(enabled: Boolean) = viewModelScope.launch {
        preferences.setAutoCleanEnabled(enabled)
        if (enabled) autoCleanScheduler.schedule(24) else autoCleanScheduler.cancel()
    }

    fun setThreshold(percent: Int) = viewModelScope.launch { preferences.setStorageAlertThresholdPercent(percent) }

    fun clearAllData(onDone: () -> Unit) = viewModelScope.launch {
        withContext(Dispatchers.IO) { runCatching { database.clearAllTables() } }
        onDone()
    }
}
