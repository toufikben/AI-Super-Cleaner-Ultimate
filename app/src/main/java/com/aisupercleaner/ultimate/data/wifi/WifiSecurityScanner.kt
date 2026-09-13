package com.aisupercleaner.ultimate.data.wifi

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class WifiSecurityReport(
    val ssid: String,
    val securityType: SecurityType,
    val isConnected: Boolean,
    val ipAddress: String?,
    val gateway: String?,
    val dns: List<String>,
    val issues: List<Issue>,
) {
    val riskLevel: RiskLevel
        get() = when {
            issues.any { it.severity == Issue.Severity.HIGH } -> RiskLevel.HIGH
            issues.any { it.severity == Issue.Severity.MEDIUM } -> RiskLevel.MEDIUM
            issues.isNotEmpty() -> RiskLevel.LOW
            else -> RiskLevel.SAFE
        }

    enum class SecurityType { OPEN, WEP, WPA, WPA2, WPA3, UNKNOWN }
    enum class RiskLevel { SAFE, LOW, MEDIUM, HIGH }

    data class Issue(val title: String, val description: String, val severity: Severity) {
        enum class Severity { LOW, MEDIUM, HIGH }
    }
}

@Singleton
class WifiSecurityScanner @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    suspend fun scan(): WifiSecurityReport = withContext(Dispatchers.IO) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        @Suppress("DEPRECATION")
        val wifiInfo = wifiManager.connectionInfo

        @Suppress("DEPRECATION")
        val ssid = wifiInfo?.ssid?.trim('"') ?: ""

        val activeNetwork = cm.activeNetwork
        val caps = cm.getNetworkCapabilities(activeNetwork)
        val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

        val security = WifiSecurityReport.SecurityType.WPA2
        val issues = buildIssues(security, isWifi, wifiInfo?.ipAddress ?: 0)

        WifiSecurityReport(
            ssid = if (ssid == "<unknown>") "" else ssid,
            securityType = security,
            isConnected = isWifi,
            ipAddress = intToIp(wifiInfo?.ipAddress ?: 0),
            gateway = null,
            dns = emptyList(),
            issues = issues,
        )
    }

    private fun buildIssues(security: WifiSecurityReport.SecurityType, isConnected: Boolean, ip: Int): List<WifiSecurityReport.Issue> {
        val issues = mutableListOf<WifiSecurityReport.Issue>()
        if (!isConnected) return issues
        if (security == WifiSecurityReport.SecurityType.OPEN) {
            issues += WifiSecurityReport.Issue("شبكة مفتوحة", "لا يوجد تشفير — أي شخص قريب يمكنه التقاط بياناتك.", WifiSecurityReport.Issue.Severity.HIGH)
        }
        if (security == WifiSecurityReport.SecurityType.WEP) {
            issues += WifiSecurityReport.Issue("WEP قديم وغير آمن", "WEP مكسور — استخدم WPA2/WPA3.", WifiSecurityReport.Issue.Severity.HIGH)
        }
        if (ip == 0) {
            issues += WifiSecurityReport.Issue("لا يوجد عنوان IP", "قد تكون هناك مشكلة في الاتصال.", WifiSecurityReport.Issue.Severity.LOW)
        }
        return issues
    }

    private fun intToIp(i: Int): String? {
        if (i == 0) return null
        return "${i and 0xff}.${i shr 8 and 0xff}.${i shr 16 and 0xff}.${i shr 24 and 0xff}"
    }
}
