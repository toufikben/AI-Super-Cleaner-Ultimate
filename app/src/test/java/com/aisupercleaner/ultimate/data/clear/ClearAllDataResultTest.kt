package com.aisupercleaner.ultimate.data.clear

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ClearAllDataResultTest {
    @Test
    fun completeOnlyWhenEveryAreaClearedWithoutErrors() {
        assertThat(
            ClearAllDataResult(
                databaseCleared = true,
                dataStoreCleared = true,
                vaultCleared = true,
                tempCleared = true,
                workManagerCleared = true,
            ).isComplete
        ).isTrue()
    }

    @Test
    fun partialOrErroredClearIsNotComplete() {
        assertThat(
            ClearAllDataResult(
                databaseCleared = true,
                dataStoreCleared = true,
                vaultCleared = true,
                tempCleared = true,
                workManagerCleared = false,
            ).isComplete
        ).isFalse()
        assertThat(
            ClearAllDataResult(
                databaseCleared = true,
                dataStoreCleared = true,
                vaultCleared = true,
                tempCleared = true,
                workManagerCleared = true,
                errors = listOf("database: failed"),
            ).isComplete
        ).isFalse()
    }
}
