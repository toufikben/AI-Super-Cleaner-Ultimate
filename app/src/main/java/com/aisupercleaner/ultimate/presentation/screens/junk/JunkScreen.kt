package com.aisupercleaner.ultimate.presentation.screens.junk

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import android.app.Activity
import com.aisupercleaner.ultimate.ads.AdManager
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aisupercleaner.ultimate.R
import com.aisupercleaner.ultimate.core.util.Formatter
import com.aisupercleaner.ultimate.presentation.screens.junk.components.ConfirmCleanupDialog
import com.aisupercleaner.ultimate.presentation.screens.junk.components.JunkCategoryCard
import com.aisupercleaner.ultimate.presentation.screens.junk.components.JunkHeader
import com.aisupercleaner.ultimate.presentation.screens.junk.components.ResultDialog
import com.aisupercleaner.ultimate.presentation.screens.junk.components.ScanningCard

@Composable
fun JunkScreen(adManager: AdManager? = null, isPremium: Boolean = false, viewModel: JunkViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(state.lastResult) {
        val result = state.lastResult
        val activity = context as? Activity
        if (result != null && result.deletedCount > 0 && activity != null) {
            adManager?.showInterstitialAfterCleanup(activity, isPremium) {}
        }
    }
    var showConfirm by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        when (state.phase) {
            JunkUiState.Phase.IDLE -> IdleContent(onScan = viewModel::startScanWithPermissionCheck)
            JunkUiState.Phase.SCANNING -> ScanningCard(progress = state.progress, onCancel = viewModel::cancelScan)
            JunkUiState.Phase.READY, JunkUiState.Phase.CLEANING, JunkUiState.Phase.DONE -> ReadyContent(
                state = state,
                onToggleCategory = viewModel::toggleCategory,
                onToggleItem = viewModel::toggleSelection,
                onToggleExpand = viewModel::toggleExpanded,
                onSelectAll = viewModel::selectAll,
                onDeselectAll = viewModel::deselectAll,
                onRescan = viewModel::startScanWithPermissionCheck,
            )
            JunkUiState.Phase.ERROR -> ErrorContent(
                message = state.errorMessage ?: "Unknown",
                onRetry = viewModel::startScanWithPermissionCheck,
                onDismiss = viewModel::dismissError,
                onOpenPermissions = { runCatching { context.startActivity(viewModel.openAllFilesSettings()) } },
            )
        }

        AnimatedVisibility(
            visible = state.phase == JunkUiState.Phase.READY && state.totalSelectedCount > 0,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            CleanupBar(bytes = state.totalSelectedBytes, count = state.totalSelectedCount, onCleanClick = { showConfirm = true })
        }

        if (state.phase == JunkUiState.Phase.CLEANING) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Surface(tonalElevation = 6.dp, shape = MaterialTheme.shapes.extraLarge) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.cleaning_in_progress))
                    }
                }
            }
        }
    }

    if (showConfirm) {
        ConfirmCleanupDialog(
            count = state.totalSelectedCount,
            bytes = state.totalSelectedBytes,
            onConfirm = { showConfirm = false; viewModel.deleteSelected() },
            onDismiss = { showConfirm = false },
        )
    }

    state.lastResult?.let { result -> ResultDialog(result = result, onDismiss = viewModel::dismissResult) }
}

@Composable
private fun IdleContent(onScan: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.junk_idle_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.junk_idle_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(32.dp))
        Button(onClick = onScan, modifier = Modifier.fillMaxWidth().height(56.dp), shape = MaterialTheme.shapes.large) {
            Icon(Icons.Rounded.Refresh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.start_scan), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ReadyContent(
    state: JunkUiState,
    onToggleCategory: (com.aisupercleaner.ultimate.data.scanner.JunkCategory) -> Unit,
    onToggleItem: (String) -> Unit,
    onToggleExpand: (com.aisupercleaner.ultimate.data.scanner.JunkCategory) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onRescan: () -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { JunkHeader(selectedBytes = state.totalSelectedBytes, selectedCount = state.totalSelectedCount) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = if (state.allSelected) onDeselectAll else onSelectAll, modifier = Modifier.weight(1f)) {
                    Text(stringResource(if (state.allSelected) R.string.deselect_all else R.string.select_all))
                }
                OutlinedButton(onClick = onRescan, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.rescan))
                }
            }
        }
        items(state.groups, key = { it.category.name }) { group ->
            JunkCategoryCard(
                group = group,
                expanded = group.category in state.expandedCategories,
                selectedPaths = state.selectedPaths,
                onToggleExpand = { onToggleExpand(group.category) },
                onToggleCategory = { onToggleCategory(group.category) },
                onToggleItem = onToggleItem,
            )
        }
        item { Spacer(Modifier.height(96.dp)) }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit, onDismiss: () -> Unit, onOpenPermissions: () -> Unit) {
    val isPermission = message == "MISSING_PERMISSION"
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(if (isPermission) "صلاحيات ناقصة" else stringResource(R.string.scan_error), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(if (isPermission) "نحتاج إذن قراءة الملفات لفحص جهازك." else message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            Button(onClick = if (isPermission) onOpenPermissions else onRetry) {
                Text(if (isPermission) stringResource(R.string.perm_grant) else stringResource(R.string.retry))
            }
        }
    }
}

@Composable
private fun CleanupBar(bytes: Long, count: Int, onCleanClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(Formatter.formatBytes(bytes), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("$count عنصر محدد", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = onCleanClick, shape = MaterialTheme.shapes.large, modifier = Modifier.height(48.dp)) {
                Icon(Icons.Rounded.Delete, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.clean_now), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
