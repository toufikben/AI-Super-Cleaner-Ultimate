package com.aisupercleaner.ultimate.presentation.screens.ram

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aisupercleaner.ultimate.data.system.RamInfo
import com.aisupercleaner.ultimate.data.system.RamManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RamUiState(
    val info: RamInfo = RamInfo.EMPTY,
    val isBoosting: Boolean = false,
    val lastFreedBytes: Long = 0L,
    val showSuccess: Boolean = false,
)

@HiltViewModel
class RamViewModel @Inject constructor(private val ramManager: RamManager) : ViewModel() {
    private val _uiState = MutableStateFlow(RamUiState())
    val uiState: StateFlow<RamUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        val info = ramManager.getRamInfo()
        _uiState.update { it.copy(info = info) }
    }

    fun boost() {
        _uiState.update { it.copy(isBoosting = true, showSuccess = false) }
        viewModelScope.launch {
            val freed = ramManager.boost()
            val info = ramManager.getRamInfo()
            _uiState.update { it.copy(info = info, isBoosting = false, lastFreedBytes = freed, showSuccess = true) }
        }
    }

    fun dismissSuccess() { _uiState.update { it.copy(showSuccess = false) } }
}
