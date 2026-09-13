package com.aisupercleaner.ultimate.data.system

import android.app.ActivityManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class RamInfo(
    val totalBytes: Long,
    val availBytes: Long,
    val usedBytes: Long,
    val usedPercent: Float,
    val isLowMemory: Boolean,
    val thresholdBytes: Long,
) {
    companion object { val EMPTY = RamInfo(0, 0, 0, 0f, false, 0) }
}

@Singleton
class RamManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    suspend fun getRamInfo(): RamInfo = withContext(Dispatchers.IO) {
        runCatching {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val mi = ActivityManager.MemoryInfo()
            am.getMemoryInfo(mi)
            val used = mi.totalMem - mi.availMem
            RamInfo(
                totalBytes = mi.totalMem,
                availBytes = mi.availMem,
                usedBytes = used,
                usedPercent = if (mi.totalMem > 0) used.toFloat() / mi.totalMem.toFloat() else 0f,
                isLowMemory = mi.lowMemory,
                thresholdBytes = mi.threshold,
            )
        }.getOrDefault(RamInfo.EMPTY)
    }

    suspend fun boost(): Long = withContext(Dispatchers.IO) {
        val before = getRamInfo().availBytes
        runCatching { System.gc() }
        kotlinx.coroutines.delay(600)
        val after = getRamInfo().availBytes
        (after - before).coerceAtLeast(0)
    }
}
