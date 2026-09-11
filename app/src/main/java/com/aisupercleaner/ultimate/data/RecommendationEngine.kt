package com.aisupercleaner.ultimate.data

import kotlin.math.roundToInt

data class Recommendation(
    val category: String,
    val reason: String,
    val confidencePercent: Int,
    val estimatedBytes: Long,
    val fileCount: Int
)

data class SmartCleanupReport(
    val healthScore: Int,
    val usedBytes: Long,
    val availableBytes: Long,
    val potentialReviewBytes: Long,
    val recommendations: List<Recommendation>,
    val explanation: String
)

class RecommendationEngine(private val dao: StorageDao) {
    suspend fun analyze(usedBytes: Long, availableBytes: Long): SmartCleanupReport {
        val recommendations = buildList {
            val largeVideos = dao.aggregateLargeFiles("video", 500L * 1024L * 1024L)
            if (largeVideos.fileCount > 0) add(Recommendation("Large videos", "Videos at or above 500 MB are worth reviewing because they have high storage impact; size alone does not mean they are unnecessary.", 92, largeVideos.totalBytes, largeVideos.fileCount))
            val largeImages = dao.aggregateLargeFiles("image", 20L * 1024L * 1024L)
            if (largeImages.fileCount > 0) add(Recommendation("Large photos", "Photos at or above 20 MB may have lower-value copies or export versions. Review before selecting anything.", 82, largeImages.totalBytes, largeImages.fileCount))
            val screenshots = dao.aggregateScreenshots()
            if (screenshots.fileCount > 0) add(Recommendation("Screenshots", "The filename indicates a screenshot. The app will never delete it without your explicit selection.", 88, screenshots.totalBytes, screenshots.fileCount))
        }.sortedByDescending { it.estimatedBytes }
        val potential = recommendations.sumOf { it.estimatedBytes }
        val usageRatio = if (usedBytes + availableBytes > 0) usedBytes.toDouble() / (usedBytes + availableBytes).toDouble() else 0.0
        val pressurePenalty = (usageRatio * 55.0).roundToInt()
        val reviewPenalty = (potential.toDouble() / (usedBytes.coerceAtLeast(1L)) * 20.0).coerceAtMost(20.0).roundToInt()
        val score = (100 - pressurePenalty - reviewPenalty).coerceIn(0, 100)
        val explanation = if (recommendations.isEmpty()) "No high-confidence review categories were found from the indexed metadata." else "The score reflects device storage pressure and high-confidence categories that are safe to review. It is not a claim that these files are junk."
        return SmartCleanupReport(score, usedBytes, availableBytes, potential, recommendations, explanation)
    }
}
