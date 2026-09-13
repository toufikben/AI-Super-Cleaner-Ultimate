package com.aisupercleaner.ultimate.data.vault

data class VaultFile(
    val id: Long,
    val originalName: String,
    val encryptedName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val importedAt: Long,
    val isImage: Boolean,
    val isVideo: Boolean,
) {
    val displaySize: String get() = com.aisupercleaner.ultimate.core.util.Formatter.formatBytes(sizeBytes)
}
