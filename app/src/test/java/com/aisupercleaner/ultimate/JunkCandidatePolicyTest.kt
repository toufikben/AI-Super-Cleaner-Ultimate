package com.aisupercleaner.ultimate

import com.aisupercleaner.ultimate.data.FileMetadataEntity
import com.aisupercleaner.ultimate.data.JunkCandidatePolicy
import com.aisupercleaner.ultimate.data.JunkClassification
import org.junit.Assert.assertTrue
import org.junit.Test

class JunkCandidatePolicyTest {
    @Test fun oldSmallTempFileIsReviewCandidate() {
        val now = 100L * 24L * 60L * 60L
        val file = FileMetadataEntity("content://tmp", "render.tmp", "application/octet-stream", 1024L, now - 8L * 24L * 60L * 60L, "other")
        assertTrue(JunkCandidatePolicy.classify(file, now) is JunkClassification.TemporaryLeftover)
    }

    @Test fun recentOrLargeTempFileIsNotCandidate() {
        val now = 100L * 24L * 60L * 60L
        val recent = FileMetadataEntity("content://tmp1", "render.tmp", "application/octet-stream", 1024L, now - 2L * 24L * 60L * 60L, "other")
        val large = recent.copy(uri = "content://tmp2", sizeBytes = JunkCandidatePolicy.MAX_REVIEW_SIZE_BYTES + 1L, modifiedEpochSeconds = now - 20L * 24L * 60L * 60L)
        assertTrue(JunkCandidatePolicy.classify(recent, now) is JunkClassification.NotCandidate)
        assertTrue(JunkCandidatePolicy.classify(large, now) is JunkClassification.NotCandidate)
    }

    @Test fun oldApkNeedsReview() {
        val now = 100L * 24L * 60L * 60L
        val file = FileMetadataEntity("content://apk", "old.apk", "application/vnd.android.package-archive", 1024L, now - 31L * 24L * 60L * 60L, "apk")
        assertTrue(JunkCandidatePolicy.classify(file, now) is JunkClassification.ObsoleteApkReview)
    }
}
