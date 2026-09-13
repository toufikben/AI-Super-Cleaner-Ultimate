package com.aisupercleaner.ultimate.presentation.screens.junk

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aisupercleaner.ultimate.core.permissions.PermissionManager
import com.aisupercleaner.ultimate.data.cleanup.CleanupManager
import com.aisupercleaner.ultimate.data.scanner.JunkCategory
import com.aisupercleaner.ultimate.data.scanner.JunkItem
import com.aisupercleaner.ultimate.data.scanner.JunkScanner
import com.aisupercleaner.ultimate.data.scanner.ScanProgress
import com.aisupercleaner.ultimate.data.scanner.ScanResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JunkViewModel @Inject constructor(
    private val scanner: JunkScanner,
    private val cleanupManager: CleanupManager,
    private val permissionManager: PermissionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(JunkUiState())
    val uiState: StateFlow<JunkUiState> = _uiState.asStateFlow()

    private var scanJob: Job? = null

    fun startScanWithPermissionCheck() {
        if (!permissionManager.canReadFiles()) {
            _uiState.update { it.copy(phase = JunkUiState.Phase.ERROR, errorMessage = "MISSING_PERMISSION") }
            return
        }
        startScan()
    }

    fun startScan() {
        scanJob?.cancel()
        _uiState.update {
            it.copy(
                phase = JunkUiState.Phase.SCANNING,
                progress = ScanProgress(),
                groups = emptyList(),
                selectedPaths = emptySet(),
                errorMessage = null,
            )
        }

        scanJob = viewModelScope.launch {
            scanner.scan().collect { result ->
                when (result) {
                    is ScanResult.Progress -> _uiState.update { it.copy(progress = result.progress) }
                    is ScanResult.Completed -> {
                        val defaultSelected = result.groups
                            .filter { it.category.isSafeToDelete }
                            .flatMap { it.items }
                            .map { it.path }
                            .toSet()

                        _uiState.update {
                            it.copy(
                                phase = JunkUiState.Phase.READY,
                                groups = result.groups,
                                selectedPaths = defaultSelected,
                                expandedCategories = result.groups.map { g -> g.category }.toSet(),
                            )
                        }
                    }
                    is ScanResult.Error -> _uiState.update {
                        it.copy(phase = JunkUiState.Phase.ERROR, errorMessage = result.throwable.message ?: "Unknown error")
                    }
                }
            }
        }
    }

    fun cancelScan() {
        scanJob?.cancel(); scanJob = null
        _uiState.update { it.copy(phase = JunkUiState.Phase.IDLE) }
    }

    fun toggleSelection(path: String) {
        _uiState.update { state ->
            val newSet = state.selectedPaths.toMutableSet().apply { if (!add(path)) remove(path) }
            state.copy(selectedPaths = newSet)
        }
    }

    fun toggleCategory(category: JunkCategory) {
        _uiState.update { state ->
            val categoryItems = state.groups.firstOrNull { it.category == category }?.items?.map { it.path }?.toSet() ?: emptySet()
            val allSelected = categoryItems.all { it in state.selectedPaths }
            val newSelected = state.selectedPaths.toMutableSet().apply {
                if (allSelected) removeAll(categoryItems) else addAll(categoryItems)
            }
            state.copy(selectedPaths = newSelected)
        }
    }

    fun toggleExpanded(category: JunkCategory) {
        _uiState.update { state ->
            val newSet = state.expandedCategories.toMutableSet().apply { if (!add(category)) remove(category) }
            state.copy(expandedCategories = newSet)
        }
    }

    fun selectAll() {
        _uiState.update { state ->
            state.copy(selectedPaths = state.groups.flatMap { it.items }.map { it.path }.toSet())
        }
    }

    fun deselectAll() { _uiState.update { it.copy(selectedPaths = emptySet()) } }

    fun deleteSelected() {
        val state = _uiState.value
        val items: List<JunkItem> = state.groups.flatMap { it.items }.filter { it.path in state.selectedPaths }
        if (items.isEmpty()) return

        _uiState.update { it.copy(phase = JunkUiState.Phase.CLEANING) }

        viewModelScope.launch {
            val result = cleanupManager.deleteJunk(items)
            _uiState.update {
                it.copy(
                    phase = JunkUiState.Phase.DONE,
                    lastResult = result,
                    groups = it.groups.mapNotNull { group ->
                        val remaining = group.items.filter { item -> item.path !in state.selectedPaths }
                        if (remaining.isEmpty()) null else group.copy(items = remaining)
                    },
                    selectedPaths = emptySet(),
                )
            }
        }
    }

    fun dismissResult() {
        _uiState.update { it.copy(lastResult = null, phase = if (it.hasResults) JunkUiState.Phase.READY else JunkUiState.Phase.IDLE) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null, phase = if (it.hasResults) JunkUiState.Phase.READY else JunkUiState.Phase.IDLE) }
    }

    fun openAllFilesSettings(): Intent = permissionManager.openAllFilesSettings()
}
