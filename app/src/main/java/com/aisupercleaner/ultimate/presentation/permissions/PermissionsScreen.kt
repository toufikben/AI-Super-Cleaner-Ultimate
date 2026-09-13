package com.aisupercleaner.ultimate.presentation.permissions

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aisupercleaner.ultimate.R
import com.aisupercleaner.ultimate.core.permissions.PermissionType
import com.aisupercleaner.ultimate.presentation.permissions.components.PermissionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(
    onAllGranted: () -> Unit,
    viewModel: PermissionsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { viewModel.refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.perm_screen_title)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
                Column(Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding()) {
                    if (!state.canProceed) {
                        Text(stringResource(R.string.perm_required_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    Button(
                        onClick = { if (state.canProceed) onAllGranted() },
                        enabled = state.canProceed,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Text(stringResource(R.string.continue_label), fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Rounded.ArrowForward, contentDescription = null)
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { IntroHeader(state.grantedCount, state.totalApplicable) }
            items(PermissionType.entries.toList(), key = { it.name }) { type ->
                if (type.isSupportedOnThisDevice()) {
                    PermissionCard(
                        type = type,
                        status = viewModel.statusFor(type),
                        onRequest = {
                            viewModel.markRequested(type)
                            when (type) {
                                PermissionType.ALL_FILES -> runCatching { context.startActivity(viewModel.allFilesIntent()) }
                                PermissionType.BIOMETRIC, PermissionType.QUERY_PACKAGES -> Unit
                                else -> {
                                    val perms = type.manifestPermissions()
                                    if (perms.isNotEmpty()) launcher.launch(perms)
                                }
                            }
                        },
                        onOpenSettings = { runCatching { context.startActivity(viewModel.appSettingsIntent()) } },
                    )
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun IntroHeader(granted: Int, total: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(stringResource(R.string.perm_intro_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.perm_intro_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f))
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { if (total > 0) granted.toFloat() / total else 0f },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.perm_progress, granted, total), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}
