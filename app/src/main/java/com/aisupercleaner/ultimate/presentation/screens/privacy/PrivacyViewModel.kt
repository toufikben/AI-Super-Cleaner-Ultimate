package com.aisupercleaner.ultimate.presentation.screens.privacy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aisupercleaner.ultimate.data.privacy.AppPrivacyInfo
import com.aisupercleaner.ultimate.data.privacy.PrivacyScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PrivacyUiState(
    val isLoading: Boolean = true,
    val apps: List<AppPrivacyInfo> = emptyList(),
    val filter: Filter = Filter.ALL,
) {
    val highRiskCount: Int get() = apps.count { it.riskLevel == AppPrivacyInfo.RiskLevel.HIGH }
    val filtered: List<AppPrivacyInfo> get() = when (filter) {
        Filter.ALL -> apps
        Filter.HIGH -> apps.filter { it.riskLevel == AppPrivacyInfo.RiskLevel.HIGH }
        Filter.MEDIUM_PLUS -> apps.filter { it.riskLevel == AppPrivacyInfo.RiskLevel.HIGH || it.riskLevel == AppPrivacyInfo.RiskLevel.MEDIUM }
    }
    enum class Filter { ALL, HIGH, MEDIUM_PLUS }
}

@HiltViewModel
class PrivacyViewModel @Inject constructor(private val scanner: PrivacyScanner) : ViewModel() {
    private val _uiState = MutableStateFlow(PrivacyUiState())
    val uiState: StateFlow<PrivacyUiState> = _uiState.asStateFlow()

    init { scan() }

    fun scan() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            scanner.scan().collect { apps -> _uiState.update { it.copy(apps = apps, isLoading = false) } }
        }
    }

    fun setFilter(filter: PrivacyUiState.Filter) { _uiState.update { it.copy(filter = filter) } }
}
