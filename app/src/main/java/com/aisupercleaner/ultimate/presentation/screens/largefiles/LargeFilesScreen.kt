package com.aisupercleaner.ultimate.presentation.screens.largefiles

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import android.app.Activity
import com.aisupercleaner.ultimate.ads.AdManager
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aisupercleaner.ultimate.R
import com.aisupercleaner.ultimate.core.util.Formatter
import com.aisupercleaner.ultimate.data.scanner.LargeFileScanner
import com.aisupercleaner.ultimate.presentation.screens.junk.components.ConfirmCleanupDialog
import com.aisupercleaner.ultimate.presentation.screens.junk.components.ResultDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LargeFilesScreen(
    onBack: () -> Unit = {},
    adManager: AdManager? = null,
    isPremium: Boolean = false,
    viewModel: LargeFilesViewModel = hiltViewModel(),
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
    var showFilterSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.large_files_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = null) } },
                actions = {
                    if (state.phase == LargeFilesUiState.Phase.READY) {
                        IconButton(onClick = { showFilterSheet = true }) { Icon(Icons.Rounded.Tune, contentDescription = null) }
                    }
                },
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = state.phase == LargeFilesUiState.Phase.READY && state.totalSelectedCount > 0,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            ) {
                DeleteBar(state.totalSelectedBytes, state.totalSelectedCount) { showConfirm = true }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (state.phase) {
                LargeFilesUiState.Phase.IDLE -> IdleView(viewModel::startScan, state.filter.minSizeBytes / (1024 * 1024))
                LargeFilesUiState.Phase.SCANNING -> ScanningView(state.scanned, state.currentPath, viewModel::cancelScan)
                LargeFilesUiState.Phase.READY, LargeFilesUiState.Phase.DELETING, LargeFilesUiState.Phase.DONE -> ReadyView(state, viewModel::toggleSelection, viewModel::selectAll, viewModel::deselectAll, viewModel::startScan)
                LargeFilesUiState.Phase.ERROR -> ErrorView(
                    state.errorMessage ?: "",
                    viewModel::startScan,
                    viewModel::dismissError,
                    { runCatching { context.startActivity(viewModel.openAllFilesSettings()) } },
                )
            }
            if (state.phase == LargeFilesUiState.Phase.DELETING) {
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

    if (showFilterSheet) {
        FilterSheet(
            current = state.filter,
            onApply = { viewModel.setFilter(it); showFilterSheet = false; viewModel.startScan() },
            onDismiss = { showFilterSheet = false },
        )
    }
}

@Composable
private fun IdleView(onScan: () -> Unit, minSizeMb: Long) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Rounded.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), modifier = Modifier.size(96.dp))
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.large_files_idle_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.large_files_idle_desc, minSizeMb), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        Button(onClick = onScan, modifier = Modifier.fillMaxWidth().height(56.dp), shape = MaterialTheme.shapes.large) {
            Icon(Icons.Rounded.Search, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.start_scan), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ScanningView(scanned: Int, path: String, onCancel: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        CircularProgressIndicator(Modifier.size(72.dp))
        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.large_files_scanning), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("$scanned ملف", color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (path.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(path, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        }
        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
    }
}

@Composable
private fun ReadyView(
    state: LargeFilesUiState,
    onToggle: (String) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onRescan: () -> Unit,
) {
    if (state.files.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(96.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.large_files_empty), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onRescan) { Text(stringResource(R.string.rescan)) }
        }
        return
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.large_files_found, state.files.size), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(Formatter.formatBytes(state.totalBytes), style = MaterialTheme.typography.bodySmall)
                    }
                    Text(Formatter.formatBytes(state.totalSelectedBytes), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = if (state.selectedPaths.size == state.files.size) onDeselectAll else onSelectAll,
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(if (state.selectedPaths.size == state.files.size) R.string.deselect_all else R.string.select_all)) }
                OutlinedButton(onClick = onRescan, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.rescan))
                }
            }
        }
        items(state.files, key = { it.path }) { file -> LargeFileCard(file, file.path in state.selectedPaths) { onToggle(file.path) } }
        item { Spacer(Modifier.height(96.dp)) }
    }
}

@Composable
private fun LargeFileCard(file: LargeFileScanner.LargeFile, selected: Boolean, onToggle: () -> Unit) {
    val dateFmt = remember(file.lastModified) { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onToggle),
        colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                Icon(file.category.icon(), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(file.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(file.folder, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(file.formattedSize, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    Text(" · ", style = MaterialTheme.typography.labelSmall)
                    Text(dateFmt.format(Date(file.lastModified)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Checkbox(checked = selected, onCheckedChange = { onToggle() })
        }
    }
}

@Composable
private fun DeleteBar(bytes: Long, count: Int, onDelete: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 8.dp, shadowElevation = 8.dp) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(Formatter.formatBytes(bytes), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("$count ملف", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(
                onClick = onDelete,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.height(48.dp),
            ) {
                Icon(Icons.Rounded.Delete, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.delete), fontWeight = FontWeight.SemiBold)
            }
        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheet(
    current: LargeFileScanner.Filter,
    onApply: (LargeFileScanner.Filter) -> Unit,
    onDismiss: () -> Unit,
) {
    var minSizeMb by remember { mutableStateOf(current.minSizeBytes / (1024 * 1024)) }
    var selectedCategories by remember { mutableStateOf(current.categories) }
    var olderThanDays by remember { mutableStateOf(current.olderThanDays) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(16.dp).navigationBarsPadding()) {
            Text("مرشحات", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            Text("الحد الأدنى للحجم: $minSizeMb MB", style = MaterialTheme.typography.bodyMedium)
            Slider(value = minSizeMb.toFloat(), onValueChange = { minSizeMb = it.toLong() }, valueRange = 10f..2048f)
            Spacer(Modifier.height(16.dp))
            Text("الفئات", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            LargeFileScanner.LargeFile.Category.entries.forEach { cat ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = cat in selectedCategories, onCheckedChange = { checked ->
                        selectedCategories = if (checked) selectedCategories + cat else selectedCategories - cat
                    })
                    Text(cat.name.lowercase().replaceFirstChar { it.uppercase() })
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("أقدم من (بالأيام)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(null, 7, 30, 90, 365).forEach { days ->
                    FilterChip(selected = olderThanDays == days, onClick = { olderThanDays = days }, label = { Text(days?.let { "$it يوم" } ?: "الكل") })
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    onApply(
                        LargeFileScanner.Filter(
                            minSizeBytes = minSizeMb * 1024 * 1024,
                            categories = selectedCategories.ifEmpty { LargeFileScanner.LargeFile.Category.entries.toSet() },
                            olderThanDays = olderThanDays,
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.large,
            ) { Text("تطبيق", fontWeight = FontWeight.SemiBold) }
        }
    }
}

private fun LargeFileScanner.LargeFile.Category.icon(): ImageVector = when (this) {
    LargeFileScanner.LargeFile.Category.VIDEO -> Icons.Rounded.Videocam
    LargeFileScanner.LargeFile.Category.AUDIO -> Icons.Rounded.Audiotrack
    LargeFileScanner.LargeFile.Category.IMAGE -> Icons.Rounded.Image
    LargeFileScanner.LargeFile.Category.ARCHIVE -> Icons.Rounded.FolderZip
    LargeFileScanner.LargeFile.Category.DOCUMENT -> Icons.Rounded.Description
    LargeFileScanner.LargeFile.Category.APK -> Icons.Rounded.Android
    LargeFileScanner.LargeFile.Category.OTHER -> Icons.Rounded.InsertDriveFile
}
