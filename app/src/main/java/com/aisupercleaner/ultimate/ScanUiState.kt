package com.aisupercleaner.ultimate

import com.aisupercleaner.ultimate.data.ScanProgress

sealed interface ScanUiState {
    data object Idle : ScanUiState
    data object PermissionRequired : ScanUiState
    data class Scanning(val progress: ScanProgress? = null) : ScanUiState
    data object Analyzing : ScanUiState
    data class Success(val message: String) : ScanUiState
    data class Error(val message: String) : ScanUiState
    data object Cancelled : ScanUiState
}

fun ScanUiState.isBusy(): Boolean = this is ScanUiState.Scanning || this is ScanUiState.Analyzing
