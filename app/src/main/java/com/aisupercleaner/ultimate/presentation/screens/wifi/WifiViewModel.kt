package com.aisupercleaner.ultimate.presentation.screens.wifi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aisupercleaner.ultimate.data.wifi.WifiSecurityReport
import com.aisupercleaner.ultimate.data.wifi.WifiSecurityScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WifiUiState(val isLoading: Boolean = true, val report: WifiSecurityReport? = null)

@HiltViewModel
class WifiViewModel @Inject constructor(private val scanner: WifiSecurityScanner) : ViewModel() {
    private val _uiState = MutableStateFlow(WifiUiState())
    val uiState: StateFlow<WifiUiState> = _uiState.asStateFlow()

    init { scan() }

    fun scan() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val report = scanner.scan()
            _uiState.update { it.copy(isLoading = false, report = report) }
        }
    }
}
