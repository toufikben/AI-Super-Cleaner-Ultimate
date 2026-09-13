package com.aisupercleaner.ultimate.presentation.screens.duplicates.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aisupercleaner.ultimate.R
import com.aisupercleaner.ultimate.core.util.Formatter

@Composable
fun DuplicatesHeaderCard(
    groupsCount: Int,
    duplicatesCount: Int,
    wastedBytes: Long,
    selectedBytes: Long,
    selectedCount: Int,
) {
    val colors = MaterialTheme.colorScheme
    Card(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = colors.primaryContainer),
    ) {
        Column(
            Modifier.fillMaxWidth().background(
                Brush.linearGradient(listOf(colors.primaryContainer, colors.secondaryContainer.copy(alpha = 0.6f)))
            ).padding(20.dp)
        ) {
            Text(
                stringResource(R.string.duplicates_found_wasted, Formatter.formatBytes(wastedBytes)),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = colors.onPrimaryContainer,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.duplicates_found_groups, groupsCount) + " · $duplicatesCount تكرار",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onPrimaryContainer.copy(alpha = 0.8f),
            )
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(stringResource(R.string.duplicates_selected_label), style = MaterialTheme.typography.labelSmall, color = colors.onPrimaryContainer.copy(alpha = 0.7f))
                    Text(Formatter.formatBytes(selectedBytes), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.onPrimaryContainer)
                }
                Text("$selectedCount عنصر", style = MaterialTheme.typography.bodySmall, color = colors.onPrimaryContainer.copy(alpha = 0.7f))
            }
        }
    }
}
