package com.aisupercleaner.ultimate.data

object ScanCachePolicy {
    fun isUnchanged(cached: FileMetadataEntity?, current: FileMetadataEntity): Boolean = cached != null && cached.sizeBytes == current.sizeBytes && cached.modifiedEpochSeconds == current.modifiedEpochSeconds && cached.mimeType == current.mimeType
}
