package com.aisupercleaner.ultimate.presentation.permissions

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aisupercleaner.ultimate.core.permissions.PermissionManager
import com.aisupercleaner.ultimate.core.permissions.PermissionStatus
import com.aisupercleaner.ultimate.core.permissions.PermissionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PermissionsViewModel @Inject constructor(
    private val permissionManager: PermissionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PermissionsUiState())
    val uiState: StateFlow<PermissionsUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val statuses = PermissionType.entries.associateWith { permissionManager.checkStatus(it) }
            _uiState.update { it.copy(statuses = statuses) }
        }
    }

    fun markRequested(type: PermissionType) = permissionManager.markRequested(type)
    fun statusFor(type: PermissionType): PermissionStatus = _uiState.value.statuses[type] ?: PermissionStatus.NOT_REQUESTED
    fun allFilesIntent(): Intent = permissionManager.openAllFilesSettings()
    fun appSettingsIntent(): Intent = permissionManager.openAppSettings()
}
