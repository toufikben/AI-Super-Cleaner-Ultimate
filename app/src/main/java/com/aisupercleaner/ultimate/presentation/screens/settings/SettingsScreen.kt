package com.aisupercleaner.ultimate.presentation.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aisupercleaner.ultimate.R

@Composable
fun SettingsScreen(
    onNavigateToTheme: () -> Unit = {},
    onNavigateToScheduler: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToVault: () -> Unit = {},
    onNavigateToPremium: () -> Unit = {},
    onNavigateToLanguage: () -> Unit = {},
    onNavigateToRules: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!state.isPremium) {
            Card(
                Modifier.fillMaxWidth().clickable(onClick = onNavigateToPremium),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.WorkspacePremium, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.settings_upgrade_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.settings_upgrade_sub), style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                }
            }
        }

        Section(stringResource(R.string.settings_appearance))
        RowItem(stringResource(R.string.settings_theme), stringResource(R.string.settings_theme_sub), Icons.Rounded.Palette, onNavigateToTheme)
        RowItem(stringResource(R.string.settings_language), stringResource(R.string.settings_language_sub), Icons.Rounded.Language, onNavigateToLanguage)

        Section(stringResource(R.string.settings_automation))
        SwitchRowItem(stringResource(R.string.settings_auto_clean), stringResource(R.string.settings_auto_clean_sub), Icons.Rounded.Autorenew, state.autoCleanEnabled, viewModel::setAutoClean)
        RowItem(stringResource(R.string.settings_scheduler), stringResource(R.string.settings_scheduler_sub), Icons.Rounded.Schedule, onNavigateToScheduler)
        RowItem(stringResource(R.string.rules_settings), stringResource(R.string.rules_settings_sub), Icons.Rounded.Rule, onNavigateToRules)

        Section(stringResource(R.string.settings_notifications))
        SwitchRowItem(stringResource(R.string.settings_notif_enable), stringResource(R.string.settings_notif_enable_sub), Icons.Rounded.Notifications, state.notificationsEnabled, viewModel::setNotifications)
        ThresholdRow(state.storageAlertThreshold, state.notificationsEnabled, viewModel::setThreshold)

        Section(stringResource(R.string.settings_security))
        RowItem(
            stringResource(R.string.settings_vault),
            if (state.vaultPinSet) stringResource(R.string.settings_vault_active) else stringResource(R.string.settings_vault_inactive),
            Icons.Rounded.Lock,
            onNavigateToVault,
        )

        Section(stringResource(R.string.settings_data))
        RowItem(stringResource(R.string.settings_history), stringResource(R.string.settings_history_sub), Icons.Rounded.History, onNavigateToHistory)

        Section(stringResource(R.string.settings_about))
        InfoRowItem(stringResource(R.string.settings_version), "1.0.0", Icons.Rounded.Info)

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Section(title: String) {
    Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp, start = 4.dp))
}

@Composable
private fun RowItem(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), elevation = CardDefaults.cardElevation(0.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SwitchRowItem(title: String, subtitle: String, icon: ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(0.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun InfoRowItem(title: String, value: String, icon: ImageVector) {
    Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(0.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ThresholdRow(threshold: Int, enabled: Boolean, onChange: (Int) -> Unit) {
    Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(0.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_alert_threshold), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text(stringResource(R.string.settings_alert_threshold_sub, threshold), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(8.dp))
            Slider(value = threshold.toFloat(), onValueChange = { onChange(it.toInt()) }, valueRange = 50f..95f, steps = 8, enabled = enabled)
        }
    }
}
