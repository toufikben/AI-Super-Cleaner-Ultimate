package com.aisupercleaner.ultimate.presentation.screens.shredder

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aisupercleaner.ultimate.R
import com.aisupercleaner.ultimate.core.util.Formatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShredderScreen(
    onBack: () -> Unit = {},
    viewModel: ShredderViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirm by remember { mutableStateOf(false) }

    val pickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        viewModel.onFilesPicked(uris, context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.shredder_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = null) } },
            )
        },
        bottomBar = {
            if (state.selectedFiles.isNotEmpty() && !state.isRunning) {
                Surface(tonalElevation = 8.dp, shadowElevation = 8.dp) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(Formatter.formatBytes(state.totalBytes), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(stringResource(R.string.shred_selected_count, state.selectedFiles.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = { confirm = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError),
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.height(48.dp),
                        ) {
                            Icon(Icons.Rounded.DeleteForever, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.shred_now), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (!state.isRunning) {
                ExtendedFloatingActionButton(
                    onClick = { pickerLauncher.launch(arrayOf("*/*")) },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.shred_add)) },
                )
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.phase == ShredderUiState.Phase.SHREDDING) {
                ShreddingIndicator(state.currentIndex, state.selectedFiles.size, onCancel = viewModel::cancelShred)
                return@Column
            }
            LevelSelector(selected = state.level, onSelect = viewModel::setLevel)
            if (state.selectedFiles.isEmpty()) EmptyShredView()
            else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(state.selectedFiles, key = { it.path }) { file ->
                        ShredFileRow(file.name, file.sizeBytes) { viewModel.removeFile(file.path) }
                    }
                    item { Spacer(Modifier.height(96.dp)) }
                }
            }
        }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            icon = { Icon(Icons.Rounded.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.shred_confirm_title)) },
            text = { Text(stringResource(R.string.shred_confirm_msg, state.selectedFiles.size, Formatter.formatBytes(state.totalBytes))) },
            confirmButton = {
                TextButton(onClick = { confirm = false; viewModel.startShred() }) {
                    Text(stringResource(R.string.shred_confirm_yes), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    if (state.phase == ShredderUiState.Phase.DONE) {
        val totalBytes = state.results.sumOf { it.bytesShredded }
        AlertDialog(
            onDismissRequest = viewModel::dismissResult,
            icon = { Icon(Icons.Rounded.CheckCircle, contentDescription = null) },
            title = { Text(stringResource(R.string.shred_done_title)) },
            text = { Text(stringResource(R.string.shred_done_msg, Formatter.formatBytes(totalBytes))) },
            confirmButton = { TextButton(onClick = viewModel::dismissResult) { Text(stringResource(R.string.ok)) } },
        )
    }
}

@Composable
private fun LevelSelector(selected: ShredderUiState.Level, onSelect: (ShredderUiState.Level) -> Unit) {
    Column(Modifier.padding(16.dp)) {
        Text(stringResource(R.string.shred_level_label), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        ShredderUiState.Level.entries.forEach { level ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = level == selected, onClick = { onSelect(level) })
                Column(Modifier.weight(1f)) {
                    Text(stringResource(level.titleRes), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(level.descRes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
    }
}

@Composable
private fun ShredFileRow(name: String, size: Long, onRemove: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.InsertDriveFile, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                Text(Formatter.formatBytes(size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onRemove) { Icon(Icons.Rounded.Close, contentDescription = null) }
        }
    }
}

@Composable
private fun ShreddingIndicator(current: Int, total: Int, onCancel: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        CircularProgressIndicator(Modifier.size(80.dp))
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.shredding_in_progress), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("$current / $total", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        OutlinedButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
    }
}

@Composable
private fun EmptyShredView() {
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Rounded.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), modifier = Modifier.size(96.dp))
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.shred_empty_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.shred_empty_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}
