package com.aisupercleaner.ultimate.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.aisupercleaner.ultimate.data.local.database.entity.DuplicateGroupEntity
import com.aisupercleaner.ultimate.data.local.database.entity.DuplicateItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DuplicateDao {

    @Query("SELECT * FROM duplicate_groups ORDER BY wastedBytes DESC")
    fun observeGroups(): Flow<List<DuplicateGroupEntity>>

    @Query("SELECT * FROM duplicate_items WHERE groupId = :groupId ORDER BY lastModified ASC")
    suspend fun getItemsForGroup(groupId: Long): List<DuplicateItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: DuplicateGroupEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<DuplicateItemEntity>)

    @Transaction
    suspend fun replaceAll(groups: List<Pair<DuplicateGroupEntity, List<DuplicateItemEntity>>>) {
        clearAll()
        groups.forEach { (group, items) ->
            val groupId = insertGroup(group.copy(id = 0))
            insertItems(items.map { it.copy(id = 0, groupId = groupId) })
        }
    }

    @Query("DELETE FROM duplicate_groups")
    suspend fun clearAll()

    @Query("DELETE FROM duplicate_items WHERE path IN (:paths)")
    suspend fun deleteItemsByPaths(paths: List<String>)

    @Query("SELECT SUM(wastedBytes) FROM duplicate_groups")
    suspend fun totalWastedBytes(): Long?

    @Query("SELECT MAX(scannedAt) FROM duplicate_groups")
    suspend fun lastScanTime(): Long?
}
