package com.aisupercleaner.ultimate.presentation.screens.tools

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aisupercleaner.ultimate.R

data class ToolItem(
    val id: String,
    val titleRes: Int,
    val subtitleRes: Int,
    val icon: ImageVector,
    val isPro: Boolean = false,
)

@Composable
fun ToolsScreen(isPremium: Boolean = false, onNavigateToPremium: () -> Unit = {}, onNavigate: (String) -> Unit = {}) {
    val cleaningTools = listOf(
        ToolItem("duplicates", R.string.action_duplicates, R.string.action_duplicates_sub, Icons.Rounded.ContentCopy),
        ToolItem("large_files", R.string.large_files_title, R.string.action_large_files_sub, Icons.Rounded.FolderOpen),
        ToolItem("shredder", R.string.shredder_title, R.string.shredder_title, Icons.Rounded.DeleteForever, true),
    )
    val securityTools = listOf(
        ToolItem("vault", R.string.vault_title, R.string.action_vault_sub, Icons.Rounded.Lock, true),
        ToolItem("privacy", R.string.privacy_title, R.string.privacy_title, Icons.Rounded.Security),
        ToolItem("wifi", R.string.wifi_title, R.string.wifi_title, Icons.Rounded.Wifi),
    )
    val performanceTools = listOf(
        ToolItem("ram", R.string.ram_title, R.string.ram_title, Icons.Rounded.Memory),
        ToolItem("battery", R.string.battery_title, R.string.battery_title, Icons.Rounded.BatteryFull),
    )
    val automationTools = listOf(
        ToolItem("rules", R.string.rules_settings, R.string.rules_settings_sub, Icons.Rounded.Rule, true),
        ToolItem("scheduler", R.string.scheduler_title, R.string.scheduler_enable_desc, Icons.Rounded.Schedule),
        ToolItem("history", R.string.history_title, R.string.settings_history_sub, Icons.Rounded.History),
    )

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionHeader(stringResource(R.string.tools_cleaning)) }
        items(cleaningTools, key = { it.id }) { ToolCard(it, isPremium, onNavigateToPremium, onNavigate) }
        item { SectionHeader(stringResource(R.string.tools_security)) }
        items(securityTools, key = { it.id }) { ToolCard(it, isPremium, onNavigateToPremium, onNavigate) }
        item { SectionHeader(stringResource(R.string.tools_performance)) }
        items(performanceTools, key = { it.id }) { ToolCard(it, isPremium, onNavigateToPremium, onNavigate) }
        item { SectionHeader(stringResource(R.string.tools_automation)) }
        items(automationTools, key = { it.id }) { ToolCard(it, isPremium, onNavigateToPremium, onNavigate) }
        item { Spacer(Modifier.height(96.dp)) }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp, start = 4.dp))
}

@Composable
private fun ToolCard(tool: ToolItem, isPremium: Boolean, onNavigateToPremium: () -> Unit, onNavigate: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { if (tool.isPro && !isPremium) onNavigateToPremium() else onNavigate(tool.id) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium) {
                Icon(tool.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(10.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(tool.titleRes), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    if (tool.isPro) {
                        Spacer(Modifier.width(6.dp))
                        Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = MaterialTheme.shapes.small) {
                            Text("PRO", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                    }
                }
                Text(stringResource(tool.subtitleRes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
