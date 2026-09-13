package com.aisupercleaner.ultimate.presentation.screens.duplicates.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aisupercleaner.ultimate.R
import com.aisupercleaner.ultimate.presentation.screens.duplicates.DuplicatesUiState

@Composable
fun KeepPolicySelector(
    selected: DuplicatesUiState.KeepPolicy,
    onSelect: (DuplicatesUiState.KeepPolicy) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(stringResource(R.string.duplicates_smart_select), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            DuplicatesUiState.KeepPolicy.entries.forEachIndexed { index, policy ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index, DuplicatesUiState.KeepPolicy.entries.size),
                    selected = policy == selected,
                    onClick = { onSelect(policy) },
                    label = { Text(stringResource(policy.labelRes()), style = MaterialTheme.typography.labelMedium) },
                )
            }
        }
    }
}

private fun DuplicatesUiState.KeepPolicy.labelRes(): Int = when (this) {
    DuplicatesUiState.KeepPolicy.OLDEST -> R.string.duplicates_keep_oldest
    DuplicatesUiState.KeepPolicy.NEWEST -> R.string.duplicates_keep_newest
    DuplicatesUiState.KeepPolicy.SHORTEST_PATH -> R.string.duplicates_keep_shortest
}
