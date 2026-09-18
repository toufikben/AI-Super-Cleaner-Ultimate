package com.aisupercleaner.ultimate.data.cleanup

import com.aisupercleaner.ultimate.data.history.HistoryRepository
import com.aisupercleaner.ultimate.data.preferences.AppPreferences
import com.aisupercleaner.ultimate.data.scanner.JunkCategory
import com.aisupercleaner.ultimate.data.scanner.JunkItem
import com.aisupercleaner.ultimate.data.storage.StorageRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CleanupManagerHistoryTest {
    @Test
    fun failureOnlyCleanupDoesNotCreateHistorySession() = runTest {
        val history = mockk<HistoryRepository>(relaxed = true)
        val preferences = mockk<AppPreferences>(relaxed = true)
        val storage = mockk<StorageRepository>(relaxed = true)
        val manager = CleanupManager(storage, preferences, history)

        val result = manager.deleteJunk(
            items = listOf(
                JunkItem(
                    path = "/definitely-missing/rules-file.tmp",
                    sizeBytes = 42L,
                    lastModified = 0L,
                    category = JunkCategory.CACHE,
                ),
            ),
            source = com.aisupercleaner.ultimate.data.history.HistoryEntry.Source.JUNK,
        )

        assertThat(result.deletedCount).isEqualTo(0)
        assertThat(result.failedCount).isEqualTo(1)
        coVerify(exactly = 0) { history.record(any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { preferences.recordCleanup(any()) }
    }
}
