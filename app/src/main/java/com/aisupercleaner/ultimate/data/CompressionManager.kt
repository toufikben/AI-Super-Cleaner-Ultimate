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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

sealed interface CompressionResult {
    data class Success(val outputUri: Uri, val originalBytes: Long, val outputBytes: Long) : CompressionResult
    data class Failure(val message: String) : CompressionResult
}

enum class ImagePreset(val quality: Int) { SMALL(55), BALANCED(75), HIGH_QUALITY(90) }

@OptIn(markerClass = [UnstableApi::class])
class CompressionManager(private val context: Context, private val resolver: ContentResolver, private val dao: StorageDao) {
    private val videoCompressor = VideoCompressor(context)

    suspend fun compressVideoCopy(input: Uri, preset: VideoPreset, originalBytes: Long): CompressionResult = withContext(Dispatchers.IO) {
        try {
            val temp = videoCompressor.compressCopy(input, preset)
            val output = exportFile(temp, "compressed_video_${System.currentTimeMillis()}.mp4", "video/mp4", MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            temp.delete()
            dao.insertCompressionHistory(CompressionHistoryEntity(inputUri = input.toString(), outputUri = output.toString(), mediaType = "video", preset = preset.name, originalBytes = originalBytes, outputBytes = querySize(output), status = "completed", createdAtEpochMillis = System.currentTimeMillis()))
            CompressionResult.Success(output, originalBytes, querySize(output))
        } catch (e: Exception) {
            dao.insertCompressionHistory(CompressionHistoryEntity(inputUri = input.toString(), outputUri = null, mediaType = "video", preset = preset.name, originalBytes = originalBytes, outputBytes = 0L, status = "failed", createdAtEpochMillis = System.currentTimeMillis()))
            CompressionResult.Failure("Video compression failed: ${e.message ?: "unsupported codec or inaccessible file"}")
        }
    }

    suspend fun compressImageCopy(input: Uri, preset: ImagePreset, originalBytes: Long): CompressionResult = withContext(Dispatchers.IO) {
        var bitmap: Bitmap? = null
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(input)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            val sample = calculateSample(bounds.outWidth, bounds.outHeight)
            val options = BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = Bitmap.Config.RGB_565 }
            bitmap = resolver.openInputStream(input)?.use { BitmapFactory.decodeStream(it, null, options) } ?: throw IllegalStateException("Image is inaccessible")
            val temp = File.createTempFile("compressed_image_", ".jpg", context.cacheDir)
            temp.outputStream().use { output -> if (!bitmap!!.compress(Bitmap.CompressFormat.JPEG, preset.quality, output)) throw IllegalStateException("Image codec failed") }
            val output = exportFile(temp, "compressed_image_${System.currentTimeMillis()}.jpg", "image/jpeg", MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            temp.delete()
            val outputBytes = querySize(output)
            dao.insertCompressionHistory(CompressionHistoryEntity(inputUri = input.toString(), outputUri = output.toString(), mediaType = "image", preset = preset.name, originalBytes = originalBytes, outputBytes = outputBytes, status = "completed", createdAtEpochMillis = System.currentTimeMillis()))
            CompressionResult.Success(output, originalBytes, outputBytes)
        } catch (e: Exception) {
            dao.insertCompressionHistory(CompressionHistoryEntity(inputUri = input.toString(), outputUri = null, mediaType = "image", preset = preset.name, originalBytes = originalBytes, outputBytes = 0L, status = "failed", createdAtEpochMillis = System.currentTimeMillis()))
            CompressionResult.Failure("Image compression failed: ${e.message ?: "inaccessible image"}")
        } finally { bitmap?.recycle() }
    }

    private fun exportFile(temp: File, displayName: String, mimeType: String, collection: Uri): Uri {
        val values = ContentValues().apply { put(MediaStore.MediaColumns.DISPLAY_NAME, displayName); put(MediaStore.MediaColumns.MIME_TYPE, mimeType); if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { put(MediaStore.MediaColumns.RELATIVE_PATH, "Movies/AI Super Cleaner"); put(MediaStore.MediaColumns.IS_PENDING, 1) } }
        val uri = resolver.insert(collection, values) ?: throw IllegalStateException("MediaStore could not create output")
        try {
            resolver.openOutputStream(uri)?.use { output -> temp.inputStream().use { it.copyTo(output) } } ?: throw IllegalStateException("Output stream unavailable")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            return uri
        } catch (e: Exception) { resolver.delete(uri, null, null); throw e }
    }

    private fun querySize(uri: Uri): Long = resolver.query(uri, arrayOf(MediaStore.MediaColumns.SIZE), null, null, null)?.use { cursor -> if (cursor.moveToFirst()) cursor.getLong(0) else 0L } ?: 0L
    private fun calculateSample(width: Int, height: Int): Int { var sample = 1; while (width / sample > 4096 || height / sample > 4096) sample *= 2; return sample }
}
