package com.aisupercleaner.ultimate.presentation.screens.duplicates

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
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
import com.aisupercleaner.ultimate.presentation.screens.duplicates.components.DuplicateGroupCard
import com.aisupercleaner.ultimate.presentation.screens.duplicates.components.DuplicatesHeaderCard
import com.aisupercleaner.ultimate.presentation.screens.duplicates.components.KeepPolicySelector
import com.aisupercleaner.ultimate.presentation.screens.duplicates.components.ScanningView
import com.aisupercleaner.ultimate.presentation.screens.junk.components.ConfirmCleanupDialog
import com.aisupercleaner.ultimate.presentation.screens.junk.components.ResultDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicatesScreen(
    onBack: () -> Unit = {},
    adManager: AdManager? = null,
    isPremium: Boolean = false,
    viewModel: DuplicatesViewModel = hiltViewModel(),
) {
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.duplicates_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = null) } }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (state.phase) {
                DuplicatesUiState.Phase.IDLE -> IdleView { viewModel.startScan(includeSimilarImages = true) }
                DuplicatesUiState.Phase.SCANNING -> ScanningView(state.progress, viewModel::cancelScan)
                DuplicatesUiState.Phase.READY, DuplicatesUiState.Phase.DELETING, DuplicatesUiState.Phase.DONE ->
                    ReadyView(
                        state = state,
                        onToggleGroup = viewModel::toggleGroup,
                        onToggleItem = viewModel::toggleItem,
                        onToggleExpand = viewModel::toggleExpanded,
                        onPolicyChange = viewModel::setKeepPolicy,
                        onSelectAll = viewModel::selectAll,
                        onDeselectAll = viewModel::deselectAll,
                        onRescan = { viewModel.startScan(includeSimilarImages = true) },
                    )
                DuplicatesUiState.Phase.ERROR -> ErrorView(
                    message = state.errorMessage ?: "Unknown",
                    onRetry = { viewModel.startScan(includeSimilarImages = true) },
                    onDismiss = viewModel::dismissError,
                    onOpenSettings = { runCatching { context.startActivity(viewModel.openAllFilesSettings()) } },
                )
            }

            AnimatedVisibility(
                visible = state.phase == DuplicatesUiState.Phase.READY && state.totalSelectedCount > 0,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                DeleteBar(state.totalSelectedBytes, state.totalSelectedCount, onDelete = { showConfirm = true })
            }

            if (state.phase == DuplicatesUiState.Phase.DELETING) {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
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
    }

    if (showConfirm) {
        ConfirmCleanupDialog(
            count = state.totalSelectedCount,
            bytes = state.totalSelectedBytes,
            onConfirm = { showConfirm = false; viewModel.deleteSelected() },
            onDismiss = { showConfirm = false },
        )
    }

    state.lastResult?.let { ResultDialog(result = it, onDismiss = viewModel::dismissResult) }
}

@Composable
private fun IdleView(onScan: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(stringResource(R.string.duplicates_intro_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.duplicates_intro_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(32.dp))
        Button(onClick = onScan, modifier = Modifier.fillMaxWidth().height(56.dp), shape = MaterialTheme.shapes.large) {
            Icon(Icons.Rounded.Search, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.duplicates_scan_button), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ReadyView(
    state: DuplicatesUiState,
    onToggleGroup: (com.aisupercleaner.ultimate.data.scanner.duplicates.DuplicateGroup) -> Unit,
    onToggleItem: (String) -> Unit,
    onToggleExpand: (String) -> Unit,
    onPolicyChange: (DuplicatesUiState.KeepPolicy) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onRescan: () -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            DuplicatesHeaderCard(
                groupsCount = state.totalGroups,
                duplicatesCount = state.totalDuplicates,
                wastedBytes = state.totalWastedBytes,
                selectedBytes = state.totalSelectedBytes,
                selectedCount = state.totalSelectedCount,
            )
        }
        item { KeepPolicySelector(selected = state.keepPolicy, onSelect = onPolicyChange) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onSelectAll, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.select_all)) }
                OutlinedButton(onClick = onDeselectAll, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.deselect_all)) }
                OutlinedButton(onClick = onRescan, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }
        items(state.groups, key = { it.id }) { group ->
            DuplicateGroupCard(
                group = group,
                expanded = group.id in state.expandedGroups,
                selectedPaths = state.selectedPaths,
                keepPolicy = state.keepPolicy,
                onToggleExpand = { onToggleExpand(group.id) },
                onToggleGroup = { onToggleGroup(group) },
                onToggleItem = onToggleItem,
            )
        }
        item { Spacer(Modifier.height(96.dp)) }
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit, onDismiss: () -> Unit, onOpenSettings: () -> Unit) {
    val isPermission = message == "MISSING_PERMISSION"
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(if (isPermission) stringResource(R.string.perm_required) else stringResource(R.string.scan_error), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(if (isPermission) stringResource(R.string.duplicates_need_permission) else message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            Button(onClick = if (isPermission) onOpenSettings else onRetry) {
                Text(if (isPermission) stringResource(R.string.perm_grant) else stringResource(R.string.retry))
            }
        }
    }
}

@Composable
private fun DeleteBar(bytes: Long, count: Int, onDelete: () -> Unit) {
    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, tonalElevation = 8.dp, shadowElevation = 8.dp) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(Formatter.formatBytes(bytes), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("$count عنصر", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(
                onClick = onDelete,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError),
            ) {
                Icon(Icons.Rounded.Delete, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.duplicates_delete_selected), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
