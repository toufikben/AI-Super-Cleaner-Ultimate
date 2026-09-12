package com.aisupercleaner.ultimate

import com.aisupercleaner.ultimate.data.DuplicateGrouping
import com.aisupercleaner.ultimate.data.FileMetadataEntity
import com.aisupercleaner.ultimate.data.MediaClassification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase15PolicyTest {
    private fun file(
        uri: String,
        name: String = "photo.jpg",
        size: Long = 100L,
        hash: String? = null,
        mediaType: String = "image",
        path: String? = null
    ) = FileMetadataEntity(uri, name, "image/jpeg", size, 1L, mediaType, contentHash = hash, relativePath = path)

    @Test fun sameSizeDifferentHashesAreOnlyCandidatesNotExactDuplicates() {
        val files = listOf(file("a", size = 20, hash = "hash-a"), file("b", size = 20, hash = "hash-b"))
        assertEquals(1, DuplicateGrouping.candidateGroups(files).size)
        assertTrue(DuplicateGrouping.exactGroups(files).isEmpty())
    }

    @Test fun equalHashesProduceRecoverableBytesWithoutCountingAnchor() {
        val group = DuplicateGrouping.exactGroups(listOf(file("a", size = 10, hash = "same"), file("b", size = 20, hash = "same"), file("c", size = 30, hash = "other"))).single()
        assertEquals(20L, group.recoverableBytes)
        assertEquals(2, group.files.size)
    }

    @Test fun screenshotRequiresImageAndKnownPattern() {
        assertEquals("Potential screenshot", MediaClassification.screenshotFinding(file("a", name = "Screenshot_1.png"))?.classification)
        assertNull(MediaClassification.screenshotFinding(file("b", name = "Screenshot_1.mp4", mediaType = "video")))
        assertNull(MediaClassification.screenshotFinding(file("c", name = "holiday.jpg")))
    }

    @Test fun blurThresholdIsReviewOnlyAndBounded() {
        val finding = MediaClassification.blurFinding(file("a"), 0.01)
        assertEquals("Potentially blurry", finding?.classification)
        assertTrue(finding!!.confidencePercent in 1..99)
        assertNull(MediaClassification.blurFinding(file("b"), 0.10))
    }

    @Test fun candidateGroupingHandlesLargeLibraryWithoutEmptyGroups() {
        val files = (1..100_000).map { index -> file("uri:$index", size = (index % 100 + 1).toLong()) }
        val groups = DuplicateGrouping.candidateGroups(files)
        assertEquals(100, groups.size)
        assertTrue(groups.all { it.size == 1_000 })
    }
}
