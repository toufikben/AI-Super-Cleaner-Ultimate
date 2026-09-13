package com.aisupercleaner.ultimate.presentation.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aisupercleaner.ultimate.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onReady: () -> Unit) {
    val scale = remember { Animatable(0.6f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(1f, tween(700))
        alpha.animateTo(1f, tween(500))
        delay(400)
        onReady()
    }

    val colors = MaterialTheme.colorScheme
    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(colors.background, colors.surfaceVariant))),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(scale.value).alpha(alpha.value),
        ) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = colors.primary, modifier = Modifier.size(96.dp))
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = colors.onBackground)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.splash_tagline), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
    }
}
