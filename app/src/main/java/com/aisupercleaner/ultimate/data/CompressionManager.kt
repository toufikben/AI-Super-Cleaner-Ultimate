package com.aisupercleaner.ultimate.data

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

sealed interface CompressionResult {
    data class Success(val outputUri: Uri, val originalBytes: Long, val outputBytes: Long) : CompressionResult
    data class Failure(val error: CompressionError, val message: String = error.userMessage) : CompressionResult
}

enum class ImagePreset(val quality: Int) { SMALL(55), BALANCED(75), HIGH_QUALITY(90) }

@OptIn(markerClass = [UnstableApi::class])
class CompressionManager(private val context: Context, private val resolver: ContentResolver, private val dao: StorageDao) {
    private val videoCompressor = VideoCompressor(context)

    suspend fun compressVideoCopy(input: Uri, preset: VideoPreset, originalBytes: Long): CompressionResult = withContext(Dispatchers.IO) {
        var temp: File? = null
        try {
            temp = videoCompressor.compressCopy(input, preset)
            val output = exportFile(temp, CompressionPolicy.exportName("video"), "video/mp4", MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            val outputBytes = querySize(output)
            dao.insertCompressionHistory(CompressionHistoryEntity(inputUri = input.toString(), outputUri = output.toString(), mediaType = "video", preset = preset.name, originalBytes = originalBytes, outputBytes = outputBytes, status = "completed", createdAtEpochMillis = System.currentTimeMillis()))
            CompressionResult.Success(output, originalBytes, outputBytes)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            recordFailure(input, "video", preset.name, originalBytes)
            CompressionResult.Failure(CompressionPolicy.classify(error))
        } finally {
            temp?.delete()
        }
    }

    suspend fun compressImageCopy(input: Uri, preset: ImagePreset, originalBytes: Long): CompressionResult = withContext(Dispatchers.IO) {
        var bitmap: Bitmap? = null
        var temp: File? = null
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(input)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw UnsupportedOperationException("unsupported image codec")
            val sample = calculateSample(bounds.outWidth, bounds.outHeight)
            val options = BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = Bitmap.Config.RGB_565 }
            bitmap = resolver.openInputStream(input)?.use { BitmapFactory.decodeStream(it, null, options) } ?: throw IllegalStateException("Image is inaccessible")
            resolver.openInputStream(input)?.use { bitmap = ImageOrientation.apply(bitmap!!, it) }
            temp = File.createTempFile("compressed_image_", ".jpg", context.cacheDir)
            temp.outputStream().use { output -> if (!bitmap!!.compress(Bitmap.CompressFormat.JPEG, preset.quality, output)) throw UnsupportedOperationException("image codec failed") }
            val output = exportFile(temp, CompressionPolicy.exportName("image"), "image/jpeg", MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            val outputBytes = querySize(output)
            dao.insertCompressionHistory(CompressionHistoryEntity(inputUri = input.toString(), outputUri = output.toString(), mediaType = "image", preset = preset.name, originalBytes = originalBytes, outputBytes = outputBytes, status = "completed", createdAtEpochMillis = System.currentTimeMillis()))
            CompressionResult.Success(output, originalBytes, outputBytes)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            recordFailure(input, "image", preset.name, originalBytes)
            CompressionResult.Failure(CompressionPolicy.classify(error))
        } finally {
            temp?.delete()
            bitmap?.recycle()
        }
    }

    private suspend fun recordFailure(input: Uri, mediaType: String, preset: String, originalBytes: Long) {
        dao.insertCompressionHistory(CompressionHistoryEntity(inputUri = input.toString(), outputUri = null, mediaType = mediaType, preset = preset, originalBytes = originalBytes, outputBytes = 0L, status = "failed", createdAtEpochMillis = System.currentTimeMillis()))
    }

    private fun exportFile(temp: File, displayName: String, mimeType: String, collection: Uri): Uri {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Movies/AI Super Cleaner")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(collection, values) ?: throw IllegalStateException("MediaStore could not create output")
        try {
            resolver.openOutputStream(uri)?.use { output -> temp.inputStream().use { it.copyTo(output) } } ?: throw IllegalStateException("Output stream unavailable")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            return uri
        } catch (error: Throwable) {
            resolver.delete(uri, null, null)
            throw error
        }
    }

    private fun querySize(uri: Uri): Long = resolver.query(uri, arrayOf(MediaStore.MediaColumns.SIZE), null, null, null)?.use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else 0L } ?: 0L
    private fun calculateSample(width: Int, height: Int): Int { var sample = 1; while (width / sample > 4096 || height / sample > 4096) sample *= 2; return sample }
}
