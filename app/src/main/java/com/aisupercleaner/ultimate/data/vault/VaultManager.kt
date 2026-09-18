package com.aisupercleaner.ultimate.data.vault

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import com.aisupercleaner.ultimate.core.util.FileUtils
import com.aisupercleaner.ultimate.data.local.database.dao.VaultDao
import com.aisupercleaner.ultimate.data.local.database.entity.VaultFileEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Collections
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vaultDao: VaultDao,
) {
    private val vaultDir: File by lazy { File(context.filesDir, "vault").apply { if (!exists()) mkdirs() } }
    private val tempDir: File by lazy { File(context.cacheDir, "vault_temp").apply { if (!exists()) mkdirs() } }
    private val masterKey: MasterKey by lazy { MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build() }
    // Preview/export temp files are reserved while in use so cleanup cannot race a reader.
    private val protectedTempFiles = Collections.synchronizedSet(mutableSetOf<String>())

    fun observeVaultFiles(): Flow<List<VaultFile>> = vaultDao.observeAll().map { entities -> entities.map { it.toModel() } }

    suspend fun repairOrphans() = withContext(Dispatchers.IO) { reconcileOrphans() }
    suspend fun getCount(): Int = withContext(Dispatchers.IO) { vaultDao.count() }
    suspend fun getTotalSize(): Long = withContext(Dispatchers.IO) { vaultDao.totalSizeBytes() }

    suspend fun importFile(sourceUri: Uri, deleteOriginal: Boolean = false): Result<VaultFile> = withContext(Dispatchers.IO) {
        try {
            val (originalName, queriedSize) = queryFileInfo(sourceUri)
            val mimeType = context.contentResolver.getType(sourceUri) ?: "application/octet-stream"
            val encryptedName = "${UUID.randomUUID()}.enc"
            val targetFile = File(vaultDir, encryptedName)
            val encryptedFile = EncryptedFile.Builder(context, targetFile, masterKey, EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB).build()
            var copiedBytes = 0L
            try {
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    encryptedFile.openFileOutput().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            copiedBytes += read
                        }
                    }
                } ?: error("Cannot open source file")
            } catch (cancelled: CancellationException) {
                FileUtils.shredFile(targetFile, passes = 1)
                throw cancelled
            } catch (failure: Throwable) {
                FileUtils.shredFile(targetFile, passes = 1)
                throw failure
            }
            val entity = VaultFileEntity(
                originalName = originalName, encryptedName = encryptedName, mimeType = mimeType,
                sizeBytes = queriedSize ?: copiedBytes, importedAt = System.currentTimeMillis(),
                isImage = mimeType.startsWith("image/"), isVideo = mimeType.startsWith("video/"),
            )
            val id = vaultDao.insert(entity)
            if (deleteOriginal && queriedSize != null) deleteSourceIfStillTheSame(sourceUri, queriedSize)
            Result.success(entity.copy(id = id).toModel())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Throwable) {
            Result.failure(failure)
        }
    }

    suspend fun decryptToTemp(fileId: Long): Result<File> = withContext(Dispatchers.IO) {
        try {
            val entity = vaultDao.getById(fileId) ?: error("Vault file not found")
            val encrypted = File(vaultDir, entity.encryptedName)
            if (!encrypted.exists()) {
                vaultDao.deleteById(fileId)
                error("Encrypted file missing")
            }
            // Keep the display name out of the cache filename; only a random opaque name is persisted.
            val tempFile = File(tempDir, "${UUID.randomUUID()}.tmp")
            try {
                val encryptedFile = EncryptedFile.Builder(context, encrypted, masterKey, EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB).build()
                encryptedFile.openFileInput().use { input ->
                    FileOutputStream(tempFile).use { output -> input.copyTo(output, bufferSize = 64 * 1024) }
                }
                protectedTempFiles += tempFile.canonicalPath
                Result.success(tempFile)
            } catch (cancelled: CancellationException) {
                FileUtils.shredFile(tempFile, passes = 1)
                throw cancelled
            } catch (failure: Throwable) {
                FileUtils.shredFile(tempFile, passes = 1)
                throw failure
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Throwable) {
            Result.failure(failure)
        }
    }

    suspend fun exportFile(fileId: Long, destinationUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val temp = decryptToTemp(fileId).getOrThrow()
            try {
                context.contentResolver.openOutputStream(destinationUri)?.use { out -> temp.inputStream().use { input -> input.copyTo(out) } }
                    ?: error("Cannot open destination")
            } finally { releaseTemp(temp) }
            Result.success(Unit)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Throwable) {
            Result.failure(failure)
        }
    }

    suspend fun cleanupTemp(except: File? = null) = withContext(Dispatchers.IO) {
        cleanupTempNow(except)
    }

    /** Release a temp file after its consumer is done, then securely remove it. */
    suspend fun releaseTemp(file: File?) = withContext(Dispatchers.IO) {
        if (file != null) {
            protectedTempFiles.remove(file.canonicalPath)
            FileUtils.shredFile(file, passes = 1)
        }
    }

    /** Synchronous best-effort cleanup for ViewModel.onCleared; flash storage is not guaranteed erasable. */
    fun cleanupTempNow(except: File? = null) {
        val keep = except?.canonicalPath
        val protected = synchronized(protectedTempFiles) { protectedTempFiles.toSet() }
        tempDir.listFiles()?.forEach {
            if (it.canonicalPath == keep || it.canonicalPath in protected) return@forEach
            if (it.isFile) FileUtils.shredFile(it, passes = 1) else it.deleteRecursively()
        }
    }

    /** Clears vault content and decrypted temporary files, but never deletes encryption/authentication keys. */
    suspend fun clearAllData(): VaultDataClearResult = withContext(Dispatchers.IO) {
        val errors = mutableListOf<String>()
        val vaultOk = runCatching {
            vaultDir.listFiles()?.forEach { if (!it.deleteRecursively()) error("Could not delete vault file: ${it.name}") }
            vaultDao.clearAll()
        }.onFailure {
            if (it is CancellationException) throw it
            errors += "vault: ${it.message ?: it::class.simpleName}"
        }.isSuccess
        val tempOk = runCatching {
            protectedTempFiles.clear()
            tempDir.listFiles()?.forEach { if (it.isFile) FileUtils.shredFile(it, passes = 1) else it.deleteRecursively() }
        }.onFailure {
            if (it is CancellationException) throw it
            errors += "temp: ${it.message ?: it::class.simpleName}"
        }.isSuccess
        VaultDataClearResult(vaultCleared = vaultOk, tempCleared = tempOk, errors = errors)
    }

    suspend fun deleteFile(fileId: Long, shred: Boolean = true): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val entity = vaultDao.getById(fileId) ?: return@withContext Result.success(Unit)
            val encrypted = File(vaultDir, entity.encryptedName)
            val removed = !encrypted.exists() || if (shred) FileUtils.shredFile(encrypted, passes = 3) else encrypted.delete()
            if (!removed) error("Could not delete vault file")
            vaultDao.deleteById(fileId)
            Result.success(Unit)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Throwable) {
            Result.failure(failure)
        }
    }

    private fun queryFileInfo(uri: Uri): Pair<String, Long?> {
        var name = "unknown"; var size: Long? = null
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIdx >= 0) name = cursor.getString(nameIdx) ?: name
                if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) size = cursor.getLong(sizeIdx)
            }
        }
        return name to size
    }

    private fun deleteSourceIfStillTheSame(uri: Uri, importedSize: Long) {
        if (uri.scheme == "file") {
            uri.path?.let { source ->
                val file = File(source)
                if (file.exists() && file.length() == importedSize) file.delete()
            }
        } else {
            // A provider may replace a document between import and delete. Restrict deletion
            // to providers that expose a stable, queryable size; never delete on uncertainty.
            val (_, currentSize) = queryFileInfo(uri)
            if (currentSize != null && currentSize == importedSize) context.contentResolver.delete(uri, null, null)
        }
    }

    private suspend fun reconcileOrphans() {
        val entities = vaultDao.getAll()
        val known = entities.map { it.encryptedName }.toSet()
        entities.filter { !File(vaultDir, it.encryptedName).isFile }.forEach { vaultDao.deleteById(it.id) }
        vaultDir.listFiles()?.filter { it.isFile && it.name !in known }?.forEach { FileUtils.shredFile(it, passes = 1) }
    }

    private fun VaultFileEntity.toModel() = VaultFile(id, originalName, encryptedName, mimeType, sizeBytes, importedAt, isImage, isVideo)
}
