package com.aisupercleaner.ultimate.presentation.screens.duplicates.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aisupercleaner.ultimate.R
import com.aisupercleaner.ultimate.presentation.screens.duplicates.DuplicatesUiState

@Composable
fun ScanningView(progress: DuplicatesUiState.Progress, onCancel: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse",
    )

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(130.dp).scale(pulse).clip(CircleShape).background(colors.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.ContentCopy, contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(60.dp))
        }
        Spacer(Modifier.height(28.dp))
        Text(stringResource(R.string.duplicates_scanning_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(progress.titleRes()), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        if (progress.total > 0) {
            LinearProgressIndicator(progress = { progress.fraction }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = colors.primary)
            Spacer(Modifier.height(8.dp))
            Text("${progress.scanned} / ${progress.total}", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
        } else {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = colors.primary)
        }
        if (progress.currentPath.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(progress.currentPath, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 16.dp))
        }
        Spacer(Modifier.height(32.dp))
        TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
    }
}

private fun DuplicatesUiState.Progress.titleRes(): Int = when (phaseName) {
    "ENUMERATING" -> R.string.duplicates_phase_enumerating
    "HASHING_SIZE" -> R.string.duplicates_phase_size
    "HASHING_SHA" -> R.string.duplicates_phase_hash
    "HASHING_IMAGE" -> R.string.duplicates_phase_image
    else -> R.string.duplicates_scanning_title
}
