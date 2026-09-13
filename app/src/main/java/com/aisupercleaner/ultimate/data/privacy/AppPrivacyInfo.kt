package com.aisupercleaner.ultimate.data.privacy

import android.graphics.drawable.Drawable

data class AppPrivacyInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val dangerousPermissions: List<String>,
    val riskLevel: RiskLevel,
    val lastUsedDays: Int?,
    val isSystemApp: Boolean,
) {
    val riskScore: Int get() = when (riskLevel) {
        RiskLevel.HIGH -> 3
        RiskLevel.MEDIUM -> 2
        RiskLevel.LOW -> 1
        RiskLevel.SAFE -> 0
    }

    enum class RiskLevel { SAFE, LOW, MEDIUM, HIGH }
}
