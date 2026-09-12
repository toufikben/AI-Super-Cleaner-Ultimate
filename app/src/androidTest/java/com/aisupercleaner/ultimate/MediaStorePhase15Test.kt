package com.aisupercleaner.ultimate

import android.content.Context
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aisupercleaner.ultimate.data.MediaStoreInventory
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MediaStorePhase15Test {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun mediaStoreCollectionsAreAvailable() {
        assertNotNull(context.contentResolver)
        assertTrue(MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString().startsWith("content://"))
        assertTrue(MediaStore.Video.Media.EXTERNAL_CONTENT_URI.toString().startsWith("content://"))
    }

    @Test fun appDoesNotRequestBroadStoragePermission() {
        val permissions = context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty()
        assertTrue(permissions.none { it == "android.permission.MANAGE_EXTERNAL_STORAGE" })
    }

    @Test fun inventoryIncludesNonMediaFilesWithoutDuplicatingMediaCollections() {
        val general = MediaStoreInventory.supportedSources.single { it.mediaType == "file" }
        assertTrue(general.collection == MediaStore.Files.getContentUri("external"))
        assertTrue(general.selection.orEmpty().contains("media_type"))
        assertTrue(general.selectionArgs.orEmpty().contentEquals(arrayOf("0")))
        assertTrue(MediaStoreInventory.supportedSources.size == 4)
    }
}
