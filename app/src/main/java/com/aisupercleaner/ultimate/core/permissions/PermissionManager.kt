package com.aisupercleaner.ultimate.core.permissions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun checkStatus(type: PermissionType): PermissionStatus {
        if (!type.isSupportedOnThisDevice()) return PermissionStatus.NOT_APPLICABLE
        return when (type) {
            PermissionType.ALL_FILES -> checkAllFiles()
            PermissionType.BIOMETRIC -> checkBiometric()
            PermissionType.QUERY_PACKAGES -> PermissionStatus.GRANTED
            else -> checkStandard(type)
        }
    }

    private fun checkStandard(type: PermissionType): PermissionStatus {
        val perms = type.manifestPermissions()
        if (perms.isEmpty()) return PermissionStatus.NOT_APPLICABLE
        val allGranted = perms.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) return PermissionStatus.GRANTED

        val activity = context as? Activity
        if (activity != null) {
            val permanentlyDenied = perms.any {
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
            }
            if (permanentlyDenied && hasBeenRequested(type)) return PermissionStatus.PERMANENTLY_DENIED
        }
        return if (hasBeenRequested(type)) PermissionStatus.DENIED else PermissionStatus.NOT_REQUESTED
    }

    private fun checkAllFiles(): PermissionStatus {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return PermissionStatus.NOT_APPLICABLE
        return if (Environment.isExternalStorageManager()) PermissionStatus.GRANTED
        else if (hasBeenRequested(PermissionType.ALL_FILES)) PermissionStatus.DENIED
        else PermissionStatus.NOT_REQUESTED
    }

    private fun checkBiometric(): PermissionStatus {
        val pm = context.packageManager
        val has = pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT) ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && pm.hasSystemFeature(PackageManager.FEATURE_FACE))
        return if (has) PermissionStatus.GRANTED else PermissionStatus.NOT_APPLICABLE
    }

    fun openAllFilesSettings(): Intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }

    fun openAppSettings(): Intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.parse("package:${context.packageName}")
    )

    fun markRequested(type: PermissionType) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(type.name, true).apply()
    }

    private fun hasBeenRequested(type: PermissionType): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(type.name, false)
    }

    fun hasCriticalPermissions(): Boolean =
        PermissionType.entries.filter { it.isCritical }
            .all { checkStatus(it) == PermissionStatus.GRANTED }

    fun canReadFiles(): Boolean {
        val storage = checkStatus(PermissionType.STORAGE)
        val allFiles = checkStatus(PermissionType.ALL_FILES)
        return storage == PermissionStatus.GRANTED || allFiles == PermissionStatus.GRANTED
    }

    companion object {
        private const val PREFS_NAME = "permissions_prefs"
    }
}
