package com.aisupercleaner.ultimate.presentation.screens.shredder

import com.aisupercleaner.ultimate.data.shredder.ShredResult

data class ShredderUiState(
    val phase: Phase = Phase.IDLE,
    val selectedFiles: List<SelectedFile> = emptyList(),
    val level: Level = Level.SECURE,
    val currentIndex: Int = 0,
    val results: List<ShredResult> = emptyList(),
    val errorMessage: String? = null,
) {
    val totalBytes: Long get() = selectedFiles.sumOf { it.sizeBytes }
    val isRunning: Boolean get() = phase == Phase.SHREDDING

    enum class Phase { IDLE, PICKING, SHREDDING, DONE, ERROR }
    enum class Level(val passes: Int, val titleRes: Int, val descRes: Int) {
        FAST(1, com.aisupercleaner.ultimate.R.string.shred_level_fast, com.aisupercleaner.ultimate.R.string.shred_level_fast_desc),
        SECURE(3, com.aisupercleaner.ultimate.R.string.shred_level_secure, com.aisupercleaner.ultimate.R.string.shred_level_secure_desc),
        MILITARY(7, com.aisupercleaner.ultimate.R.string.shred_level_military, com.aisupercleaner.ultimate.R.string.shred_level_military_desc),
    }
    data class SelectedFile(val path: String, val sizeBytes: Long) {
        val name: String get() = path.substringAfterLast('/')
    }
}
