package com.aisupercleaner.ultimate.data

import java.io.FileNotFoundException
import java.io.IOException
import java.nio.channels.ClosedChannelException

sealed interface CompressionError {
    val userMessage: String

    data object PermissionDenied : CompressionError { override val userMessage = "Permission to read or export this media is unavailable." }
    data object FileGone : CompressionError { override val userMessage = "The selected media is no longer available." }
    data object StorageFull : CompressionError { override val userMessage = "There is not enough free storage for the compressed copy." }
    data object UnsupportedFormat : CompressionError { override val userMessage = "This media format or codec is not supported on this device." }
    data object OutputUnavailable : CompressionError { override val userMessage = "The compressed copy could not be exported." }
    data object Cancelled : CompressionError { override val userMessage = "Compression was canceled." }
    data class Unknown(val detail: String? = null) : CompressionError { override val userMessage = "Compression failed. The original file was not changed." }
}

object CompressionPolicy {
    private val supportedImageMimeTypes = setOf("image/jpeg", "image/png", "image/webp", "image/heic", "image/heif")
    private val supportedVideoMimeTypes = setOf("video/mp4", "video/3gpp", "video/webm", "video/matroska")

    fun isSupportedMime(mimeType: String?, mediaType: String): Boolean = when (mediaType) {
        "image" -> mimeType?.lowercase() in supportedImageMimeTypes
        "video" -> mimeType?.lowercase() in supportedVideoMimeTypes
        else -> false
    }

    fun classify(error: Throwable): CompressionError = when {
        error is kotlinx.coroutines.CancellationException -> CompressionError.Cancelled
        error is SecurityException -> CompressionError.PermissionDenied
        error is FileNotFoundException || error is ClosedChannelException -> CompressionError.FileGone
        error is UnsupportedOperationException || error.message.orEmpty().contains("codec", ignoreCase = true) -> CompressionError.UnsupportedFormat
        error is IOException && error.message.orEmpty().contains("space", ignoreCase = true) -> CompressionError.StorageFull
        error.message.orEmpty().contains("output", ignoreCase = true) -> CompressionError.OutputUnavailable
        else -> CompressionError.Unknown(error.message)
    }

    fun exportName(mediaType: String): String =
        "compressed_${mediaType}_${System.currentTimeMillis()}${if (mediaType == "image") ".jpg" else ".mp4"}"
}
