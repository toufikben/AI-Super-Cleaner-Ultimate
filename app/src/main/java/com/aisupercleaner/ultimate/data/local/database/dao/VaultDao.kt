package com.aisupercleaner.ultimate.data.local.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aisupercleaner.ultimate.data.local.database.entity.VaultFileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {

    @Query("SELECT * FROM vault_files ORDER BY importedAt DESC")
    fun observeAll(): Flow<List<VaultFileEntity>>

    @Query("SELECT * FROM vault_files")
    suspend fun getAll(): List<VaultFileEntity>

    @Query("SELECT * FROM vault_files WHERE id = :id")
    suspend fun getById(id: Long): VaultFileEntity?

    @Query("SELECT * FROM vault_files WHERE encryptedName = :name LIMIT 1")
    suspend fun getByEncryptedName(name: String): VaultFileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: VaultFileEntity): Long

    @Delete
    suspend fun delete(entity: VaultFileEntity)

    @Query("DELETE FROM vault_files WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM vault_files WHERE encryptedName = :name")
    suspend fun deleteByEncryptedName(name: String)

    @Query("SELECT COUNT(*) FROM vault_files")
    suspend fun count(): Int

    @Query("SELECT COALESCE(SUM(sizeBytes), 0) FROM vault_files")
    suspend fun totalSizeBytes(): Long
    @Query("DELETE FROM vault_files")
    suspend fun clearAll()
}
