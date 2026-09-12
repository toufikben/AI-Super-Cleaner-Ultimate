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
import java.io.IOException
import kotlin.coroutines.coroutineContext
import kotlin.math.abs

data class DuplicateGroup(val hash: String, val files: List<FileMetadataEntity>, val recoverableBytes: Long)
data class SimilarGroup(val anchorUri: String, val files: List<FileMetadataEntity>, val similarity: Int)
data class MediaAnalysisReport(val duplicateGroups: List<DuplicateGroup>, val similarGroups: List<SimilarGroup>, val blurredFiles: List<MediaFinding>, val screenshotFiles: List<MediaFinding>)

class DuplicateEngine(private val resolver: ContentResolver, private val dao: StorageDao) {
    suspend fun analyze(): MediaAnalysisReport = withContext(Dispatchers.IO) {
        val files = dao.allFiles()
        val exactCandidates = DuplicateGrouping.candidateGroups(files)
        val hashToFiles = linkedMapOf<String, MutableList<FileMetadataEntity>>()
        for (candidateGroup in exactCandidates) {
            for (file in candidateGroup) {
                coroutineContext.ensureActive()
                val hash = file.contentHash ?: sha256(file.uri) ?: continue
                if (file.contentHash == null) dao.updateAnalysis(file.uri, hash, file.perceptualHash, file.blurScore, file.isScreenshot, ScanCachePolicy.CURRENT_ANALYSIS_VERSION)
                hashToFiles.getOrPut(hash) { mutableListOf() }.add(file.copy(contentHash = hash))
            }
        }
        val duplicateGroups = DuplicateGrouping.exactGroups(hashToFiles.values.flatten())
        val imageFiles = files.filter { it.mediaType == "image" }
        val exactUris = duplicateGroups.asSequence().flatMap { it.files.asSequence() }.map { it.uri }.toSet()
        val imageHashes = mutableListOf<Pair<FileMetadataEntity, Long>>()
        val blurred = mutableListOf<MediaFinding>()
        val screenshots = mutableListOf<MediaFinding>()
        val semaphore = Semaphore(2)
        imageFiles.chunked(32).forEach { chunk ->
            coroutineContext.ensureActive()
            coroutineScope {
                chunk.map { file -> async(Dispatchers.IO) { semaphore.withPermit { file to if (ScanCachePolicy.analysisIsCurrent(file) && file.perceptualHash != null && file.blurScore != null) null else analyzeImage(file.uri) } } }.awaitAll().forEach { (file, analysis) ->
                    val screenshotFinding = if (analysis != null) MediaClassification.screenshotFinding(file) else if (file.isScreenshot) MediaFinding(file, "Potential screenshot", 90, "Previously matched a screenshot filename or path pattern.") else null
                    val isScreenshot = screenshotFinding != null
                    val perceptualHash = analysis?.hash ?: file.perceptualHash
                    val blurScore = analysis?.blurScore ?: file.blurScore
                    if (analysis != null) dao.updateAnalysis(file.uri, file.contentHash, analysis.hash, analysis.blurScore, isScreenshot, ScanCachePolicy.CURRENT_ANALYSIS_VERSION)
                    if (perceptualHash != null && blurScore != null) {
                        imageHashes += file.copy(perceptualHash = perceptualHash, blurScore = blurScore, isScreenshot = isScreenshot) to perceptualHash.toULong(16).toLong()
                        MediaClassification.blurFinding(file, blurScore)?.let { blurred += it }
                    }
                    screenshotFinding?.let { screenshots += it }
                }
            }
        }
        val similarGroups = buildSimilarGroups(imageHashes.filterNot { it.first.uri in exactUris })
        MediaAnalysisReport(duplicateGroups, similarGroups, blurred, screenshots)
    }

    private fun sha256(uri: String): String? = try {
        resolver.openInputStream(android.net.Uri.parse(uri))?.use { input ->
            ContentHasher.sha256(input)
        }
    } catch (_: IOException) {
        null
    } catch (_: SecurityException) {
        null
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
