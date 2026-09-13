package com.aisupercleaner.ultimate.core.util

import java.io.File
import java.security.MessageDigest

object FileUtils {

    fun safeLength(file: File): Long = runCatching { file.length() }.getOrDefault(0L)

    fun safeDelete(file: File): Boolean = runCatching {
        if (file.isDirectory) file.deleteRecursively() else file.delete()
    }.getOrDefault(false)

    fun shredFile(file: File, passes: Int = 3): Boolean = runCatching {
        if (!file.exists() || !file.isFile) return@runCatching false
        val length = file.length()
        if (length == 0L) return@runCatching file.delete()

        java.io.RandomAccessFile(file, "rws").use { raf ->
            val buffer = ByteArray(64 * 1024)
            repeat(passes) {
                raf.seek(0)
                var remaining = length
                while (remaining > 0) {
                    java.security.SecureRandom().nextBytes(buffer)
                    val toWrite = minOf(buffer.size.toLong(), remaining).toInt()
                    raf.write(buffer, 0, toWrite)
                    remaining -= toWrite
                }
                raf.fd.sync()
            }
        }
        file.delete()
    }.getOrDefault(false)

    fun computeSha256(file: File, bufferSize: Int = 64 * 1024): String? = runCatching {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered(bufferSize).use { input ->
            val buf = ByteArray(bufferSize)
            var read: Int
            while (input.read(buf).also { read = it } > 0) {
                digest.update(buf, 0, read)
            }
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }.getOrNull()

    fun getAvailableStorageBytes(root: File): Long = runCatching { root.usableSpace }.getOrDefault(0L)
}
