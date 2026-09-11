package com.aisupercleaner.ultimate.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Navy = Color(0xFF102A43)
private val Teal = Color(0xFF159A9C)
private val Mint = Color(0xFFE8F7F4)
private val Cloud = Color(0xFFF6F9FC)
private val Ink = Color(0xFF172B4D)

private val LightColors = lightColorScheme(
    primary = Navy,
    secondary = Teal,
    tertiary = Color(0xFF6B5DD3),
    background = Cloud,
    surface = Color.White,
    onPrimary = Color.White,
    onBackground = Ink,
    onSurface = Ink
)

@Composable
fun AISuperCleanerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColors, typography = Typography(), content = content)
}

val ScoreTrack = Color(0xFFDCE8EF)
val ScoreAccent = Teal
val SoftMint = Mint
val SoftLavender = Color(0xFFF0EEFF)
val SoftBlue = Color(0xFFEAF3FF)
val SoftAmber = Color(0xFFFFF5DF)
