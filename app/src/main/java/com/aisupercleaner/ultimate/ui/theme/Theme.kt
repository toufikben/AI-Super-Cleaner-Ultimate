package com.aisupercleaner.ultimate.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Navy = Color(0xFF102A43)
private val Teal = Color(0xFF159A9C)
private val Violet = Color(0xFF7357D8)
private val Coral = Color(0xFFE26D5A)
private val Mint = Color(0xFFE8F7F4)
private val Cloud = Color(0xFFF6F9FC)
private val Ink = Color(0xFF172B4D)

private fun accentFor(style: String): Color = when (style.lowercase()) {
    "violet" -> Violet
    "coral" -> Coral
    else -> Teal
}

@Composable
fun AISuperCleanerTheme(darkTheme: Boolean = false, accentStyle: String = "teal", content: @Composable () -> Unit) {
    val accent = accentFor(accentStyle)
    val colors = if (darkTheme) {
        darkColorScheme(
            primary = accent,
            secondary = accent.copy(alpha = .88f),
            tertiary = Violet,
            background = Color(0xFF0D1726),
            surface = Color(0xFF142235),
            surfaceVariant = Color(0xFF22334A),
            onPrimary = Color.White,
            onBackground = Color(0xFFEAF2FA),
            onSurface = Color(0xFFEAF2FA)
        )
    } else {
        lightColorScheme(
            primary = Navy,
            secondary = accent,
            tertiary = Violet,
            background = Cloud,
            surface = Color.White,
            onPrimary = Color.White,
            onBackground = Ink,
            onSurface = Ink
        )
    }
    MaterialTheme(colorScheme = colors, typography = Typography(), content = content)
}

val ScoreTrack = Color(0xFFDCE8EF)
val ScoreAccent = Teal
val SoftMint = Mint
val SoftLavender = Color(0xFFF0EEFF)
val SoftBlue = Color(0xFFEAF3FF)
val SoftAmber = Color(0xFFFFF5DF)
