package com.aisupercleaner.ultimate.presentation.screens.ram

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aisupercleaner.ultimate.R
import com.aisupercleaner.ultimate.core.util.Formatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RamBoosterScreen(onBack: () -> Unit = {}, viewModel: RamViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ram_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = null) } },
                actions = { IconButton(onClick = viewModel::refresh) { Icon(Icons.Rounded.Refresh, contentDescription = null) } },
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            RamGauge(state.info.usedPercent, state.info.usedBytes, state.info.totalBytes)
            InfoRow("متاح", Formatter.formatBytes(state.info.availBytes))
            InfoRow("مُستخدَم", Formatter.formatBytes(state.info.usedBytes))
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = viewModel::boost,
                enabled = !state.isBoosting,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.large,
            ) {
                if (state.isBoosting) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("جاري التحرير…", fontWeight = FontWeight.SemiBold)
                } else {
                    Icon(Icons.Rounded.Speed, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.ram_boost), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (state.showSuccess) {
        AlertDialog(
            onDismissRequest = viewModel::dismissSuccess,
            icon = { Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("تم التحرير") },
            text = { Text("تم تحرير ${Formatter.formatBytes(state.lastFreedBytes)} من الذاكرة.") },
            confirmButton = { TextButton(onClick = viewModel::dismissSuccess) { Text(stringResource(R.string.ok)) } },
        )
    }
}

@Composable
private fun RamGauge(usedPercent: Float, usedBytes: Long, totalBytes: Long) {
    val animated by animateFloatAsState(targetValue = usedPercent, animationSpec = tween(900), label = "ram_progress")
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
        CircularProgressIndicator(progress = { 1f }, modifier = Modifier.size(220.dp), color = MaterialTheme.colorScheme.surfaceVariant, strokeWidth = 18.dp)
        CircularProgressIndicator(
            progress = { animated },
            modifier = Modifier.size(220.dp),
            color = when { animated > 0.85f -> MaterialTheme.colorScheme.error; animated > 0.7f -> MaterialTheme.colorScheme.tertiary; else -> MaterialTheme.colorScheme.primary },
            strokeWidth = 18.dp,
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${(animated * 100).toInt()}%", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
            Text(Formatter.formatBytes(usedBytes), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("من ${Formatter.formatBytes(totalBytes)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
