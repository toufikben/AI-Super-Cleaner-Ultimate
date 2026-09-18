package com.aisupercleaner.ultimate.data.shredder

import com.aisupercleaner.ultimate.core.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShredderManager @Inject constructor() {

    suspend fun shred(file: File, passes: Int = 3, onProgress: (Int, Int) -> Unit = { _, _ -> }): ShredResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        val size = file.length()
        val success = FileUtils.shredFile(file, passes)
        ShredResult(file.absolutePath, if (success) size else 0L, passes, success, System.currentTimeMillis() - start)
    }

    suspend fun shredBatch(files: List<File>, passes: Int = 3, onProgress: (Int, Int) -> Unit = { _, _ -> }): List<ShredResult> = withContext(Dispatchers.IO) {
        files.mapIndexed { idx, file ->
            currentCoroutineContext().ensureActive()
            onProgress(idx, files.size)
            val result = shred(file, passes)
            onProgress(idx + 1, files.size)
            result
        }
    }

    enum class Level(val passes: Int, val title: String, val description: String) {
        FAST(1, "سريع", "كتابة عشوائية واحدة — للأداء"),
        SECURE(3, "آمن", "معيار DoD 5220.22-M — موصى به"),
        MILITARY(7, "عسكري", "7 مرات كتابة — أقصى أمان"),
    }
}

data class ShredResult(
    val path: String,
    val bytesShredded: Long,
    val passes: Int,
    val success: Boolean,
    val durationMs: Long,
)
