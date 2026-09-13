package com.aisupercleaner.ultimate.presentation.screens.vault

import com.aisupercleaner.ultimate.data.vault.VaultFile

data class VaultUiState(
    val isLocked: Boolean = true,
    val needsSetup: Boolean = true,
    val files: List<VaultFile> = emptyList(),
    val totalBytes: Long = 0L,
    val errorMessage: String? = null,
    val pinInput: String = "",
    val pinError: Boolean = false,
    val biometricAvailable: Boolean = false,
    val biometricEnabled: Boolean = true,
    val isImporting: Boolean = false,
    val importProgress: ImportProgress? = null,
    val previewFile: VaultFile? = null,
) {
    val isEmpty: Boolean get() = files.isEmpty()

    data class ImportProgress(val currentName: String = "", val currentIndex: Int = 0, val total: Int = 0) {
        val fraction: Float get() = if (total > 0) currentIndex.toFloat() / total else 0f
    }
}
