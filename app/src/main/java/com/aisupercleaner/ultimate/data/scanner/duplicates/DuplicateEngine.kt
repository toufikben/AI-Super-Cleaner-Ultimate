package com.aisupercleaner.ultimate.data.scanner.duplicates

import android.content.Context
import android.os.Environment
import com.aisupercleaner.ultimate.core.util.FileUtils
import com.aisupercleaner.ultimate.core.util.ImageHasher
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
class DuplicateEngine @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    data class Progress(
        val phase: Phase,
        val scanned: Int,
        val total: Int,
        val currentPath: String = "",
    ) {
        enum class Phase { ENUMERATING, HASHING_SIZE, HASHING_SHA, HASHING_IMAGE, DONE }
        val fraction: Float get() = if (total > 0) scanned.toFloat() / total else 0f
    }

    fun findDuplicates(
        includeImages: Boolean = true,
        roots: List<File> = defaultRoots(),
    ): Flow<Event> = flow {
        val start = System.currentTimeMillis()

        emit(Event.Progress(Progress(Progress.Phase.ENUMERATING, 0, 0)))
        val allFiles = mutableListOf<File>()
        for (root in roots) {
            if (!root.exists() || !root.canRead()) continue
            root.walkTopDown()
                .onEnter { true }
                .filter { it.isFile && it.length() >= MIN_FILE_SIZE && it.canRead() }
                .forEach { allFiles += it }
        }

        if (allFiles.isEmpty()) {
            emit(Event.Done(DuplicateScanResult(emptyList(), 0, 0)))
            return@flow
        }

        val total = allFiles.size
        emit(Event.Progress(Progress(Progress.Phase.ENUMERATING, total, total)))

        emit(Event.Progress(Progress(Progress.Phase.HASHING_SIZE, 0, total)))
        val bySize = allFiles.groupBy { it.length() }
            .filterValues { it.size > 1 }
            .values.flatten()

        if (bySize.isEmpty()) {
            emit(Event.Done(DuplicateScanResult(emptyList(), total, System.currentTimeMillis() - start)))
            return@flow
        }

        val exactGroups = mutableMapOf<String, MutableList<DuplicateItem>>()
        var processed = 0

        for (file in bySize) {
            currentCoroutineContext().ensureActive()
            processed++
            if (processed % 5 == 0) {
                emit(Event.Progress(Progress(Progress.Phase.HASHING_SHA, processed, bySize.size, file.absolutePath)))
            }
            val hash = FileUtils.computeSha256(file) ?: continue
            exactGroups.getOrPut(hash) { mutableListOf() }.add(
                DuplicateItem(file.absolutePath, file.length(), file.lastModified(), isImage(file), sha256 = hash)
            )
        }

        val exactDuplicates = exactGroups
            .filterValues { it.size > 1 }
            .map { (hash, items) ->
                DuplicateGroup("sha_$hash", items.sortedBy { it.lastModified }, DuplicateGroup.Kind.EXACT)
            }

        val similarGroups = if (includeImages) {
            emit(Event.Progress(Progress(Progress.Phase.HASHING_IMAGE, 0, 0)))
            findSimilarImages(allFiles)
        } else emptyList()

        val allGroups = (exactDuplicates + similarGroups).sortedByDescending { it.wastedBytes }
        emit(Event.Done(DuplicateScanResult(allGroups, total, System.currentTimeMillis() - start)))
    }.flowOn(Dispatchers.IO)

    private suspend fun findSimilarImages(files: List<File>): List<DuplicateGroup> {
        val imageFiles = files.filter { isImage(it) }
        if (imageFiles.size < 2) return emptyList()

        val hashed = imageFiles.mapNotNull { file ->
            currentCoroutineContext().ensureActive()
            val hash = ImageHasher.dHash(file) ?: return@mapNotNull null
            DuplicateItem(file.absolutePath, file.length(), file.lastModified(), true, perceptualHash = hash)
        }

        val parent = IntArray(hashed.size) { it }
        fun find(x: Int): Int {
            var i = x
            while (parent[i] != i) { parent[i] = parent[parent[i]]; i = parent[i] }
            return i
        }
        fun union(a: Int, b: Int) {
            val ra = find(a); val rb = find(b)
            if (ra != rb) parent[rb] = ra
        }

        for (i in hashed.indices) {
            for (j in i + 1 until hashed.size) {
                val hi = hashed[i].perceptualHash ?: continue
                val hj = hashed[j].perceptualHash ?: continue
                if (ImageHasher.areSimilar(hi, hj, SIMILARITY_THRESHOLD)) union(i, j)
            }
        }

        return hashed.indices
            .groupBy { find(it) }
            .mapValues { (_, idx) -> idx.map { hashed[it] } }
            .filterValues { it.size > 1 }
            .map { (root, items) ->
                DuplicateGroup("img_$root", items.sortedBy { it.lastModified }, DuplicateGroup.Kind.SIMILAR)
            }
    }

    private fun defaultRoots(): List<File> {
        val ext = Environment.getExternalStorageDirectory()
        return listOf(
            File(ext, Environment.DIRECTORY_DCIM),
            File(ext, Environment.DIRECTORY_PICTURES),
            File(ext, Environment.DIRECTORY_DOWNLOADS),
            File(ext, Environment.DIRECTORY_DOCUMENTS),
            File(ext, Environment.DIRECTORY_MUSIC),
            File(ext, Environment.DIRECTORY_MOVIES),
            File(ext, "WhatsApp"),
            File(ext, "Telegram"),
            File(ext, "Android/media"),
        ).filter { it.exists() }
    }

    private fun isImage(file: File): Boolean = file.extension.lowercase() in IMAGE_EXTS

    companion object {
        private const val MIN_FILE_SIZE = 4096L
        private const val SIMILARITY_THRESHOLD = 10
        private val IMAGE_EXTS = setOf("jpg", "jpeg", "png", "webp", "heic", "heif", "gif", "bmp")
    }
}

sealed interface Event {
    data class Progress(val progress: DuplicateEngine.Progress) : Event
    data class Done(val result: DuplicateScanResult) : Event
    data class Error(val throwable: Throwable) : Event
}
