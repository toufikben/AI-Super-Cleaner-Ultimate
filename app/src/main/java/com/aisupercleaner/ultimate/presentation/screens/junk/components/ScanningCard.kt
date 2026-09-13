package com.aisupercleaner.ultimate.presentation.screens.junk.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import com.aisupercleaner.ultimate.R
import com.aisupercleaner.ultimate.data.scanner.ScanProgress

@Composable
fun ScanningCard(progress: ScanProgress, onCancel: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "scan_pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.9f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse",
    )

    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Box(
            modifier = Modifier.size(120.dp).scale(pulse).clip(CircleShape).background(colors.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(90.dp), color = colors.primary, strokeWidth = 4.dp)
        }
        Text(stringResource(progress.phase.titleRes()), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        if (progress.phase == ScanProgress.Phase.SCANNING) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                LinearProgressIndicator(progress = { progress.progress }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = colors.primary)
                Spacer(Modifier.height(8.dp))
                Text("${progress.scannedFiles} ملف تم فحصه", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
        }
        TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
    }
}

private fun ScanProgress.Phase.titleRes(): Int = when (this) {
    ScanProgress.Phase.IDLE -> R.string.scan_idle
    ScanProgress.Phase.ENUMERATING -> R.string.scan_enumerating
    ScanProgress.Phase.SCANNING -> R.string.scan_scanning
    ScanProgress.Phase.FINALIZING -> R.string.scan_finalizing
    ScanProgress.Phase.COMPLETED -> R.string.scan_completed
    ScanProgress.Phase.CANCELLED -> R.string.scan_cancelled
    ScanProgress.Phase.ERROR -> R.string.scan_error
}
