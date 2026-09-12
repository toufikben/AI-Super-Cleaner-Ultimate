package com.aisupercleaner.ultimate.data

/** Review-only policy for removable-looking files exposed by MediaStore. */
object JunkCandidatePolicy {
    const val TEMP_MIN_AGE_DAYS = 7L
    const val APK_MIN_AGE_DAYS = 30L
    const val MAX_REVIEW_SIZE_BYTES = 50L * 1024L * 1024L

    fun classify(file: FileMetadataEntity, nowEpochSeconds: Long): JunkClassification {
        val ageDays = ((nowEpochSeconds - file.modifiedEpochSeconds).coerceAtLeast(0L)) / (24L * 60L * 60L)
        val name = file.displayName.lowercase()
        val temporaryName = TEMP_SUFFIXES.any { name.endsWith(it) } || TEMP_TOKENS.any { name.contains(it) }
        if (temporaryName && ageDays >= TEMP_MIN_AGE_DAYS && file.sizeBytes <= MAX_REVIEW_SIZE_BYTES) {
            return JunkClassification.TemporaryLeftover(ageDays)
        }
        if (name.endsWith(".apk") && ageDays >= APK_MIN_AGE_DAYS) {
            return JunkClassification.ObsoleteApkReview(ageDays)
        }
        return JunkClassification.NotCandidate
    }

    private val TEMP_SUFFIXES = listOf(".tmp", ".temp", ".bak", ".old", ".log", ".cache")
    private val TEMP_TOKENS = listOf(".part", ".crdownload", ".download")
}

sealed interface JunkClassification {
    data class TemporaryLeftover(val ageDays: Long) : JunkClassification
    data class ObsoleteApkReview(val ageDays: Long) : JunkClassification
    data object NotCandidate : JunkClassification
}
