package com.aisupercleaner.ultimate.presentation.components

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle

@Composable
fun AnimatedInt(
    value: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    durationMs: Int = 700,
) {
    val animated by animateIntAsState(targetValue = value, animationSpec = tween(durationMs), label = "animated_int")
    Text(text = animated.toString(), modifier = modifier, style = style)
}
