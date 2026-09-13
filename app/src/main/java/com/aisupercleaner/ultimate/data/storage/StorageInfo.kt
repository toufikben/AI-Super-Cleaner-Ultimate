package com.aisupercleaner.ultimate.data.storage

data class StorageInfo(
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long,
    val usedPercent: Float,
) {
    companion object {
        val EMPTY = StorageInfo(0L, 0L, 0L, 0f)
    }
}
