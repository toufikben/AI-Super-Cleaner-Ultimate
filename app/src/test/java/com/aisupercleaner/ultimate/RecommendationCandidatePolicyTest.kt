package com.aisupercleaner.ultimate

import com.aisupercleaner.ultimate.data.FileMetadataEntity
import com.aisupercleaner.ultimate.data.RecommendationCandidatePolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendationCandidatePolicyTest {
    private fun file(name: String, type: String, bytes: Long) = FileMetadataEntity("content://$name", name, "image/*", bytes, 1L, type)

    @Test fun mapsLargeVideosAndPhotos() {
        assertTrue(RecommendationCandidatePolicy.matches("Large videos", file("movie.mp4", "video", 500L * 1024L * 1024L)))
        assertTrue(RecommendationCandidatePolicy.matches("Large photos", file("export.jpg", "image", 20L * 1024L * 1024L)))
        assertFalse(RecommendationCandidatePolicy.matches("Large photos", file("small.jpg", "image", 1L)))
    }

    @Test fun mapsScreenshotsWithoutTreatingOtherImagesAsScreenshots() {
        assertTrue(RecommendationCandidatePolicy.matches("Screenshots", file("Screenshot_001.png", "image", 10L)))
        assertFalse(RecommendationCandidatePolicy.matches("Screenshots", file("holiday.png", "image", 10L)))
    }
}
