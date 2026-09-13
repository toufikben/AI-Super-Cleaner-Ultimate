package com.aisupercleaner.ultimate.data.storage

import android.os.Environment
import android.os.StatFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageRepository @Inject constructor() {

    private val _refreshTrigger = MutableStateFlow(0)
    val refreshTrigger: Flow<Int> = _refreshTrigger.asStateFlow()

    // ✅ FIX: يبثّ مرة واحدة فقط
    fun observeStorageInfo(): Flow<StorageInfo> = flow {
        emit(readInternalStorage())
    }.flowOn(Dispatchers.IO)

    suspend fun getStorageInfo(): StorageInfo = withContext(Dispatchers.IO) {
        readInternalStorage()
    }

    fun refresh() { _refreshTrigger.value += 1 }

    private fun readInternalStorage(): StorageInfo = runCatching {
        val stat = StatFs(Environment.getDataDirectory().path)
        val blockSize = stat.blockSizeLong
        val total = blockSize * stat.blockCountLong
        val free = blockSize * stat.availableBlocksLong
        val used = (total - free).coerceAtLeast(0L)
        val percent = if (total > 0) used.toFloat() / total.toFloat() else 0f
        StorageInfo(total, used, free, percent)
    }.getOrElse { StorageInfo.EMPTY }
}
