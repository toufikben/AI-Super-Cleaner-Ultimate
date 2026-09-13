package com.aisupercleaner.ultimate.data.vault

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import com.aisupercleaner.ultimate.data.local.database.dao.VaultDao
import com.aisupercleaner.ultimate.data.local.database.entity.VaultFileEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vaultDao: VaultDao,
) {

    private val vaultDir: File by lazy {
        File(context.filesDir, "vault").apply { if (!exists()) mkdirs() }
    }

    private val tempDir: File by lazy {
        File(context.cacheDir, "vault_temp").apply { if (!exists()) mkdirs() }
    }

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    }

    fun observeVaultFiles(): Flow<List<VaultFile>> =
        vaultDao.observeAll().map { entities -> entities.map { it.toModel() } }

    suspend fun getCount(): Int = withContext(Dispatchers.IO) { vaultDao.count() }
    suspend fun getTotalSize(): Long = withContext(Dispatchers.IO) { vaultDao.totalSizeBytes() }

    suspend fun importFile(sourceUri: Uri, deleteOriginal: Boolean = false): Result<VaultFile> = withContext(Dispatchers.IO) {
        runCatching {
            val (originalName, originalSize) = queryFileInfo(sourceUri)
            val mimeType = context.contentResolver.getType(sourceUri) ?: "application/octet-stream"
            val isImage = mimeType.startsWith("image/")
            val isVideo = mimeType.startsWith("video/")

            val encryptedName = "${UUID.randomUUID()}.enc"
            val targetFile = File(vaultDir, encryptedName)

            val encryptedFile = EncryptedFile.Builder(
                context, targetFile, masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB,
            ).build()

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                encryptedFile.openFileOutput().use { output ->
                    input.copyTo(output, bufferSize = 64 * 1024)
                }
            } ?: error("Cannot open source file")

            val entity = VaultFileEntity(
                originalName = originalName,
                encryptedName = encryptedName,
                mimeType = mimeType,
                sizeBytes = originalSize,
                importedAt = System.currentTimeMillis(),
                isImage = isImage,
                isVideo = isVideo,
            )
            val id = vaultDao.insert(entity)

            if (deleteOriginal) runCatching { deleteSource(sourceUri) }

            entity.copy(id = id).toModel()
        }
    }

    suspend fun decryptToTemp(fileId: Long): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val entity = vaultDao.getById(fileId) ?: error("Vault file not found")
            val encrypted = File(vaultDir, entity.encryptedName)
            if (!encrypted.exists()) error("Encrypted file missing")

            val tempFile = File(tempDir, "${UUID.randomUUID()}_${entity.originalName}")
            val encryptedFile = EncryptedFile.Builder(
                context, encrypted, masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB,
            ).build()

            encryptedFile.openFileInput().use { input ->
                FileOutputStream(tempFile).use { output -> input.copyTo(output, bufferSize = 64 * 1024) }
            }
            tempFile
        }
    }

    suspend fun exportFile(fileId: Long, destinationUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val temp = decryptToTemp(fileId).getOrThrow()
            try {
                context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                    temp.inputStream().use { input -> input.copyTo(out) }
                } ?: error("Cannot open destination")
            } finally { temp.delete() }
            Unit
        }
    }

    suspend fun cleanupTemp() = withContext(Dispatchers.IO) { tempDir.listFiles()?.forEach { it.delete() } }

    suspend fun deleteFile(fileId: Long, shred: Boolean = true): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val entity = vaultDao.getById(fileId) ?: return@runCatching Unit
            val encrypted = File(vaultDir, entity.encryptedName)
            if (encrypted.exists()) {
                if (shred) com.aisupercleaner.ultimate.core.util.FileUtils.shredFile(encrypted, passes = 3)
                else encrypted.delete()
            }
            vaultDao.deleteById(fileId)
        }
    }

    private fun queryFileInfo(uri: Uri): Pair<String, Long> {
        var name = "unknown"; var size = 0L
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIdx >= 0) name = cursor.getString(nameIdx) ?: name
                if (sizeIdx >= 0) size = cursor.getLong(sizeIdx)
            }
        }
        return name to size
    }

    private fun deleteSource(uri: Uri) {
        if (uri.scheme == "file") uri.path?.let { File(it).delete() }
        else context.contentResolver.delete(uri, null, null)
    }

    private fun VaultFileEntity.toModel() = VaultFile(id, originalName, encryptedName, mimeType, sizeBytes, importedAt, isImage, isVideo)
}
