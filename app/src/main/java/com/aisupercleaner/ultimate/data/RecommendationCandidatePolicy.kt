package com.aisupercleaner.ultimate.data

object RecommendationCandidatePolicy {
    fun matches(category: String, file: FileMetadataEntity): Boolean = when (category) {
        "Large videos" -> file.mediaType == "video" && file.sizeBytes >= 500L * 1024L * 1024L
        "Large photos" -> file.mediaType == "image" && file.sizeBytes >= 20L * 1024L * 1024L
        "Screenshots" -> file.mediaType == "image" && listOf("screenshot", "screen_shot", "screen-shot", "screen shot").any { file.displayName.lowercase().contains(it) }
        else -> false
    }
}
