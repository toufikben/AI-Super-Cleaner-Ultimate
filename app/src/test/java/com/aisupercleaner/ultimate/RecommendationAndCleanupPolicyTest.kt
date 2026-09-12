package com.aisupercleaner.ultimate

import com.aisupercleaner.ultimate.data.CleanupResult
import com.aisupercleaner.ultimate.data.RecommendationScoring
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendationAndCleanupPolicyTest {
    @Test fun pressureUsesTotalStorageNotCleanupPotential() {
        assertEquals(80, RecommendationScoring.pressurePercent(800, 200))
        assertEquals(0, RecommendationScoring.pressurePercent(0, 0))
    }

    @Test fun storageImpactIsSeparateFromClassificationConfidence() {
        assertEquals(10, RecommendationScoring.storageImpactPercent(100, 500, 500))
    }

    @Test fun healthScoreIsBounded() {
        assertTrue(RecommendationScoring.healthScore(0, 0, 1_000) in 0..100)
        assertTrue(RecommendationScoring.healthScore(100, 10_000, 1) in 0..100)
    }

    @Test fun partialCleanupIsNotSuccessClaim() {
        val result: CleanupResult = CleanupResult.PartialSuccess(2, 100, 1, "2 moved; 1 failed")
        assertTrue(result is CleanupResult.PartialSuccess)
    }
}
