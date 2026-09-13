package com.aisupercleaner.ultimate.data.history

import com.aisupercleaner.ultimate.data.local.database.dao.HistoryDao
import com.aisupercleaner.ultimate.data.local.database.entity.CleanupHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class HistoryEntry(
    val id: Long,
    val timestamp: Long,
    val freedBytes: Long,
    val deletedCount: Int,
    val failedCount: Int,
    val source: Source,
    val durationMs: Long,
) {
    enum class Source { JUNK, DUPLICATES, VAULT, AUTO, SHREDDER, UNKNOWN }
}

@Singleton
class HistoryRepository @Inject constructor(private val dao: HistoryDao) {

    fun observeAll(): Flow<List<HistoryEntry>> = dao.observeAll().map { list -> list.map { it.toModel() } }
    fun observeRecent(limit: Int = 30): Flow<List<HistoryEntry>> = dao.observeRecent(limit).map { it.map { e -> e.toModel() } }

    suspend fun record(
        freedBytes: Long,
        deletedCount: Int,
        failedCount: Int,
        source: HistoryEntry.Source,
        durationMs: Long,
    ) = withContext(Dispatchers.IO) {
        dao.insert(
            CleanupHistoryEntity(
                timestamp = System.currentTimeMillis(),
                freedBytes = freedBytes,
                deletedCount = deletedCount,
                failedCount = failedCount,
                source = source.name,
                durationMs = durationMs,
            )
        )
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) { dao.clearAll() }
    suspend fun sumFreedSince(since: Long): Long = withContext(Dispatchers.IO) { dao.sumFreedSince(since) ?: 0L }

    private fun CleanupHistoryEntity.toModel() = HistoryEntry(
        id = id, timestamp = timestamp, freedBytes = freedBytes,
        deletedCount = deletedCount, failedCount = failedCount,
        source = runCatching { HistoryEntry.Source.valueOf(source) }.getOrDefault(HistoryEntry.Source.UNKNOWN),
        durationMs = durationMs,
    )
}
