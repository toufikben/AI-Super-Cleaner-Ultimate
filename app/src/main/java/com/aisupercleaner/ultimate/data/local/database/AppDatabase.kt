package com.aisupercleaner.ultimate.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.aisupercleaner.ultimate.data.local.database.dao.DuplicateDao
import com.aisupercleaner.ultimate.data.local.database.dao.HistoryDao
import com.aisupercleaner.ultimate.data.local.database.dao.VaultDao
import com.aisupercleaner.ultimate.data.local.database.entity.CleanupHistoryEntity
import com.aisupercleaner.ultimate.data.local.database.entity.DuplicateGroupEntity
import com.aisupercleaner.ultimate.data.local.database.entity.DuplicateItemEntity
import com.aisupercleaner.ultimate.data.local.database.entity.VaultFileEntity

@Database(
    entities = [
        DuplicateGroupEntity::class,
        DuplicateItemEntity::class,
        VaultFileEntity::class,
        CleanupHistoryEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun duplicateDao(): DuplicateDao
    abstract fun vaultDao(): VaultDao
    abstract fun historyDao(): HistoryDao
}
