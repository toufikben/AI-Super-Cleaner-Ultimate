package com.aisupercleaner.ultimate.presentation.screens.junk

import com.aisupercleaner.ultimate.data.cleanup.CleanupResult
import com.aisupercleaner.ultimate.data.scanner.JunkCategory
import com.aisupercleaner.ultimate.data.scanner.JunkGroup
import com.aisupercleaner.ultimate.data.scanner.ScanProgress

data class JunkUiState(
    val phase: Phase = Phase.IDLE,
    val progress: ScanProgress = ScanProgress(),
    val groups: List<JunkGroup> = emptyList(),
    val selectedPaths: Set<String> = emptySet(),
    val expandedCategories: Set<JunkCategory> = emptySet(),
    val lastResult: CleanupResult? = null,
    val errorMessage: String? = null,
) {
    val totalSelectedBytes: Long get() = groups.flatMap { it.items }.filter { it.path in selectedPaths }.sumOf { it.sizeBytes }
    val totalSelectedCount: Int get() = selectedPaths.size
    val totalJunkBytes: Long get() = groups.sumOf { it.totalBytes }
    val totalJunkCount: Int get() = groups.sumOf { it.count }
    val allSelected: Boolean get() = groups.isNotEmpty() && groups.flatMap { it.items }.all { it.path in selectedPaths }
    val hasResults: Boolean get() = groups.isNotEmpty()

    enum class Phase { IDLE, SCANNING, READY, CLEANING, DONE, ERROR }
}
