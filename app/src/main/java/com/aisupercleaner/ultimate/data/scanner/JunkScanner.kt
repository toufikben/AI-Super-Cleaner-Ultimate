package com.aisupercleaner.ultimate.data.scanner

import android.content.Context
import android.os.Environment
import com.aisupercleaner.ultimate.core.util.FileUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JunkScanner @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun scan(): Flow<ScanResult> = flow {
        emit(ScanResult.Progress(ScanProgress(phase = ScanProgress.Phase.ENUMERATING)))

        val allGroups = mutableListOf<JunkGroup>()
        var totalScanned = 0

        for (category in JunkCategory.entries) {
            val items = scanCategory(category) { scanned -> totalScanned = scanned }
            if (items.isNotEmpty()) allGroups += JunkGroup(category, items)
        }

        emit(ScanResult.Progress(ScanProgress(phase = ScanProgress.Phase.FINALIZING)))
        emit(ScanResult.Completed(allGroups.sortedByDescending { it.totalBytes }))
    }.flowOn(Dispatchers.IO)

    private suspend fun scanCategory(
        category: JunkCategory,
        onProgress: (Int) -> Unit,
    ): List<JunkItem> {
        val roots = candidateRoots(category)
        val items = mutableListOf<JunkItem>()
        var counter = 0

        for (root in roots) {
            if (!root.exists() || !root.canRead()) continue
            root.walkTopDown()
                .onEnter { true }
                .forEach { file ->
                    counter++
                    if (counter % 50 == 0) {
                        onProgress(counter)
                    }
                    if (matchesCategory(file, category)) {
                        val size = FileUtils.safeLength(file)
                        if (size > 0) items += JunkItem(file.absolutePath, size, file.lastModified(), category)
                    }
                }
        }
        return items
    }

    private fun candidateRoots(category: JunkCategory): List<File> {
        val external = Environment.getExternalStorageDirectory()
        val appCache = context.cacheDir
        val appFiles = context.filesDir

        return when (category) {
            JunkCategory.CACHE -> listOf(File(appCache.parentFile, "cache"), File(context.externalCacheDir?.parentFile, "cache")).filterNotNull().distinct()
            JunkCategory.TEMP -> listOf(File("/data/local/tmp"), File(appCache, "tmp"), File(external, "tmp"), File(external, "temp"))
            JunkCategory.LOGS -> listOf(File(external, "logs"), File(external, "Log"), File(appFiles, "logs"))
            JunkCategory.THUMBNAILS -> listOf(File(external, "DCIM/.thumbnails"), File(external, "Pictures/.thumbnails"), File(external, ".thumbnails"))
            JunkCategory.EMPTY_FOLDERS -> listOf(File(external, "Download"), File(external, "Documents"))
            JunkCategory.APK_RESIDUE -> listOf(File(external, "Download"), File(external, "Android/data"))
            JunkCategory.DOWNLOAD_RESIDUE -> listOf(File(external, Environment.DIRECTORY_DOWNLOADS))
            JunkCategory.OBSOLETE -> listOf(File(external, ".Trash"), File(external, ".cache"))
        }
    }

    private fun matchesCategory(file: File, category: JunkCategory): Boolean {
        if (!file.isFile) {
            return category == JunkCategory.EMPTY_FOLDERS && file.isDirectory && isEmptyDir(file)
        }
        val name = file.name.lowercase()
        val ext = name.substringAfterLast('.', "")

        return when (category) {
            JunkCategory.CACHE -> ext in CACHE_EXTS || name.contains("cache")
            JunkCategory.TEMP -> ext in TEMP_EXTS || name.startsWith("tmp_") || name.endsWith(".tmp")
            JunkCategory.LOGS -> ext == "log" || name.endsWith(".log.1") || name.endsWith(".log.old")
            JunkCategory.THUMBNAILS -> name.startsWith(".thumb") || ext == "thumbdata"
            JunkCategory.EMPTY_FOLDERS -> false
            JunkCategory.APK_RESIDUE -> ext == "apk" && file.lastModified() < oneMonthAgo()
            JunkCategory.DOWNLOAD_RESIDUE -> ext in ARCHIVE_EXTS && file.lastModified() < oneWeekAgo()
            JunkCategory.OBSOLETE -> ext in OBSOLETE_EXTS
        }
    }

    private fun isEmptyDir(dir: File): Boolean = runCatching { dir.listFiles()?.isEmpty() == true }.getOrDefault(false)
    private fun oneMonthAgo() = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
    private fun oneWeekAgo() = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000

    companion object {
        private val CACHE_EXTS = setOf("cache", "cch")
        private val TEMP_EXTS = setOf("tmp", "temp", "bak", "old", "~")
        private val ARCHIVE_EXTS = setOf("zip", "rar", "7z", "tar", "gz")
        private val OBSOLETE_EXTS = setOf("thumbnails", "ds_store", "part", "crdownload")
    }
}

sealed interface ScanResult {
    data class Progress(val progress: ScanProgress) : ScanResult
    data class Completed(val groups: List<JunkGroup>) : ScanResult
    data class Error(val throwable: Throwable) : ScanResult
}
