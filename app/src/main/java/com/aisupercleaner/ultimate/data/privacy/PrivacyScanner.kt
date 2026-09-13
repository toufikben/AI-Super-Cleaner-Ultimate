package com.aisupercleaner.ultimate.data.privacy

import android.Manifest
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrivacyScanner @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val dangerousPermissions = listOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.READ_SMS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.BODY_SENSORS,
        Manifest.permission.READ_CALENDAR,
        "android.permission.READ_MEDIA_IMAGES",
        "android.permission.READ_MEDIA_VIDEO",
        "android.permission.READ_MEDIA_AUDIO",
        "android.permission.POST_NOTIFICATIONS",
    )

    fun scan(): Flow<List<AppPrivacyInfo>> = flow {
        val pm = context.packageManager
        val packages = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)

        val result = packages.mapNotNull { pkg ->
            val appInfo = pkg.applicationInfo ?: return@mapNotNull null
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            if (isSystem) return@mapNotNull null

            val requested = pkg.requestedPermissions ?: emptyArray()
            val granted = requested.filter { perm ->
                perm in dangerousPermissions && pm.checkPermission(perm, pkg.packageName) == PackageManager.PERMISSION_GRANTED
            }

            val risk = when {
                granted.any { it in HIGH_RISK } -> AppPrivacyInfo.RiskLevel.HIGH
                granted.size >= 5 -> AppPrivacyInfo.RiskLevel.HIGH
                granted.any { it in MED_RISK } -> AppPrivacyInfo.RiskLevel.MEDIUM
                granted.isNotEmpty() -> AppPrivacyInfo.RiskLevel.LOW
                else -> AppPrivacyInfo.RiskLevel.SAFE
            }

            AppPrivacyInfo(
                packageName = pkg.packageName,
                appName = pm.getApplicationLabel(appInfo).toString(),
                icon = runCatching { pm.getApplicationIcon(pkg.packageName) }.getOrNull(),
                dangerousPermissions = granted,
                riskLevel = risk,
                lastUsedDays = null,
                isSystemApp = isSystem,
            )
        }.sortedByDescending { it.riskScore }

        emit(result)
    }.flowOn(Dispatchers.IO)

    companion object {
        private val HIGH_RISK = setOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
        private val MED_RISK = setOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    }
}
