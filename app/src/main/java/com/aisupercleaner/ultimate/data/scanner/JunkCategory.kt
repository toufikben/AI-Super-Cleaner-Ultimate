package com.aisupercleaner.ultimate.data.scanner

import androidx.annotation.StringRes
import com.aisupercleaner.ultimate.R

enum class JunkCategory(
    @StringRes val titleRes: Int,
    val isSafeToDelete: Boolean,
) {
    CACHE(R.string.junk_cache, true),
    TEMP(R.string.junk_temp, true),
    LOGS(R.string.junk_logs, true),
    THUMBNAILS(R.string.junk_thumbnails, true),
    EMPTY_FOLDERS(R.string.junk_empty_folders, true),
    APK_RESIDUE(R.string.junk_apk, false),
    DOWNLOAD_RESIDUE(R.string.junk_downloads, false),
    OBSOLETE(R.string.junk_obsolete, false),
}

data class JunkGroup(
    val category: JunkCategory,
    val items: List<JunkItem>,
) {
    val totalBytes: Long get() = items.sumOf { it.sizeBytes }
    val count: Int get() = items.size
}

data class JunkItem(
    val path: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val category: JunkCategory,
)
