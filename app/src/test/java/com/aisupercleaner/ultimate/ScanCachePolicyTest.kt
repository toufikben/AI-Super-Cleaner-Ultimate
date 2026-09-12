package com.aisupercleaner.ultimate

import com.aisupercleaner.ultimate.data.FileMetadataEntity
import com.aisupercleaner.ultimate.data.ScanCachePolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanCachePolicyTest {
    private val cached = FileMetadataEntity("content://media/1", "photo.jpg", "image/jpeg", 1024, 100, "image")

    @Test fun unchangedMetadataIsCacheHit() {
        assertTrue(ScanCachePolicy.isUnchanged(cached, cached.copy()))
    }

    @Test fun changedSizeIsCacheMiss() {
        assertFalse(ScanCachePolicy.isUnchanged(cached, cached.copy(sizeBytes = 2048)))
    }

    @Test fun changedTimestampIsCacheMiss() {
        assertFalse(ScanCachePolicy.isUnchanged(cached, cached.copy(modifiedEpochSeconds = 101)))
    }

    @Test fun changedMediaTypeIsCacheMiss() {
        assertFalse(ScanCachePolicy.isUnchanged(cached, cached.copy(mediaType = "video")))
    }

    @Test fun missingCacheIsCacheMiss() {
        assertFalse(ScanCachePolicy.isUnchanged(null, cached))
    }

    @Test fun oldAnalysisVersionIsInvalidated() {
        assertFalse(ScanCachePolicy.analysisIsCurrent(cached))
    }

    @Test fun currentAnalysisVersionIsReusable() {
        assertTrue(ScanCachePolicy.analysisIsCurrent(cached.copy(analysisVersion = ScanCachePolicy.CURRENT_ANALYSIS_VERSION)))
    }
}
