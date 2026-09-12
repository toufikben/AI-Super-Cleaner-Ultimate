package com.aisupercleaner.ultimate

import com.aisupercleaner.ultimate.data.CrossFeatureInvalidation
import com.aisupercleaner.ultimate.data.DerivedFeature
import com.aisupercleaner.ultimate.data.InvalidationEvent
import org.junit.Assert.assertTrue
import org.junit.Test

class CrossFeatureInvalidationTest {
    @Test fun destructiveTransitionsInvalidateAllDerivedViews() {
        val expected = setOf(DerivedFeature.CACHE, DerivedFeature.DUPLICATES, DerivedFeature.RECOMMENDATIONS, DerivedFeature.COUNTERS, DerivedFeature.DASHBOARD_SCORE, DerivedFeature.CLEANUP_SELECTION)
        assertTrue(CrossFeatureInvalidation.affectedBy(InvalidationEvent.MOVE_TO_TRASH).containsAll(expected))
        assertTrue(CrossFeatureInvalidation.affectedBy(InvalidationEvent.RESTORE).containsAll(expected))
        assertTrue(CrossFeatureInvalidation.affectedBy(InvalidationEvent.PERMANENT_DELETE).containsAll(expected))
    }

    @Test fun externalStaleResultCannotRemainEligibleForCleanup() {
        val affected = CrossFeatureInvalidation.affectedBy(InvalidationEvent.EXTERNAL_REVALIDATION_FAILURE)
        assertTrue(DerivedFeature.DUPLICATES in affected)
        assertTrue(DerivedFeature.RECOMMENDATIONS in affected)
        assertTrue(DerivedFeature.CLEANUP_SELECTION in affected)
    }
}
