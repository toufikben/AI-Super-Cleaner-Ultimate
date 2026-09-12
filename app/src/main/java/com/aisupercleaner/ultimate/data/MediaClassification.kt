package com.aisupercleaner.ultimate.data

data class MediaFinding(
    val file: FileMetadataEntity,
    val classification: String,
    val confidencePercent: Int,
    val reason: String
)

object MediaClassification {
    fun blurFinding(file: FileMetadataEntity, score: Double): MediaFinding? {
        if (score >= 0.10) return null
        val confidence = (((0.10 - score) / 0.10) * 100.0).toInt().coerceIn(1, 99)
        return MediaFinding(file.copy(blurScore = score), "Potentially blurry", confidence, "Low edge detail heuristic; review before any action.")
    }

    fun screenshotFinding(file: FileMetadataEntity): MediaFinding? {
        val haystack = "${file.displayName} ${file.relativePath.orEmpty()}".lowercase()
        val matched = listOf("screenshot", "screen_shot", "screen-shot", "screen shot").any(haystack::contains)
        if (!matched || file.mediaType != "image") return null
        return MediaFinding(file.copy(isScreenshot = true), "Potential screenshot", 90, "Filename or MediaStore relative path matches a screenshot pattern.")
    }
}
