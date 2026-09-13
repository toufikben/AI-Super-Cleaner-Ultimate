package com.aisupercleaner.ultimate.data.repository

import com.aisupercleaner.ultimate.data.local.database.dao.DuplicateDao
import com.aisupercleaner.ultimate.data.local.database.entity.DuplicateGroupEntity
import com.aisupercleaner.ultimate.data.local.database.entity.DuplicateItemEntity
import com.aisupercleaner.ultimate.data.scanner.duplicates.DuplicateGroup
import com.aisupercleaner.ultimate.data.scanner.duplicates.DuplicateItem
import com.aisupercleaner.ultimate.data.scanner.duplicates.DuplicateScanResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DuplicateRepository @Inject constructor(private val dao: DuplicateDao) {

    fun observeSavedGroups(): Flow<List<DuplicateGroup>> = dao.observeGroups().map { entities ->
        entities.map { entity ->
            val items = dao.getItemsForGroup(entity.id).map { DuplicateItem(it.path, it.sizeBytes, it.lastModified, it.isImage) }
            DuplicateGroup(
                id = entity.fingerprint,
                items = items,
                kind = runCatching { DuplicateGroup.Kind.valueOf(entity.kind) }.getOrDefault(DuplicateGroup.Kind.EXACT),
            )
        }
    }

    suspend fun saveResult(result: DuplicateScanResult) = withContext(Dispatchers.IO) {
        val pairs = result.groups.map { group ->
            DuplicateGroupEntity(
                fingerprint = group.id,
                kind = group.kind.name,
                wastedBytes = group.wastedBytes,
                itemCount = group.count,
                scannedAt = System.currentTimeMillis(),
            ) to group.items.map { DuplicateItemEntity(0, 0, it.path, it.sizeBytes, it.lastModified, it.isImage) }
        }
        dao.replaceAll(pairs)
    }

    suspend fun deletePaths(paths: List<String>) = withContext(Dispatchers.IO) { dao.deleteItemsByPaths(paths) }
    suspend fun clearAll() = withContext(Dispatchers.IO) { dao.clearAll() }
    suspend fun totalWastedBytes(): Long = withContext(Dispatchers.IO) { dao.totalWastedBytes() ?: 0L }
}
