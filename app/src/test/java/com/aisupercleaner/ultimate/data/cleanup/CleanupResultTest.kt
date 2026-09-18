package com.aisupercleaner.ultimate.data.cleanup

import com.aisupercleaner.ultimate.data.history.HistoryEntry
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CleanupResultTest {
    @Test
    fun exposesSuccessfulAndFailedPathsSeparately() {
        val result = CleanupResult(
            deletedCount = 1,
            failedCount = 1,
            freedBytes = 12L,
            durationMs = 3L,
            successfulPaths = listOf("/cache/removed.tmp"),
            failedPaths = listOf("/cache/locked.tmp"),
            failures = listOf(CleanupResult.Failure("/cache/locked.tmp", "in use")),
        )

        assertThat(result.successfulPaths).containsExactly("/cache/removed.tmp")
        assertThat(result.failedPaths).containsExactly("/cache/locked.tmp")
        assertThat(result.failures.single().path).isEqualTo(result.failedPaths.single())
    }

    @Test
    fun historyHasDistinctSourceForLargeFilesCleanup() {
        assertThat(HistoryEntry.Source.LARGE_FILES.name).isEqualTo("LARGE_FILES")
    }
}
