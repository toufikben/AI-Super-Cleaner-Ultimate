package com.aisupercleaner.ultimate.data

import kotlin.math.ceil

/** Suggestions only: this policy never selects or deletes a file by itself. */
object DuplicateKeepBestPolicy {
    fun bestCandidate(files: List<FileMetadataEntity>): FileMetadataEntity? =
        files.maxWithOrNull(compareBy<FileMetadataEntity> {
            if (it.mediaType == "image" && it.blurScore != null) it.blurScore else 0.0
        }.thenBy { it.sizeBytes }.thenBy { it.modifiedEpochSeconds }.thenBy { it.uri })

    fun removableSuggestions(files: List<FileMetadataEntity>): List<FileMetadataEntity> {
        val best = bestCandidate(files) ?: return emptyList()
        return files.filter { it.uri != best.uri && !ProtectedItemPolicy.isProtected(it) }
    }
}

object ProtectedItemPolicy {
    private val protectedTokens = listOf(".nomedia", "keep", "favorite", "favourite", "protected")

    fun isProtected(file: FileMetadataEntity): Boolean {
        val value = "${file.displayName} ${file.relativePath.orEmpty()}".lowercase()
        return protectedTokens.any(value::contains)
    }
}

object ConfidencePolicy {
    fun label(percent: Int): String = when (percent.coerceIn(0, 100)) {
        in 0..39 -> "Low confidence — review carefully"
        in 40..74 -> "Medium confidence — review before action"
        else -> "High confidence heuristic — explicit selection required"
    }
}

object TrashPolicy {
    const val DEFAULT_RETENTION_DAYS = 30L

    fun remainingDays(trashedAtEpochMillis: Long, nowEpochMillis: Long, retentionDays: Long = DEFAULT_RETENTION_DAYS): Long {
        if (retentionDays <= 0L) return 0L
        val expiry = trashedAtEpochMillis + retentionDays * 24L * 60L * 60L * 1000L
        return ceil(((expiry - nowEpochMillis).coerceAtLeast(0L)).toDouble() / (24L * 60L * 60L * 1000L)).toLong()
    }
}
