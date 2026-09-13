package com.aisupercleaner.ultimate.presentation.screens.duplicates.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aisupercleaner.ultimate.R
import com.aisupercleaner.ultimate.core.util.Formatter
import com.aisupercleaner.ultimate.data.scanner.duplicates.DuplicateGroup
import com.aisupercleaner.ultimate.presentation.screens.duplicates.DuplicatesUiState

@Composable
fun DuplicateGroupCard(
    group: DuplicateGroup,
    expanded: Boolean,
    selectedPaths: Set<String>,
    keepPolicy: DuplicatesUiState.KeepPolicy,
    onToggleExpand: () -> Unit,
    onToggleGroup: () -> Unit,
    onToggleItem: (String) -> Unit,
) {
    val keepPath = when (keepPolicy) {
        DuplicatesUiState.KeepPolicy.OLDEST -> group.items.minByOrNull { it.lastModified }?.path
        DuplicatesUiState.KeepPolicy.NEWEST -> group.items.maxByOrNull { it.lastModified }?.path
        DuplicatesUiState.KeepPolicy.SHORTEST_PATH -> group.items.minByOrNull { it.path.length }?.path
    }
    val selectedInGroup = group.items.count { it.path in selectedPaths && it.path != keepPath }
    val totalSelectable = (group.count - 1).coerceAtLeast(1)
    val allSelected = selectedInGroup >= totalSelectable

    Card(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().clickable { onToggleExpand() }.padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = allSelected, onCheckedChange = { onToggleGroup() })
                Box(
                    Modifier.size(48.dp).clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = when (group.kind) {
                            DuplicateGroup.Kind.EXACT -> Icons.Rounded.ContentCopy
                            DuplicateGroup.Kind.SIMILAR -> Icons.Rounded.PhotoLibrary
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(
                            when (group.kind) {
                                DuplicateGroup.Kind.EXACT -> R.string.duplicates_kind_exact
                                DuplicateGroup.Kind.SIMILAR -> R.string.duplicates_kind_similar
                            }
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(stringResource(R.string.duplicates_items_count, group.count), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape) {
                            Text(
                                stringResource(R.string.duplicates_wasted_label, Formatter.formatBytes(group.wastedBytes)),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Text("$selectedInGroup/$totalSelectable", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                val rotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f, animationSpec = tween(250), label = "rot")
                Icon(Icons.Rounded.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.rotate(rotation))
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)).padding(vertical = 4.dp)
                ) {
                    group.items.forEach { item ->
                        DuplicateItemRow(
                            item = item,
                            selected = item.path in selectedPaths,
                            isKept = item.path == keepPath,
                            onToggle = { onToggleItem(item.path) },
                        )
                    }
                }
            }
        }
    }
}
