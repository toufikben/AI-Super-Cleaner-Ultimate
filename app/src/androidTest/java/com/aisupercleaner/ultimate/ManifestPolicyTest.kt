package com.aisupercleaner.ultimate

import android.Manifest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ManifestPolicyTest {
    @Test fun appHasExpectedPackage() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertNotNull(context.packageManager.getApplicationInfo(context.packageName, 0))
    }

    @Test fun broadExternalStoragePermissionIsAbsent() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        val permissions = info.requestedPermissions?.toSet().orEmpty()
        assertFalse(permissions.contains(Manifest.permission.MANAGE_EXTERNAL_STORAGE))
    }
}
