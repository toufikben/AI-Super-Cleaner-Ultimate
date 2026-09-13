package com.aisupercleaner.ultimate.presentation.screens.duplicates

import com.aisupercleaner.ultimate.data.cleanup.CleanupResult
import com.aisupercleaner.ultimate.data.scanner.duplicates.DuplicateGroup

data class DuplicatesUiState(
    val phase: Phase = Phase.IDLE,
    val progress: Progress = Progress(),
    val groups: List<DuplicateGroup> = emptyList(),
    val selectedPaths: Set<String> = emptySet(),
    val expandedGroups: Set<String> = emptySet(),
    val keepPolicy: KeepPolicy = KeepPolicy.OLDEST,
    val lastResult: CleanupResult? = null,
    val errorMessage: String? = null,
) {
    val totalSelectedBytes: Long get() = groups.flatMap { it.items }.filter { it.path in selectedPaths }.sumOf { it.sizeBytes }
    val totalSelectedCount: Int get() = selectedPaths.size
    val totalWastedBytes: Long get() = groups.sumOf { it.wastedBytes }
    val totalGroups: Int get() = groups.size
    val totalDuplicates: Int get() = groups.sumOf { it.count - 1 }
    val hasResults: Boolean get() = groups.isNotEmpty()

    enum class Phase { IDLE, SCANNING, READY, DELETING, DONE, ERROR }
    enum class KeepPolicy { OLDEST, NEWEST, SHORTEST_PATH }

    data class Progress(
        val phaseName: String = "",
        val scanned: Int = 0,
        val total: Int = 0,
        val currentPath: String = "",
    ) {
        val fraction: Float get() = if (total > 0) scanned.toFloat() / total else 0f
    }
}
