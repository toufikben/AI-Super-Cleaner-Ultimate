package com.aisupercleaner.ultimate.presentation.screens.battery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aisupercleaner.ultimate.data.system.BatteryInfo
import com.aisupercleaner.ultimate.data.system.BatteryManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BatteryUiState(
    val info: BatteryInfo = BatteryInfo(0, false, BatteryInfo.Health.UNKNOWN, 0f, 0, "—", BatteryInfo.Plugged.NONE),
    val isLoading: Boolean = true,
)

@HiltViewModel
class BatteryViewModel @Inject constructor(private val batteryManager: BatteryManager) : ViewModel() {
    private val _uiState = MutableStateFlow(BatteryUiState())
    val uiState: StateFlow<BatteryUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        val info = batteryManager.getBatteryInfo()
        _uiState.update { it.copy(info = info, isLoading = false) }
    }
}
