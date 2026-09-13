package com.aisupercleaner.ultimate.presentation.screens.wifi

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aisupercleaner.ultimate.R
import com.aisupercleaner.ultimate.data.wifi.WifiSecurityReport

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiSecurityScreen(onBack: () -> Unit = {}, viewModel: WifiViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.wifi_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = null) } },
                actions = { IconButton(onClick = viewModel::scan) { Icon(Icons.Rounded.Refresh, contentDescription = null) } },
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                return@Column
            }
            val report = state.report ?: return@Column

            val (statusColor, statusIcon, statusText) = when (report.riskLevel) {
                WifiSecurityReport.RiskLevel.HIGH -> Triple(Color(0xFFC62828), Icons.Rounded.Warning, stringResource(R.string.wifi_risk_high))
                WifiSecurityReport.RiskLevel.MEDIUM -> Triple(Color(0xFFE65100), Icons.Rounded.Warning, stringResource(R.string.wifi_risk_medium))
                WifiSecurityReport.RiskLevel.LOW -> Triple(Color(0xFF1565C0), Icons.Rounded.Info, stringResource(R.string.wifi_risk_low))
                WifiSecurityReport.RiskLevel.SAFE -> Triple(Color(0xFF2E7D32), Icons.Rounded.CheckCircle, stringResource(R.string.wifi_risk_safe))
            }

            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.15f))) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(statusText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = statusColor)
                        if (report.ssid.isNotBlank()) Text(report.ssid, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            InfoRow(stringResource(R.string.wifi_ssid), report.ssid.ifBlank { "—" })
            InfoRow(stringResource(R.string.wifi_security), report.securityType.name)
            InfoRow(stringResource(R.string.wifi_ip), report.ipAddress ?: "—")
            InfoRow(stringResource(R.string.wifi_connected), if (report.isConnected) "✓" else "✗")

            if (report.issues.isNotEmpty()) {
                Text(stringResource(R.string.wifi_issues), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                report.issues.forEach { issue ->
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                            Icon(
                                Icons.Rounded.Info,
                                contentDescription = null,
                                tint = when (issue.severity) {
                                    WifiSecurityReport.Issue.Severity.HIGH -> Color(0xFFC62828)
                                    WifiSecurityReport.Issue.Severity.MEDIUM -> Color(0xFFE65100)
                                    WifiSecurityReport.Issue.Severity.LOW -> Color(0xFF1565C0)
                                },
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(issue.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(issue.description, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
