package com.aisupercleaner.ultimate.data.vault

data class VaultDataClearResult(
    val vaultCleared: Boolean,
    val tempCleared: Boolean,
    val errors: List<String> = emptyList(),
)
