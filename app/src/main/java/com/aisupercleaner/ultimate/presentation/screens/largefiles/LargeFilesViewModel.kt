package com.aisupercleaner.ultimate.presentation.screens.largefiles

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aisupercleaner.ultimate.core.permissions.PermissionManager
import com.aisupercleaner.ultimate.data.cleanup.CleanupManager
import com.aisupercleaner.ultimate.data.history.HistoryEntry
import com.aisupercleaner.ultimate.data.scanner.LargeFileScanner
import com.aisupercleaner.ultimate.data.storage.StorageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LargeFilesViewModel @Inject constructor(
    private val scanner: LargeFileScanner,
    private val cleanupManager: CleanupManager,
    private val storageRepository: StorageRepository,
    private val permissionManager: PermissionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LargeFilesUiState())
    val uiState: StateFlow<LargeFilesUiState> = _uiState.asStateFlow()

    private var scanJob: Job? = null

    fun startScan() {
        scanJob?.cancel()
        if (!permissionManager.canReadFiles()) {
            _uiState.update { it.copy(phase = LargeFilesUiState.Phase.ERROR, errorMessage = "MISSING_PERMISSION") }
            return
        }
        _uiState.update { it.copy(phase = LargeFilesUiState.Phase.SCANNING, files = emptyList(), selectedPaths = emptySet(), scanned = 0, errorMessage = null) }

        scanJob = viewModelScope.launch {
            scanner.scan(_uiState.value.filter).collect { event ->
                when (event) {
                    is LargeFileScanner.Event.Progress -> _uiState.update { it.copy(scanned = event.progress.scanned, currentPath = event.progress.currentPath) }
                    is LargeFileScanner.Event.Done -> _uiState.update { it.copy(phase = LargeFilesUiState.Phase.READY, files = event.files, totalBytes = event.totalBytes) }
                    is LargeFileScanner.Event.Error -> _uiState.update { it.copy(phase = LargeFilesUiState.Phase.ERROR, errorMessage = event.throwable.message) }
                }
            }
        }
    }

    fun cancelScan() { scanJob?.cancel(); scanJob = null; _uiState.update { it.copy(phase = LargeFilesUiState.Phase.IDLE) } }
    fun setFilter(filter: LargeFileScanner.Filter) { _uiState.update { it.copy(filter = filter) } }
    fun toggleSelection(path: String) { _uiState.update { s -> s.copy(selectedPaths = s.selectedPaths.toMutableSet().apply { if (!add(path)) remove(path) }) } }
    fun selectAll() { _uiState.update { it.copy(selectedPaths = it.files.map { f -> f.path }.toSet()) } }
    fun deselectAll() { _uiState.update { it.copy(selectedPaths = emptySet()) } }

    fun deleteSelected() {
        val state = _uiState.value
        val paths = state.selectedPaths.toList()
        if (paths.isEmpty()) return
        _uiState.update { it.copy(phase = LargeFilesUiState.Phase.DELETING) }
        viewModelScope.launch {
            val result = cleanupManager.deleteFiles(paths, HistoryEntry.Source.LARGE_FILES)
            storageRepository.refresh()
            _uiState.update { s ->
                s.copy(
                    phase = LargeFilesUiState.Phase.DONE,
                    files = s.files.filterNot { it.path in result.successfulPaths },
                    selectedPaths = result.failedPaths.toSet(),
                    lastResult = result,
                )
            }
        }
    }

    fun dismissResult() { _uiState.update { it.copy(lastResult = null, phase = if (it.hasResults) LargeFilesUiState.Phase.READY else LargeFilesUiState.Phase.IDLE) } }
    fun dismissError() { _uiState.update { it.copy(errorMessage = null, phase = if (it.hasResults) LargeFilesUiState.Phase.READY else LargeFilesUiState.Phase.IDLE) } }
    fun openAllFilesSettings(): Intent = permissionManager.openAllFilesSettings()
}
