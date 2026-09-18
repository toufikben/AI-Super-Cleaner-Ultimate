package com.aisupercleaner.ultimate.data.cleanup

data class CleanupResult(
    val deletedCount: Int,
    val failedCount: Int,
    val freedBytes: Long,
    val durationMs: Long,
    val successfulPaths: List<String> = emptyList(),
    val failedPaths: List<String> = emptyList(),
    val failures: List<Failure> = emptyList(),
) {
    data class Failure(val path: String, val reason: String)
}
