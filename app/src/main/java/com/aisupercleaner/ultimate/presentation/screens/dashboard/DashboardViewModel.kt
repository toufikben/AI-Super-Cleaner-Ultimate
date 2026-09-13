package com.aisupercleaner.ultimate.presentation.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aisupercleaner.ultimate.data.forecast.StorageForecast
import com.aisupercleaner.ultimate.data.forecast.StorageForecaster
import com.aisupercleaner.ultimate.data.preferences.AppPreferences
import com.aisupercleaner.ultimate.data.storage.StorageInfo
import com.aisupercleaner.ultimate.data.storage.StorageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val storage: StorageInfo = StorageInfo.EMPTY,
    val isLoading: Boolean = true,
    val totalCleanedBytes: Long = 0L,
    val cleanupCount: Int = 0,
    val lastCleanTimestamp: Long = 0L,
    val forecast: StorageForecast? = null,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val storageRepository: StorageRepository,
    private val preferences: AppPreferences,
    private val forecaster: StorageForecaster,
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    private val _forecast = MutableStateFlow<StorageForecast?>(null)

    val uiState: StateFlow<DashboardUiState> = combine(
        storageRepository.refreshTrigger.flatMapLatest { storageRepository.observeStorageInfo() },
        preferences.totalCleanedBytes,
        preferences.cleanupCount,
        preferences.lastCleanTimestamp,
        _isLoading,
        _forecast,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        DashboardUiState(
            storage = values[0] as StorageInfo,
            totalCleanedBytes = values[1] as Long,
            cleanupCount = values[2] as Int,
            lastCleanTimestamp = values[3] as Long,
            isLoading = values[4] as Boolean,
            forecast = values[5] as StorageForecast?,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState(),
    )

    init {
        viewModelScope.launch {
            storageRepository.observeStorageInfo().first()
            _isLoading.value = false
        }
        viewModelScope.launch {
            _forecast.value = runCatching { forecaster.forecast() }.getOrNull()
        }
    }

    fun refresh() {
        _isLoading.value = true
        storageRepository.refresh()
        viewModelScope.launch {
            kotlinx.coroutines.delay(800)
            _isLoading.value = false
            _forecast.value = runCatching { forecaster.forecast() }.getOrNull()
        }
    }
}
