package com.aisupercleaner.ultimate.presentation.screens.largefiles

import com.aisupercleaner.ultimate.data.cleanup.CleanupResult
import com.aisupercleaner.ultimate.data.scanner.LargeFileScanner

data class LargeFilesUiState(
    val phase: Phase = Phase.IDLE,
    val files: List<LargeFileScanner.LargeFile> = emptyList(),
    val totalBytes: Long = 0L,
    val selectedPaths: Set<String> = emptySet(),
    val scanned: Int = 0,
    val currentPath: String = "",
    val filter: LargeFileScanner.Filter = LargeFileScanner.Filter(),
    val lastResult: CleanupResult? = null,
    val errorMessage: String? = null,
) {
    val totalSelectedBytes: Long get() = files.filter { it.path in selectedPaths }.sumOf { it.sizeBytes }
    val totalSelectedCount: Int get() = selectedPaths.size
    val hasResults: Boolean get() = files.isNotEmpty()

    enum class Phase { IDLE, SCANNING, READY, DELETING, DONE, ERROR }
}
