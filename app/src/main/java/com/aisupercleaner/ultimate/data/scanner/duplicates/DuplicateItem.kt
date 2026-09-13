package com.aisupercleaner.ultimate.data.scanner.duplicates

data class DuplicateItem(
    val path: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val isImage: Boolean = false,
    val perceptualHash: Long? = null,
    val sha256: String? = null,
) {
    val fileName: String get() = path.substringAfterLast('/')
    val folder: String get() = path.substringBeforeLast('/', "")
}

data class DuplicateGroup(
    val id: String,
    val items: List<DuplicateItem>,
    val kind: Kind,
) {
    val totalBytes: Long get() = items.sumOf { it.sizeBytes }
    val wastedBytes: Long get() = items.drop(1).sumOf { it.sizeBytes }
    val count: Int get() = items.size

    enum class Kind { EXACT, SIMILAR }
}

data class DuplicateScanResult(
    val groups: List<DuplicateGroup>,
    val scannedFiles: Int,
    val durationMs: Long,
) {
    val totalWastedBytes: Long get() = groups.sumOf { it.wastedBytes }
    val totalGroups: Int get() = groups.size
    val totalDuplicates: Int get() = groups.sumOf { it.count - 1 }
}
