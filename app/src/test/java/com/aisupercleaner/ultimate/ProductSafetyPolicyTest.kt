package com.aisupercleaner.ultimate

import com.aisupercleaner.ultimate.data.ConfidencePolicy
import com.aisupercleaner.ultimate.data.DuplicateKeepBestPolicy
import com.aisupercleaner.ultimate.data.FileMetadataEntity
import com.aisupercleaner.ultimate.data.ProtectedItemPolicy
import com.aisupercleaner.ultimate.data.TrashPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductSafetyPolicyTest {
    private fun file(uri: String, name: String, size: Long, blur: Double? = null) = FileMetadataEntity(uri, name, "image/jpeg", size, 10, "image", blurScore = blur)

    @Test fun keepBestPrefersSharperThenLargerThenNewerCandidate() {
        val best = DuplicateKeepBestPolicy.bestCandidate(listOf(file("a", "a.jpg", 100, 0.02), file("b", "b.jpg", 200, 0.08), file("c", "c.jpg", 300, 0.08)))
        assertEquals("c", best?.uri)
        assertEquals(listOf("a", "b"), DuplicateKeepBestPolicy.removableSuggestions(listOf(file("a", "a.jpg", 100, 0.02), file("b", "b.jpg", 200, 0.08), file("c", "c.jpg", 300, 0.08))).map { it.uri })
    }

    @Test fun protectedNamesAreNeverSuggestedForRemoval() {
        assertTrue(ProtectedItemPolicy.isProtected(file("a", "favorite.jpg", 10)))
        assertFalse(DuplicateKeepBestPolicy.removableSuggestions(listOf(file("a", "favorite.jpg", 10), file("b", "copy.jpg", 20))).any { it.uri == "a" })
    }

    @Test fun confidenceLabelsRemainHonestAndBounded() {
        assertTrue(ConfidencePolicy.label(20).startsWith("Low"))
        assertTrue(ConfidencePolicy.label(60).startsWith("Medium"))
        assertTrue(ConfidencePolicy.label(95).contains("explicit selection"))
    }

    @Test fun trashRemainingDaysNeverGoesNegativeAndRoundsUp() {
        val day = 24L * 60L * 60L * 1000L
        assertEquals(2L, TrashPolicy.remainingDays(0L, 28L * day))
        assertEquals(0L, TrashPolicy.remainingDays(0L, 31L * day))
    }
}
