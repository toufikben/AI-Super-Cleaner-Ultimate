package com.aisupercleaner.ultimate

import com.aisupercleaner.ultimate.data.FileMetadataEntity
import com.aisupercleaner.ultimate.data.ScanCachePolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanCachePolicyTest {
    private val cached = FileMetadataEntity("content://media/1", "photo.jpg", "image/jpeg", 1024, 100, "image")

    @Test fun unchangedMetadataIsCacheHit() {
        val current = cached.copy()
        assertTrue(ScanCachePolicy.isUnchanged(cached, current))
    }

    @Test fun changedSizeIsCacheMiss() {
        assertFalse(ScanCachePolicy.isUnchanged(cached, cached.copy(sizeBytes = 2048)))
    }

    @Test fun changedTimestampIsCacheMiss() {
        assertFalse(ScanCachePolicy.isUnchanged(cached, cached.copy(modifiedEpochSeconds = 101)))
    }

    @Test fun missingCacheIsCacheMiss() {
        assertFalse(ScanCachePolicy.isUnchanged(null, cached))
    }
}
