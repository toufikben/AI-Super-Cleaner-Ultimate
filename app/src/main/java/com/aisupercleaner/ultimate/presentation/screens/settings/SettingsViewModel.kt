package com.aisupercleaner.ultimate.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aisupercleaner.ultimate.billing.BillingManager
import com.aisupercleaner.ultimate.data.clear.ClearAllDataResult
import com.aisupercleaner.ultimate.data.local.database.AppDatabase
import com.aisupercleaner.ultimate.data.preferences.AppPreferences
import com.aisupercleaner.ultimate.data.vault.VaultAuthManager
import com.aisupercleaner.ultimate.data.vault.VaultManager
import com.aisupercleaner.ultimate.data.worker.WorkerScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
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
    private val billing: BillingManager,
    private val workerScheduler: WorkerScheduler,
    private val vaultAuthManager: VaultAuthManager,
    private val database: AppDatabase,
    private val vaultManager: VaultManager,
) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> = combine(
        preferences.notificationsEnabled,
        preferences.autoCleanEnabled,
        preferences.storageAlertThresholdPercent,
        billing.isPremium,
    ) { notif, auto, threshold, premium ->
        SettingsUiState(notif, auto, threshold, premium, vaultAuthManager.isPinSet(), vaultAuthManager.isBiometricEnabled())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setNotifications(enabled: Boolean) = viewModelScope.launch {
        preferences.setNotificationsEnabled(enabled)
        workerScheduler.scheduleFromPreferences()
    }

    fun setAutoClean(enabled: Boolean) = viewModelScope.launch {
        preferences.setAutoCleanEnabled(enabled)
        workerScheduler.scheduleFromPreferences()
    }

    fun setThreshold(percent: Int) = viewModelScope.launch { preferences.setStorageAlertThresholdPercent(percent.coerceIn(1, 100)) }

    /** Backward-compatible callback; callers that need the explicit outcome can use the overload below. */
    fun clearAllData(onDone: () -> Unit) = viewModelScope.launch {
        clearAllDataInternal()
        onDone()
    }

    fun clearAllData(onDone: (ClearAllDataResult) -> Unit) = viewModelScope.launch {
        onDone(clearAllDataInternal())
    }

    private suspend fun clearAllDataInternal(): ClearAllDataResult = withContext(Dispatchers.IO) {
        val errors = mutableListOf<String>()
        val workCleared = workerScheduler.cancelAllWork()
        if (!workCleared) errors += "workManager: cancel failed"
        val vaultResult = runCatching { vaultManager.clearAllData() }
            .onFailure { if (it is CancellationException) throw it }
            .getOrElse {
                errors += "vault: ${it.message ?: it::class.simpleName}"
                null
            }
        val databaseCleared = runCatching { database.clearAllTables(); true }
            .onFailure { if (it is CancellationException) throw it }
            .getOrElse {
                errors += "database: ${it.message ?: it::class.simpleName}"
                false
            }
        val dataStoreCleared = runCatching { preferences.clearAllData(); true }
            .onFailure { if (it is CancellationException) throw it }
            .getOrElse {
                errors += "dataStore: ${it.message ?: it::class.simpleName}"
                false
            }
        errors += vaultResult?.errors.orEmpty()
        ClearAllDataResult(
            databaseCleared = databaseCleared,
            dataStoreCleared = dataStoreCleared,
            vaultCleared = vaultResult?.vaultCleared == true,
            tempCleared = vaultResult?.tempCleared == true,
            workManagerCleared = workCleared,
            errors = errors,
        )
    }
}
