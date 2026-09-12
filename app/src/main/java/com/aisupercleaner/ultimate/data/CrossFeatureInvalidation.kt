package com.aisupercleaner.ultimate.data

enum class InvalidationEvent { SCAN_COMPLETED, MOVE_TO_TRASH, RESTORE, PERMANENT_DELETE, EXTERNAL_REVALIDATION_FAILURE }
enum class DerivedFeature { CACHE, DUPLICATES, RECOMMENDATIONS, COUNTERS, DASHBOARD_SCORE, CLEANUP_SELECTION }

object CrossFeatureInvalidation {
    fun affectedBy(event: InvalidationEvent): Set<DerivedFeature> = when (event) {
        InvalidationEvent.SCAN_COMPLETED -> setOf(DerivedFeature.CACHE, DerivedFeature.DUPLICATES, DerivedFeature.RECOMMENDATIONS, DerivedFeature.COUNTERS, DerivedFeature.DASHBOARD_SCORE, DerivedFeature.CLEANUP_SELECTION)
        InvalidationEvent.MOVE_TO_TRASH, InvalidationEvent.PERMANENT_DELETE -> setOf(DerivedFeature.CACHE, DerivedFeature.DUPLICATES, DerivedFeature.RECOMMENDATIONS, DerivedFeature.COUNTERS, DerivedFeature.DASHBOARD_SCORE, DerivedFeature.CLEANUP_SELECTION)
        InvalidationEvent.RESTORE -> setOf(DerivedFeature.CACHE, DerivedFeature.DUPLICATES, DerivedFeature.RECOMMENDATIONS, DerivedFeature.COUNTERS, DerivedFeature.DASHBOARD_SCORE, DerivedFeature.CLEANUP_SELECTION)
        InvalidationEvent.EXTERNAL_REVALIDATION_FAILURE -> setOf(DerivedFeature.DUPLICATES, DerivedFeature.RECOMMENDATIONS, DerivedFeature.CLEANUP_SELECTION)
    }
}
