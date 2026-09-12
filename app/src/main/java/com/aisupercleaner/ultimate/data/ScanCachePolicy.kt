package com.aisupercleaner.ultimate.data

object ScanCachePolicy {
    const val CURRENT_ANALYSIS_VERSION = 1

    fun isUnchanged(cached: FileMetadataEntity?, current: FileMetadataEntity): Boolean =
        cached != null &&
            cached.uri == current.uri &&
            cached.sizeBytes == current.sizeBytes &&
            cached.modifiedEpochSeconds == current.modifiedEpochSeconds &&
            cached.mimeType == current.mimeType &&
            cached.mediaType == current.mediaType

    fun analysisIsCurrent(file: FileMetadataEntity): Boolean =
        file.analysisVersion == CURRENT_ANALYSIS_VERSION
}
