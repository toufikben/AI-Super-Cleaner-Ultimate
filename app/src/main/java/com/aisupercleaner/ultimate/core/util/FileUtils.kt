package com.aisupercleaner.ultimate.core.util

import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import kotlinx.coroutines.CancellationException

object FileUtils {
    private val random = SecureRandom()

    fun safeLength(file: File): Long = runCatching { file.length() }.getOrDefault(0L)

    fun safeDelete(file: File): Boolean = runCatching {
        if (file.isDirectory) file.deleteRecursively() else file.delete()
    }.getOrDefault(false)

    /** Best-effort overwrite/delete; flash storage may retain prior physical pages. */
    fun shredFile(file: File, passes: Int = 3): Boolean {
        return try {
            if (!file.exists() || !file.isFile) return false
            val length = file.length()
            if (length == 0L) return file.delete()
        val safePasses = passes.coerceIn(1, 7)
        java.io.RandomAccessFile(file, "rws").use { raf ->
            val buffer = ByteArray(64 * 1024)
            repeat(safePasses) {
                raf.seek(0)
                var remaining = length
                while (remaining > 0) {
                    random.nextBytes(buffer)
                    val toWrite = minOf(buffer.size.toLong(), remaining).toInt()
                    raf.write(buffer, 0, toWrite)
                    remaining -= toWrite
                }
                raf.fd.sync()
            }
            buffer.fill(0)
        }
        file.delete()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            false
        }
    }

    fun computeSha256(file: File, bufferSize: Int = 64 * 1024): String? = runCatching {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered(bufferSize).use { input ->
            val buf = ByteArray(bufferSize)
            var read: Int
            while (input.read(buf).also { read = it } > 0) digest.update(buf, 0, read)
            buf.fill(0)
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }.getOrNull()

    fun getAvailableStorageBytes(root: File): Long = runCatching { root.usableSpace }.getOrDefault(0L)
}
