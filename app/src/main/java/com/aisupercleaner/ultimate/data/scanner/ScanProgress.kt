package com.aisupercleaner.ultimate.data.scanner

data class ScanProgress(
    val phase: Phase = Phase.IDLE,
    val scannedFiles: Int = 0,
    val totalFiles: Int = 0,
    val currentPath: String = "",
    val foundJunkBytes: Long = 0L,
) {
    val progress: Float get() = if (totalFiles > 0) scannedFiles.toFloat() / totalFiles else 0f

    enum class Phase { IDLE, ENUMERATING, SCANNING, FINALIZING, COMPLETED, CANCELLED, ERROR }
}
