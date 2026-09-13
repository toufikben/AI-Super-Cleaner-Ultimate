package com.aisupercleaner.ultimate.presentation.screens.scheduler

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aisupercleaner.ultimate.data.preferences.AppPreferences
import com.aisupercleaner.ultimate.data.worker.AutoCleanScheduler
import com.aisupercleaner.ultimate.data.worker.WorkerScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SchedulerUiState(
    val autoCleanEnabled: Boolean = false,
    val intervalHours: Int = 24,
    val lastClean: Long = 0L,
)

@HiltViewModel
class SchedulerViewModel @Inject constructor(
    private val preferences: AppPreferences,
    private val autoCleanScheduler: AutoCleanScheduler,
    private val workerScheduler: WorkerScheduler,
) : ViewModel() {

    val uiState: StateFlow<SchedulerUiState> = combine(
        preferences.autoCleanEnabled,
        preferences.autoCleanIntervalHours,
        preferences.lastCleanTimestamp,
    ) { enabled, interval, last -> SchedulerUiState(enabled, interval, last) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SchedulerUiState())

    init { workerScheduler.scheduleStorageAlerts() }

    fun setEnabled(enabled: Boolean) = viewModelScope.launch {
        preferences.setAutoCleanEnabled(enabled)
        if (enabled) autoCleanScheduler.schedule(uiState.value.intervalHours) else autoCleanScheduler.cancel()
    }

    fun setInterval(hours: Int) = viewModelScope.launch {
        preferences.setAutoCleanIntervalHours(hours)
        if (uiState.value.autoCleanEnabled) autoCleanScheduler.schedule(hours)
    }

    fun runNow() { autoCleanScheduler.runNow() }
}
