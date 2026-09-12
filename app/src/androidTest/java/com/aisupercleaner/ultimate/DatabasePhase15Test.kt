package com.aisupercleaner.ultimate

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aisupercleaner.ultimate.data.AppDatabase
import com.aisupercleaner.ultimate.data.CompressionHistoryEntity
import com.aisupercleaner.ultimate.data.FileMetadataEntity
import com.aisupercleaner.ultimate.data.ScanHistoryEntity
import com.aisupercleaner.ultimate.data.TrashItemEntity
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabasePhase15Test {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    private val dao = database.storageDao()

    @After fun closeDatabase() = database.close()

    @Test fun fileUpsertAnalysisAndScanReconciliationWork() = runBlocking {
        dao.upsertFiles(listOf(
            FileMetadataEntity("content://one", "one.jpg", "image/jpeg", 10, 1, "image"),
            FileMetadataEntity("content://two", "two.jpg", "image/jpeg", 20, 1, "image")
        ))
        dao.markFilesSeen(listOf("content://one"), 7)
        assertEquals(1, dao.removeFilesNotSeenInScan(7))
        dao.updateAnalysis("content://one", "sha", "phash", 0.04, true, 1)
        val updated = dao.findFile("content://one")
        assertEquals("sha", updated?.contentHash)
        assertEquals(true, updated?.isScreenshot)
    }

    @Test fun scanTrashAndCompressionHistoryArePersisted() = runBlocking {
        dao.insertScanHistory(ScanHistoryEntity(startedAtEpochMillis = 1, completedAtEpochMillis = 2, filesScanned = 3, totalBytes = 4, status = "completed"))
        dao.insertTrashItem(TrashItemEntity("content://trash", "trash.jpg", 99, 5))
        dao.insertCompressionHistory(CompressionHistoryEntity(inputUri = "content://in", outputUri = "content://out", mediaType = "image", preset = "BALANCED", originalBytes = 100, outputBytes = 80, status = "completed", createdAtEpochMillis = 6))
        assertNotNull(dao.latestScan())
        assertEquals(1, dao.observeTrash().first().size)
        dao.removeTrashItem("content://trash")
        assertEquals(0, dao.observeTrash().first().size)
        assertEquals(1, dao.observeCompressionHistory().first().size)
    }
}
