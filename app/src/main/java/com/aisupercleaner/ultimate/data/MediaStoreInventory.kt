package com.aisupercleaner.ultimate.data

import android.provider.MediaStore

data class MediaStoreScanSource(
    val collection: android.net.Uri,
    val mediaType: String,
    val progressLabel: String
)

object MediaStoreInventory {
    val supportedSources: List<MediaStoreScanSource> = listOf(
        MediaStoreScanSource(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image", "Scanning photos from Android MediaStore"),
        MediaStoreScanSource(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "video", "Scanning videos from Android MediaStore"),
        MediaStoreScanSource(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "audio", "Scanning audio from Android MediaStore")
    )

    const val scopeDescription = "Images, videos, and audio exposed by Android MediaStore"
}
