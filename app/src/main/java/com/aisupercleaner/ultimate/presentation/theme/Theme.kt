package com.aisupercleaner.ultimate.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun AISuperCleanerTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accentStyle: AccentStyle = AccentStyle.OCEAN_BLUE,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> accentStyle.toDarkColorScheme()
        else -> accentStyle.toLightColorScheme()
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(colorScheme = colorScheme, typography = AppTypography, content = content)
}

private fun AccentStyle.toLightColorScheme() = when (this) {
    AccentStyle.OCEAN_BLUE -> buildLightScheme(OceanBlue_PrimaryLight, OceanBlue_OnPrimaryLight, OceanBlue_PrimaryContainerLight, OceanBlue_OnPrimaryContainerLight)
    AccentStyle.ROYAL_PURPLE -> buildLightScheme(RoyalPurple_PrimaryLight, Color.White, RoyalPurple_ContainerLight, RoyalPurple_OnContainerLight)
    AccentStyle.EMERALD_GREEN -> buildLightScheme(EmeraldGreen_PrimaryLight, Color.White, EmeraldGreen_ContainerLight, EmeraldGreen_OnContainerLight)
    AccentStyle.SUNSET_ORANGE -> buildLightScheme(SunsetOrange_PrimaryLight, Color.White, SunsetOrange_ContainerLight, SunsetOrange_OnContainerLight)
    AccentStyle.CRIMSON_RED -> buildLightScheme(CrimsonRed_PrimaryLight, Color.White, CrimsonRed_ContainerLight, CrimsonRed_OnContainerLight)
    AccentStyle.TURQUOISE_TEAL -> buildLightScheme(TurquoiseTeal_PrimaryLight, Color.White, TurquoiseTeal_ContainerLight, TurquoiseTeal_OnContainerLight)
    AccentStyle.ROSE_PINK -> buildLightScheme(RosePink_PrimaryLight, Color.White, RosePink_ContainerLight, RosePink_OnContainerLight)
    AccentStyle.MIDNIGHT_INDIGO -> buildLightScheme(MidnightIndigo_PrimaryLight, Color.White, MidnightIndigo_ContainerLight, MidnightIndigo_OnContainerLight)
}

private fun AccentStyle.toDarkColorScheme() = when (this) {
    AccentStyle.OCEAN_BLUE -> buildDarkScheme(OceanBlue_PrimaryDark, OceanBlue_OnPrimaryDark, OceanBlue_PrimaryContainerDark, OceanBlue_OnPrimaryContainerDark)
    AccentStyle.ROYAL_PURPLE -> buildDarkScheme(RoyalPurple_PrimaryDark, Color(0xFF3A1E8C), RoyalPurple_ContainerDark, RoyalPurple_OnContainerDark)
    AccentStyle.EMERALD_GREEN -> buildDarkScheme(EmeraldGreen_PrimaryDark, Color(0xFF003731), EmeraldGreen_ContainerDark, EmeraldGreen_OnContainerDark)
    AccentStyle.SUNSET_ORANGE -> buildDarkScheme(SunsetOrange_PrimaryDark, Color(0xFF5C1F00), SunsetOrange_ContainerDark, SunsetOrange_OnContainerDark)
    AccentStyle.CRIMSON_RED -> buildDarkScheme(CrimsonRed_PrimaryDark, Color(0xFF690005), CrimsonRed_ContainerDark, CrimsonRed_OnContainerDark)
    AccentStyle.TURQUOISE_TEAL -> buildDarkScheme(TurquoiseTeal_PrimaryDark, Color(0xFF003544), TurquoiseTeal_ContainerDark, TurquoiseTeal_OnContainerDark)
    AccentStyle.ROSE_PINK -> buildDarkScheme(RosePink_PrimaryDark, Color(0xFF5E1133), RosePink_ContainerDark, RosePink_OnContainerDark)
    AccentStyle.MIDNIGHT_INDIGO -> buildDarkScheme(MidnightIndigo_PrimaryDark, Color(0xFF001A75), MidnightIndigo_ContainerDark, MidnightIndigo_OnContainerDark)
}

private fun buildLightScheme(primary: Color, onPrimary: Color, primaryContainer: Color, onPrimaryContainer: Color) = lightColorScheme(
    primary = primary, onPrimary = onPrimary, primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
    secondary = primary, onSecondary = onPrimary,
    background = Neutral_LightBackground, onBackground = Neutral_LightOnSurface,
    surface = Neutral_LightSurface, onSurface = Neutral_LightOnSurface,
    surfaceVariant = Neutral_LightSurfaceVariant, onSurfaceVariant = Neutral_LightOnSurfaceVariant,
    outline = Neutral_LightOutline, error = ErrorLight, onError = Color.White,
)

private fun buildDarkScheme(primary: Color, onPrimary: Color, primaryContainer: Color, onPrimaryContainer: Color) = darkColorScheme(
    primary = primary, onPrimary = onPrimary, primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
    secondary = primary, onSecondary = onPrimary,
    background = Neutral_DarkBackground, onBackground = Neutral_DarkOnSurface,
    surface = Neutral_DarkSurface, onSurface = Neutral_DarkOnSurface,
    surfaceVariant = Neutral_DarkSurfaceVariant, onSurfaceVariant = Neutral_DarkOnSurfaceVariant,
    outline = Neutral_DarkOutline, error = ErrorDark, onError = Color.Black,
)
