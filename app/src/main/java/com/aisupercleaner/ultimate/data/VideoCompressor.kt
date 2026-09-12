package com.aisupercleaner.ultimate.data

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
class VideoCompressor(private val context: Context) {
    suspend fun compressCopy(input: Uri, preset: VideoPreset): File = withContext(Dispatchers.Main) {
        val output = File(context.cacheDir, "compressed_${System.currentTimeMillis()}.mp4")
        try {
            suspendCancellableCoroutine { continuation ->
                val transformer = Transformer.Builder(context)
                    .setVideoMimeType(MimeTypes.VIDEO_H264)
                    .setAudioMimeType(MimeTypes.AUDIO_AAC)
                    .addListener(object : Transformer.Listener {
                        override fun onTransformationCompleted(inputMediaItem: MediaItem) {
                            if (continuation.isActive) continuation.resume(output)
                        }

                        override fun onTransformationError(inputMediaItem: MediaItem, exception: Exception) {
                            output.delete()
                            if (continuation.isActive) continuation.resumeWithException(exception)
                        }
                    })
                    .build()
                val mediaItem = EditedMediaItem.Builder(MediaItem.fromUri(input)).build()
                transformer.start(mediaItem, output.absolutePath)
                continuation.invokeOnCancellation {
                    transformer.cancel()
                    output.delete()
                }
            }
        } catch (error: Throwable) {
            output.delete()
            throw error
        }
    }
}

enum class VideoPreset(val targetFrameRate: Int) { SMALL(24), BALANCED(30), HIGH_QUALITY(30) }
