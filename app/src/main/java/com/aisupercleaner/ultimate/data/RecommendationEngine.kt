package com.aisupercleaner.ultimate.data

import kotlin.math.roundToInt

data class Recommendation(
    val category: String,
    val reason: String,
    val confidencePercent: Int,
    val estimatedBytes: Long,
    val fileCount: Int,
    val recommendationScore: Int = 0,
    val storageImpactPercent: Int = 0,
    val safetyLevel: String = "review-only"
)

data class StorageHealth(
    val pressurePercent: Int,
    val cleanupPotentialBytes: Long,
    val healthScore: Int
)

data class SmartCleanupReport(
    val healthScore: Int,
    val usedBytes: Long,
    val availableBytes: Long,
    val potentialReviewBytes: Long,
    val recommendations: List<Recommendation>,
    val explanation: String,
    val health: StorageHealth = StorageHealth(0, potentialReviewBytes, healthScore)
)

object RecommendationScoring {
    fun pressurePercent(usedBytes: Long, availableBytes: Long): Int {
        val total = usedBytes.coerceAtLeast(0L) + availableBytes.coerceAtLeast(0L)
        return if (total == 0L) 0 else ((usedBytes.coerceAtLeast(0L).toDouble() / total) * 100.0).roundToInt().coerceIn(0, 100)
    }

    fun storageImpactPercent(bytes: Long, usedBytes: Long, availableBytes: Long): Int {
        val total = (usedBytes.coerceAtLeast(0L) + availableBytes.coerceAtLeast(0L)).coerceAtLeast(1L)
        return ((bytes.coerceAtLeast(0L).toDouble() / total) * 100.0).roundToInt().coerceIn(0, 100)
    }

    fun healthScore(pressurePercent: Int, cleanupPotentialBytes: Long, usedBytes: Long): Int {
        val pressurePenalty = (pressurePercent * 0.55).roundToInt()
        val reviewPenalty = (cleanupPotentialBytes.toDouble() / usedBytes.coerceAtLeast(1L) * 20.0).coerceAtMost(20.0).roundToInt()
        return (100 - pressurePenalty - reviewPenalty).coerceIn(0, 100)
    }
}

class RecommendationEngine(private val dao: StorageDao) {
    suspend fun analyze(usedBytes: Long, availableBytes: Long): SmartCleanupReport {
        val totalBytes = (usedBytes.coerceAtLeast(0L) + availableBytes.coerceAtLeast(0L)).coerceAtLeast(1L)
        val recommendations = buildList {
            val largeVideos = dao.aggregateLargeFiles("video", 500L * 1024L * 1024L)
            if (largeVideos.fileCount > 0) add(reviewRecommendation("Large videos", "Videos at or above 500 MB are worth reviewing because they have high storage impact; size alone does not mean they are unnecessary.", 92, largeVideos.totalBytes, largeVideos.fileCount, totalBytes))
            val largeImages = dao.aggregateLargeFiles("image", 20L * 1024L * 1024L)
            if (largeImages.fileCount > 0) add(reviewRecommendation("Large photos", "Photos at or above 20 MB may have lower-value copies or export versions. Review before selecting anything.", 82, largeImages.totalBytes, largeImages.fileCount, totalBytes))
            val screenshots = dao.aggregateScreenshots()
            if (screenshots.fileCount > 0) add(reviewRecommendation("Screenshots", "The filename indicates a screenshot. The app will never delete it without your explicit selection.", 88, screenshots.totalBytes, screenshots.fileCount, totalBytes))
        }.sortedByDescending { it.estimatedBytes }
        val potential = recommendations.sumOf { it.estimatedBytes }
        val pressure = RecommendationScoring.pressurePercent(usedBytes, availableBytes)
        val healthScore = RecommendationScoring.healthScore(pressure, potential, usedBytes)
        val health = StorageHealth(pressure, potential, healthScore)
        val explanation = if (recommendations.isEmpty()) "No high-confidence review categories were found from the indexed metadata." else "Storage pressure, classification confidence, and review-only storage impact are shown separately. These are not claims that files are junk."
        return SmartCleanupReport(healthScore, usedBytes, availableBytes, potential, recommendations, explanation, health)
    }

    private fun reviewRecommendation(category: String, reason: String, confidence: Int, bytes: Long, count: Int, totalBytes: Long) = Recommendation(
        category = category,
        reason = reason,
        confidencePercent = confidence,
        estimatedBytes = bytes,
        fileCount = count,
        recommendationScore = ((confidence + RecommendationScoring.storageImpactPercent(bytes, 0L, totalBytes)) / 2).coerceIn(0, 100),
        storageImpactPercent = ((bytes.toDouble() / totalBytes) * 100.0).roundToInt().coerceIn(0, 100),
        safetyLevel = "review-only"
    )
}
