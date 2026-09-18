package com.aisupercleaner.ultimate.presentation.screens.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aisupercleaner.ultimate.R
import com.aisupercleaner.ultimate.core.util.Formatter
import com.aisupercleaner.ultimate.data.history.HistoryEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onBack: () -> Unit = {}, viewModel: HistoryViewModel = hiltViewModel()) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = null) } },
            )
        }
    ) { padding ->
        if (entries.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Rounded.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), modifier = Modifier.size(96.dp))
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.history_empty), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            return@Scaffold
        }

        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                val totalFreed = entries.sumOf { it.freedBytes }
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(20.dp)) {
                        Text(stringResource(R.string.history_total_freed), style = MaterialTheme.typography.labelLarge)
                        Text(Formatter.formatBytes(totalFreed), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.history_total_sessions, entries.size), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            items(entries, key = { it.id }) { HistoryRow(it) }
        }
    }
}

@Composable
private fun HistoryRow(entry: HistoryEntry) {
    val fmt = SimpleDateFormat("dd MMM · HH:mm", Locale.getDefault())
    Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                Icon(
                    when (entry.source) {
                        HistoryEntry.Source.JUNK -> Icons.Rounded.CleaningServices
                        HistoryEntry.Source.DUPLICATES -> Icons.Rounded.ContentCopy
                        HistoryEntry.Source.LARGE_FILES -> Icons.Rounded.Folder
                        HistoryEntry.Source.VAULT -> Icons.Rounded.Lock
                        HistoryEntry.Source.AUTO -> Icons.Rounded.Autorenew
                        HistoryEntry.Source.SHREDDER -> Icons.Rounded.DeleteForever
                        HistoryEntry.Source.UNKNOWN -> Icons.Rounded.History
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(Formatter.formatBytes(entry.freedBytes), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.history_entry_summary, pluralStringResource(R.plurals.history_deleted_count, entry.deletedCount, entry.deletedCount), fmt.format(Date(entry.timestamp))), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
