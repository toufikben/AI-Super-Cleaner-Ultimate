package com.aisupercleaner.ultimate.data.clear

data class ClearAllDataResult(
    val databaseCleared: Boolean = false,
    val dataStoreCleared: Boolean = false,
    val vaultCleared: Boolean = false,
    val tempCleared: Boolean = false,
    val workManagerCleared: Boolean = false,
    val errors: List<String> = emptyList(),
) {
    val isComplete: Boolean
        get() = databaseCleared && dataStoreCleared && vaultCleared && tempCleared && workManagerCleared && errors.isEmpty()
}
