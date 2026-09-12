package com.aisupercleaner.ultimate.data

import android.provider.MediaStore

data class MediaStoreScanSource(
    val collection: android.net.Uri,
    val mediaType: String,
    val progressLabel: String,
    val selection: String? = null,
    val selectionArgs: Array<String>? = null
)

object MediaStoreInventory {
    val supportedSources: List<MediaStoreScanSource> = listOf(
        MediaStoreScanSource(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image", "Scanning photos from Android MediaStore"),
        MediaStoreScanSource(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "video", "Scanning videos from Android MediaStore"),
        MediaStoreScanSource(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "audio", "Scanning audio from Android MediaStore"),
        MediaStoreScanSource(
            MediaStore.Files.getContentUri("external"),
            "file",
            "Scanning documents, archives, APKs, and Downloads from Android MediaStore",
            selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?",
            selectionArgs = arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_NONE.toString())
        )
    )

    const val scopeDescription = "Images, videos, audio, and non-media files exposed by Android MediaStore, including indexed documents, archives, APKs, and Downloads"
}
