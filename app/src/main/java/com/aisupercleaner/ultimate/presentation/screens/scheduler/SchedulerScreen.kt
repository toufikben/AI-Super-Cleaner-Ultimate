package com.aisupercleaner.ultimate.presentation.screens.scheduler

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aisupercleaner.ultimate.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulerScreen(onBack: () -> Unit = {}, viewModel: SchedulerViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val intervals = listOf(6 to R.string.scheduler_interval_6, 12 to R.string.scheduler_interval_12, 24 to R.string.scheduler_interval_24, 72 to R.string.scheduler_interval_72, 168 to R.string.scheduler_interval_168)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.scheduler_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = null) } },
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Autorenew, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.scheduler_enable), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.scheduler_enable_desc), style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = state.autoCleanEnabled, onCheckedChange = viewModel::setEnabled)
                }
            }

            Text(stringResource(R.string.scheduler_interval), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            intervals.forEach { (hours, label) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = state.intervalHours == hours, onClick = { viewModel.setInterval(hours) }, enabled = state.autoCleanEnabled)
                    Text(stringResource(label), style = MaterialTheme.typography.bodyLarge)
                }
            }

            Spacer(Modifier.height(8.dp))
            Button(onClick = viewModel::runNow, modifier = Modifier.fillMaxWidth().height(52.dp), shape = MaterialTheme.shapes.large) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.scheduler_run_now), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
