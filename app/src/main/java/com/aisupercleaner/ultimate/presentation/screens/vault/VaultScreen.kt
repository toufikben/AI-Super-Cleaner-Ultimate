package com.aisupercleaner.ultimate.presentation.screens.vault

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aisupercleaner.ultimate.R
import com.aisupercleaner.ultimate.core.util.Formatter
import com.aisupercleaner.ultimate.data.vault.VaultFile
import com.aisupercleaner.ultimate.presentation.screens.vault.components.PinSetupDialog
import com.aisupercleaner.ultimate.presentation.screens.vault.components.VaultFileGrid
import com.aisupercleaner.ultimate.presentation.screens.vault.components.VaultLockScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    viewModel: VaultViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    var showPinSetup by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<VaultFile?>(null) }

    val pickFilesLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) viewModel.importFiles(uris, deleteOriginal = false)
    }

    if (state.isLocked) {
        VaultLockScreen(
            state = state,
            onPinChanged = viewModel::onPinChanged,
            onSubmitPin = viewModel::unlockWithPin,
            onBiometric = { act -> viewModel.authenticateWithBiometric(act) },
            onSetupPin = { showPinSetup = true },
        )
        if (showPinSetup) {
            PinSetupDialog(
                onConfirm = { pin -> if (viewModel.setupPin(pin)) showPinSetup = false },
                onDismiss = { showPinSetup = false },
            )
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.vault_title), fontWeight = FontWeight.SemiBold)
                        Text(
                            stringResource(R.string.vault_stats, state.files.size, Formatter.formatBytes(state.totalBytes)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = null) } },
                actions = { IconButton(onClick = viewModel::lock) { Icon(Icons.Rounded.Lock, contentDescription = stringResource(R.string.vault_lock)) } },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { pickFilesLauncher.launch(arrayOf("*/*")) },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.vault_add_files)) },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (state.isEmpty) EmptyVaultView()
            else VaultFileGrid(
                files = state.files,
                onFileClick = viewModel::requestPreview,
                onFileLongClick = { file -> pendingDelete = file },
            )

            if (state.isImporting) {
                state.importProgress?.let { progress ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Surface(tonalElevation = 6.dp, shape = MaterialTheme.shapes.extraLarge) {
                            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(progress = { progress.fraction })
                                Spacer(Modifier.height(12.dp))
                                Text(stringResource(R.string.vault_importing, progress.currentIndex, progress.total))
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { file ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.vault_delete_title)) },
            text = { Text(stringResource(R.string.vault_delete_message, file.originalName)) },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteFile(file.id, shred = true); pendingDelete = null }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun EmptyVaultView() {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), modifier = Modifier.size(96.dp))
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.vault_empty_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.vault_empty_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}
