package com.aisupercleaner.ultimate.data

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.BufferedInputStream
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext
import kotlin.math.abs

data class DuplicateGroup(val hash: String, val files: List<FileMetadataEntity>, val recoverableBytes: Long)
data class SimilarGroup(val anchorUri: String, val files: List<FileMetadataEntity>, val similarity: Int)
data class MediaAnalysisReport(val duplicateGroups: List<DuplicateGroup>, val similarGroups: List<SimilarGroup>, val blurredFiles: List<FileMetadataEntity>, val screenshotFiles: List<FileMetadataEntity>)

class DuplicateEngine(private val resolver: ContentResolver, private val dao: StorageDao) {
    suspend fun analyze(): MediaAnalysisReport = withContext(Dispatchers.IO) {
        val files = dao.allFiles()
        val exactCandidates = files.filter { it.sizeBytes > 0 }.groupBy { it.mediaType to it.sizeBytes }.values.filter { it.size > 1 }
        val hashToFiles = linkedMapOf<String, MutableList<FileMetadataEntity>>()
        for (candidateGroup in exactCandidates) {
            for (file in candidateGroup) {
                coroutineContext.ensureActive()
                val hash = sha256(file.uri) ?: continue
                dao.updateAnalysis(file.uri, hash, file.perceptualHash, file.blurScore, file.isScreenshot)
                hashToFiles.getOrPut(hash) { mutableListOf() }.add(file.copy(contentHash = hash))
            }
        }
        val duplicateGroups = hashToFiles.values.filter { it.size > 1 }.map { group -> DuplicateGroup(group.first().contentHash ?: "", group, group.drop(1).sumOf { it.sizeBytes }) }
        val imageFiles = files.filter { it.mediaType == "image" }
        val imageHashes = mutableListOf<Pair<FileMetadataEntity, Long>>()
        val blurred = mutableListOf<FileMetadataEntity>()
        val screenshots = mutableListOf<FileMetadataEntity>()
        val semaphore = Semaphore(2)
        imageFiles.chunked(32).forEach { chunk ->
            coroutineContext.ensureActive()
            coroutineScope {
                chunk.map { file -> async(Dispatchers.IO) { semaphore.withPermit { file to analyzeImage(file.uri) } } }.awaitAll().forEach { (file, analysis) ->
                    val isScreenshot = screenshotName(file.displayName)
                    if (analysis != null) {
                        dao.updateAnalysis(file.uri, file.contentHash, analysis.hash, analysis.blurScore, isScreenshot)
                        imageHashes += file.copy(perceptualHash = analysis.hash, blurScore = analysis.blurScore, isScreenshot = isScreenshot) to analysis.hash.toULong(16).toLong()
                        if (analysis.blurScore < 0.10) blurred += file.copy(blurScore = analysis.blurScore)
                    }
                    if (isScreenshot) screenshots += file.copy(isScreenshot = true)
                }
            }
        }
        val similarGroups = buildSimilarGroups(imageHashes)
        MediaAnalysisReport(duplicateGroups, similarGroups, blurred, screenshots)
    }

    private fun sha256(uri: String): String? = resolver.openInputStream(android.net.Uri.parse(uri))?.use { input ->
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(64 * 1024)
        while (true) { val read = input.read(buffer); if (read <= 0) break; digest.update(buffer, 0, read) }
        digest.digest().joinToString("") { "%02x".format(it) }
    }

    private data class ImageAnalysis(val hash: String, val blurScore: Double)

    private fun analyzeImage(uri: String): ImageAnalysis? {
        val bitmap = resolver.openInputStream(android.net.Uri.parse(uri))?.use { stream -> BitmapFactory.decodeStream(BufferedInputStream(stream), null, BitmapFactory.Options().apply { inSampleSize = 8; inPreferredConfig = Bitmap.Config.RGB_565 }) } ?: return null
        if (bitmap.width < 8 || bitmap.height < 8) { bitmap.recycle(); return null }
        val small = Bitmap.createScaledBitmap(bitmap, 8, 8, true)
        val pixels = IntArray(64); small.getPixels(pixels, 0, 8, 0, 0, 8, 8)
        val gray = pixels.map { (it shr 16 and 0xff) * 299 + (it shr 8 and 0xff) * 587 + (it and 0xff) * 114 }
        val average = gray.average()
        val hash = gray.fold(0L) { acc, value -> (acc shl 1) or if (value >= average) 1L else 0L }.toULong().toString(16).padStart(16, '0')
        val blurScore = edgeEnergy(gray)
        small.recycle(); bitmap.recycle()
        return ImageAnalysis(hash, blurScore)
    }

    private fun edgeEnergy(gray: List<Int>): Double {
        var total = 0.0
        for (y in 0 until 7) for (x in 0 until 7) total += abs(gray[y * 8 + x] - gray[y * 8 + x + 1]) + abs(gray[y * 8 + x] - gray[(y + 1) * 8 + x])
        return (total / (49.0 * 510.0)).coerceIn(0.0, 1.0)
    }

    private fun screenshotName(name: String): Boolean { val normalized = name.lowercase(); return normalized.contains("screenshot") || normalized.contains("screen_shot") || normalized.contains("screen-shot") || normalized.contains("screen shot") }

    private fun buildSimilarGroups(items: List<Pair<FileMetadataEntity, Long>>): List<SimilarGroup> {
        val used = mutableSetOf<String>(); val output = mutableListOf<SimilarGroup>()
        for ((anchor, hash) in items) {
            if (anchor.uri in used) continue
            val matches = items.filter { it.first.uri != anchor.uri && it.first.uri !in used && java.lang.Long.bitCount(hash xor it.second) <= 8 }
            if (matches.isNotEmpty()) { val files = listOf(anchor) + matches.map { it.first }; files.forEach { used += it.uri }; output += SimilarGroup(anchor.uri, files, 100 - (java.lang.Long.bitCount(hash xor matches.first().second) * 100 / 64)) }
        }
        return output
    }
}
