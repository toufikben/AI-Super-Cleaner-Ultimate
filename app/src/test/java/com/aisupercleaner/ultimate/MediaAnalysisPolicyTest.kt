package com.aisupercleaner.ultimate

import com.aisupercleaner.ultimate.data.DuplicateGrouping
import com.aisupercleaner.ultimate.data.FileMetadataEntity
import com.aisupercleaner.ultimate.data.MediaClassification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaAnalysisPolicyTest {
    private fun file(uri: String, name: String, size: Long = 100, type: String = "image") =
        FileMetadataEntity(uri, name, "image/jpeg", size, 1, type)

    @Test fun sameSizeDifferentContentHashesAreNotGrouped() {
        val files = listOf(file("1", "a").copy(contentHash = "aaa"), file("2", "b").copy(contentHash = "bbb"))
        assertTrue(DuplicateGrouping.exactGroups(files).isEmpty())
    }

    @Test fun sameHashFilesAreGroupedAndRecoverableBytesExcludeKeeper() {
        val files = listOf(file("1", "a").copy(contentHash = "same"), file("2", "b", 250).copy(contentHash = "same"))
        val groups = DuplicateGrouping.exactGroups(files)
        assertEquals(1, groups.size)
        assertEquals(250, groups.single().recoverableBytes)
    }

    @Test fun screenshotClassificationRequiresImageNameOrPathSignal() {
        assertNotNull(MediaClassification.screenshotFinding(file("1", "Screenshot_2026.png")))
        assertNull(MediaClassification.screenshotFinding(file("2", "holiday.png")))
        assertNull(MediaClassification.screenshotFinding(file("3", "Screenshot.mp4", type = "video")))
    }

    @Test fun blurClassificationIsCautiousAndNonDestructive() {
        val finding = MediaClassification.blurFinding(file("1", "night.jpg"), 0.05)
        assertNotNull(finding)
        assertEquals("Potentially blurry", finding?.classification)
        assertTrue(finding!!.confidencePercent in 1..99)
        assertFalse(MediaClassification.blurFinding(file("2", "sharp.jpg"), 0.20) != null)
    }
}
