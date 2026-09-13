package com.aisupercleaner.ultimate.presentation.screens.duplicates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aisupercleaner.ultimate.core.permissions.PermissionManager
import com.aisupercleaner.ultimate.data.cleanup.CleanupManager
import com.aisupercleaner.ultimate.data.repository.DuplicateRepository
import com.aisupercleaner.ultimate.data.scanner.duplicates.DuplicateEngine
import com.aisupercleaner.ultimate.data.scanner.duplicates.DuplicateGroup
import com.aisupercleaner.ultimate.data.scanner.duplicates.Event
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
class DuplicatesViewModel @Inject constructor(
    private val engine: DuplicateEngine,
    private val repository: DuplicateRepository,
    private val cleanupManager: CleanupManager,
    private val storageRepository: StorageRepository,
    private val permissionManager: PermissionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DuplicatesUiState())
    val uiState: StateFlow<DuplicatesUiState> = _uiState.asStateFlow()

    private var scanJob: Job? = null

    init {
        viewModelScope.launch {
            repository.observeSavedGroups().collect { groups ->
                if (groups.isNotEmpty() && _uiState.value.groups.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            groups = groups,
                            phase = DuplicatesUiState.Phase.READY,
                            selectedPaths = computeSmartSelection(groups, it.keepPolicy),
                        )
                    }
                }
            }
        }
    }

    fun startScan(includeSimilarImages: Boolean = true) {
        scanJob?.cancel()
        if (!permissionManager.canReadFiles()) {
            _uiState.update { it.copy(phase = DuplicatesUiState.Phase.ERROR, errorMessage = "MISSING_PERMISSION") }
            return
        }

        _uiState.update {
            it.copy(
                phase = DuplicatesUiState.Phase.SCANNING,
                progress = DuplicatesUiState.Progress(),
                groups = emptyList(),
                selectedPaths = emptySet(),
                expandedGroups = emptySet(),
                errorMessage = null,
            )
        }

        scanJob = viewModelScope.launch {
            engine.findDuplicates(includeImages = includeSimilarImages).collect { event ->
                when (event) {
                    is Event.Progress -> {
                        val p = event.progress
                        _uiState.update {
                            it.copy(progress = DuplicatesUiState.Progress(p.phase.name, p.scanned, p.total, p.currentPath))
                        }
                    }
                    is Event.Done -> {
                        val result = event.result
                        _uiState.update {
                            it.copy(
                                phase = DuplicatesUiState.Phase.READY,
                                groups = result.groups,
                                selectedPaths = computeSmartSelection(result.groups, it.keepPolicy),
                                expandedGroups = emptySet(),
                            )
                        }
                        repository.saveResult(result)
                    }
                    is Event.Error -> _uiState.update {
                        it.copy(phase = DuplicatesUiState.Phase.ERROR, errorMessage = event.throwable.message ?: "Scan failed")
                    }
                }
            }
        }
    }

    fun cancelScan() {
        scanJob?.cancel(); scanJob = null
        _uiState.update { it.copy(phase = if (it.hasResults) DuplicatesUiState.Phase.READY else DuplicatesUiState.Phase.IDLE) }
    }

    fun toggleItem(path: String) {
        _uiState.update { state ->
            val newSet = state.selectedPaths.toMutableSet().apply { if (!add(path)) remove(path) }
            state.copy(selectedPaths = newSet)
        }
    }

    fun toggleGroup(group: DuplicateGroup) {
        _uiState.update { state ->
            val keepPath = keepPathOf(group, state.keepPolicy)
            val paths = group.items.map { it.path }.toSet()
            val allSelected = paths.all { it in state.selectedPaths }
            val newSet = state.selectedPaths.toMutableSet().apply {
                if (allSelected) removeAll(paths)
                else addAll(paths.filter { it != keepPath })
            }
            state.copy(selectedPaths = newSet)
        }
    }

    fun toggleExpanded(groupId: String) {
        _uiState.update { state ->
            val newSet = state.expandedGroups.toMutableSet().apply { if (!add(groupId)) remove(groupId) }
            state.copy(expandedGroups = newSet)
        }
    }

    fun setKeepPolicy(policy: DuplicatesUiState.KeepPolicy) {
        _uiState.update { state ->
            state.copy(keepPolicy = policy, selectedPaths = computeSmartSelection(state.groups, policy))
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
        val paths = state.selectedPaths.toList()
        if (paths.isEmpty()) return

        _uiState.update { it.copy(phase = DuplicatesUiState.Phase.DELETING) }

        viewModelScope.launch {
            val result = cleanupManager.deleteFiles(paths)
            repository.deletePaths(paths)

            _uiState.update { s ->
                val remaining = s.groups.mapNotNull { group ->
                    val left = group.items.filter { it.path !in paths }
                    when {
                        left.isEmpty() -> null
                        left.size == 1 -> null
                        else -> group.copy(items = left)
                    }
                }
                s.copy(
                    phase = DuplicatesUiState.Phase.DONE,
                    groups = remaining,
                    selectedPaths = emptySet(),
                    lastResult = result,
                )
            }
        }
    }

    fun dismissResult() {
        _uiState.update { it.copy(lastResult = null, phase = if (it.hasResults) DuplicatesUiState.Phase.READY else DuplicatesUiState.Phase.IDLE) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null, phase = if (it.hasResults) DuplicatesUiState.Phase.READY else DuplicatesUiState.Phase.IDLE) }
    }

    private fun computeSmartSelection(groups: List<DuplicateGroup>, policy: DuplicatesUiState.KeepPolicy): Set<String> {
        val result = mutableSetOf<String>()
        groups.forEach { group ->
            val keep = keepPathOf(group, policy)
            group.items.forEach { if (it.path != keep) result += it.path }
        }
        return result
    }

    private fun keepPathOf(group: DuplicateGroup, policy: DuplicatesUiState.KeepPolicy): String = when (policy) {
        DuplicatesUiState.KeepPolicy.OLDEST -> group.items.minByOrNull { it.lastModified }?.path.orEmpty()
        DuplicatesUiState.KeepPolicy.NEWEST -> group.items.maxByOrNull { it.lastModified }?.path.orEmpty()
        DuplicatesUiState.KeepPolicy.SHORTEST_PATH -> group.items.minByOrNull { it.path.length }?.path.orEmpty()
    }

    fun openAllFilesSettings() = permissionManager.openAllFilesSettings()
}
