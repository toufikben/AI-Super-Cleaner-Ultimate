package com.aisupercleaner.ultimate.data.scanner

import android.content.Context
import android.os.Environment
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
class LargeFileScanner @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    data class LargeFile(
        val path: String,
        val sizeBytes: Long,
        val lastModified: Long,
        val extension: String,
        val category: Category,
    ) {
        val name: String get() = path.substringAfterLast('/')
        val folder: String get() = path.substringBeforeLast('/', "")
        val formattedSize: String get() = com.aisupercleaner.ultimate.core.util.Formatter.formatBytes(sizeBytes)
        enum class Category { VIDEO, AUDIO, IMAGE, ARCHIVE, DOCUMENT, APK, OTHER }
    }

    data class Filter(
        val minSizeBytes: Long = 100L * 1024 * 1024,
        val categories: Set<LargeFile.Category> = LargeFile.Category.entries.toSet(),
        val olderThanDays: Int? = null,
    )

    data class Progress(val scanned: Int, val found: Int, val currentPath: String = "")

    sealed interface Event {
        data class Progress(val progress: LargeFileScanner.Progress) : Event
        data class Done(val files: List<LargeFile>, val totalBytes: Long) : Event
        data class Error(val throwable: Throwable) : Event
    }

    fun scan(filter: Filter = Filter()): Flow<Event> = flow {
        val roots = defaultRoots()
        val results = mutableListOf<LargeFile>()
        var scanned = 0
        val ageCutoff = filter.olderThanDays?.let { System.currentTimeMillis() - it * 24L * 60 * 60 * 1000 }

        for (root in roots) {
            if (!root.exists() || !root.canRead()) continue
            root.walkTopDown()
                .onEnter { true }
                .filter { it.isFile && it.canRead() }
                .forEach { file ->
                    scanned++
                    if (scanned % 200 == 0) emit(Event.Progress(Progress(scanned, results.size, file.absolutePath)))

                    val size = runCatching { file.length() }.getOrDefault(0L)
                    if (size < filter.minSizeBytes) return@forEach
                    if (ageCutoff != null && file.lastModified() > ageCutoff) return@forEach

                    val ext = file.extension.lowercase()
                    val category = categorize(ext)
                    if (category !in filter.categories) return@forEach

                    results += LargeFile(file.absolutePath, size, file.lastModified(), ext, category)
                }
        }

        val sorted = results.sortedByDescending { it.sizeBytes }
        emit(Event.Done(sorted, sorted.sumOf { it.sizeBytes }))
    }.flowOn(Dispatchers.IO)

    private fun categorize(ext: String): LargeFile.Category = when (ext) {
        "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "3gp", "m4v", "ts", "mpeg", "mpg" -> LargeFile.Category.VIDEO
        "mp3", "wav", "flac", "aac", "ogg", "m4a", "opus", "wma", "amr" -> LargeFile.Category.AUDIO
        "jpg", "jpeg", "png", "webp", "heic", "gif", "bmp", "tiff", "raw", "dng" -> LargeFile.Category.IMAGE
        "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "iso" -> LargeFile.Category.ARCHIVE
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "epub" -> LargeFile.Category.DOCUMENT
        "apk", "apks", "xapk", "aab" -> LargeFile.Category.APK
        else -> LargeFile.Category.OTHER
    }

    private fun defaultRoots(): List<File> {
        val ext = Environment.getExternalStorageDirectory()
        return listOf(
            File(ext, Environment.DIRECTORY_DOWNLOADS),
            File(ext, Environment.DIRECTORY_DCIM),
            File(ext, Environment.DIRECTORY_PICTURES),
            File(ext, Environment.DIRECTORY_MOVIES),
            File(ext, Environment.DIRECTORY_MUSIC),
            File(ext, Environment.DIRECTORY_DOCUMENTS),
            File(ext, Environment.DIRECTORY_PODCASTS),
            File(ext, Environment.DIRECTORY_RINGTONES),
            File(ext, "WhatsApp"),
            File(ext, "Telegram"),
            File(ext, "Android/media"),
        ).filter { it.exists() }
    }
}
