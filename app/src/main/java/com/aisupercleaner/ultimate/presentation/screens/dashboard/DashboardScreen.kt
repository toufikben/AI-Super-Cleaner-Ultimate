package com.aisupercleaner.ultimate.presentation.screens.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aisupercleaner.ultimate.R
import com.aisupercleaner.ultimate.core.util.Formatter
import com.aisupercleaner.ultimate.presentation.components.AnimatedInt
import com.aisupercleaner.ultimate.presentation.screens.dashboard.components.ForecastCard

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToDuplicates: () -> Unit = {},
    onNavigateToJunk: () -> Unit = {},
    onNavigateToVault: () -> Unit = {},
    onNavigateToLargeFiles: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        StorageCard(
            used = state.storage.usedBytes,
            total = state.storage.totalBytes,
            free = state.storage.freeBytes,
            percent = state.storage.usedPercent,
            isLoading = state.isLoading,
            onRefresh = viewModel::refresh,
        )

        state.forecast?.let { forecast -> ForecastCard(forecast) }

        StatsRow(totalCleaned = state.totalCleanedBytes, cleanupCount = state.cleanupCount, lastClean = state.lastCleanTimestamp)

        SectionTitle(stringResource(R.string.dashboard_quick_actions))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickActionCard(stringResource(R.string.action_quick_clean), stringResource(R.string.action_quick_clean_sub), Icons.Rounded.CleaningServices, Modifier.weight(1f), onNavigateToJunk)
            QuickActionCard(stringResource(R.string.action_duplicates), stringResource(R.string.action_duplicates_sub), Icons.Rounded.ContentCopy, Modifier.weight(1f), onNavigateToDuplicates)
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickActionCard(stringResource(R.string.action_large_files), stringResource(R.string.action_large_files_sub), Icons.Rounded.FolderOpen, Modifier.weight(1f), onNavigateToLargeFiles)
            QuickActionCard(stringResource(R.string.action_vault), stringResource(R.string.action_vault_sub), Icons.Rounded.Lock, Modifier.weight(1f), onNavigateToVault)
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StorageCard(used: Long, total: Long, free: Long, percent: Float, isLoading: Boolean, onRefresh: () -> Unit) {
    val animatedProgress by animateFloatAsState(targetValue = if (isLoading) 0f else percent, animationSpec = tween(900), label = "sp")
    val colors = MaterialTheme.colorScheme

    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.primaryContainer),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            Modifier.fillMaxWidth().background(
                Brush.linearGradient(listOf(colors.primaryContainer, colors.secondaryContainer.copy(alpha = 0.5f)))
            ).padding(20.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.dashboard_storage_title), style = MaterialTheme.typography.labelLarge, color = colors.onPrimaryContainer.copy(alpha = 0.8f))
                    Spacer(Modifier.height(6.dp))
                    Text(if (isLoading) "…" else Formatter.formatBytes(used), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = colors.onPrimaryContainer)
                    Text(if (isLoading) "" else "من ${Formatter.formatBytes(total)}", style = MaterialTheme.typography.bodyMedium, color = colors.onPrimaryContainer.copy(alpha = 0.7f))
                }
                IconButton(onClick = onRefresh, enabled = !isLoading) {
                    Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.refresh), tint = colors.onPrimaryContainer)
                }
            }
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                color = colors.primary,
                trackColor = colors.onPrimaryContainer.copy(alpha = 0.15f),
                strokeCap = StrokeCap.Round,
            )
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${(animatedProgress * 100).toInt()}% مُستخدَم", style = MaterialTheme.typography.bodySmall, color = colors.onPrimaryContainer.copy(alpha = 0.85f))
                Text(if (isLoading) "…" else "متاح ${Formatter.formatBytes(free)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = colors.onPrimaryContainer.copy(alpha = 0.85f))
            }
        }
    }
}

@Composable
private fun StatsRow(totalCleaned: Long, cleanupCount: Int, lastClean: Long) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard(Icons.Rounded.AutoAwesome, Formatter.formatBytes(totalCleaned), stringResource(R.string.stat_total_cleaned), Modifier.weight(1f))
        StatCardInt(Icons.Rounded.History, cleanupCount, stringResource(R.string.stat_cleanups), Modifier.weight(1f))
        StatCard(Icons.Rounded.Schedule, Formatter.formatRelativeTime(lastClean), stringResource(R.string.stat_last_clean), Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(icon: ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
        }
    }
}

@Composable
private fun StatCardInt(icon: ImageVector, value: Int, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            AnimatedInt(value = value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
}

@Composable
private fun QuickActionCard(title: String, subtitle: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.height(130.dp),
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(22.dp))
            }
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}
