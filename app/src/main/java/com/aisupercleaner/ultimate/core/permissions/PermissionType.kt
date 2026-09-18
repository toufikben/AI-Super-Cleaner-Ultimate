package com.aisupercleaner.ultimate.core.permissions

import android.Manifest
import android.os.Build
import androidx.annotation.StringRes
import com.aisupercleaner.ultimate.R

enum class PermissionType(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val isCritical: Boolean,
) {
    STORAGE(R.string.perm_storage_title, R.string.perm_storage_desc, true),
    ALL_FILES(R.string.perm_all_files_title, R.string.perm_all_files_desc, false),
    NOTIFICATIONS(R.string.perm_notif_title, R.string.perm_notif_desc, false),
    BIOMETRIC(R.string.perm_biometric_title, R.string.perm_biometric_desc, false);

    fun manifestPermissions(): Array<String> = when (this) {
        STORAGE -> when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO,
            )
            else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        NOTIFICATIONS -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            arrayOf(Manifest.permission.POST_NOTIFICATIONS) else emptyArray()
        BIOMETRIC, ALL_FILES -> emptyArray()
    }

    fun isSupportedOnThisDevice(): Boolean = when (this) {
        NOTIFICATIONS -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        ALL_FILES -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
        else -> true
    }
}
