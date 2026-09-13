package com.aisupercleaner.ultimate.data.forecast

import com.aisupercleaner.ultimate.data.history.HistoryRepository
import com.aisupercleaner.ultimate.data.storage.StorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class StorageForecast(
    val dailyUsageBytes: Long,
    val daysUntilFull: Int?,
    val estimatedFullDateMs: Long?,
    val recommendationRes: Int,
    val confidence: Confidence,
) {
    enum class Confidence { LOW, MEDIUM, HIGH }
}

@Singleton
class StorageForecaster @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val storageRepository: StorageRepository,
) {

    suspend fun forecast(): StorageForecast = withContext(Dispatchers.IO) {
        val since = System.currentTimeMillis() - 14L * 24 * 60 * 60 * 1000
        val freed = historyRepository.sumFreedSince(since)
        val info = storageRepository.getStorageInfo()
        val free = info.freeBytes

        val dailyFreed = freed / 14L
        val dailyNetGrowth = (dailyFreed * 0.3f).toLong()

        if (dailyNetGrowth <= 0) {
            return@withContext StorageForecast(0, null, null, 0, StorageForecast.Confidence.LOW)
        }

        val days = (free / dailyNetGrowth).toInt().coerceAtLeast(0)
        val confidence = when {
            freed > 0 && days < 365 -> StorageForecast.Confidence.HIGH
            freed > 0 -> StorageForecast.Confidence.MEDIUM
            else -> StorageForecast.Confidence.LOW
        }

        StorageForecast(
            dailyUsageBytes = dailyNetGrowth,
            daysUntilFull = days,
            estimatedFullDateMs = System.currentTimeMillis() + days * 24L * 60 * 60 * 1000,
            recommendationRes = when {
                days < 7 -> com.aisupercleaner.ultimate.R.string.forecast_critical
                days < 30 -> com.aisupercleaner.ultimate.R.string.forecast_warning
                days < 90 -> com.aisupercleaner.ultimate.R.string.forecast_attention
                else -> com.aisupercleaner.ultimate.R.string.forecast_fine
            },
            confidence = confidence,
        )
    }
}
