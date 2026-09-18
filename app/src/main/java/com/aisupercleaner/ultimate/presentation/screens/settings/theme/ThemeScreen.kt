package com.aisupercleaner.ultimate.presentation.screens.settings.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aisupercleaner.ultimate.R
import com.aisupercleaner.ultimate.presentation.theme.AccentStyle
import com.aisupercleaner.ultimate.presentation.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeScreen(onBack: () -> Unit = {}, viewModel: ThemeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.theme_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = null) } },
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            SectionTitle(stringResource(R.string.theme_mode))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(4.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        Row(
                            Modifier.fillMaxWidth().clickable { viewModel.setThemeMode(mode) }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                mode.icon(),
                                contentDescription = null,
                                tint = if (mode == state.themeMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                stringResource(mode.labelRes()),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (mode == state.themeMode) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.weight(1f),
                            )
                            RadioButton(selected = mode == state.themeMode, onClick = { viewModel.setThemeMode(mode) })
                        }
                    }
                }
            }

            if (state.dynamicAvailable) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.theme_dynamic), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(stringResource(R.string.theme_dynamic_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = state.dynamicColor, onCheckedChange = viewModel::setDynamic)
                    }
                }
            }

            SectionTitle(stringResource(R.string.theme_accent))
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxWidth().height(200.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                userScrollEnabled = false,
            ) {
                items(AccentStyle.entries.toList(), key = { it.name }) { style ->
                    AccentSwatch(style = style, selected = style == state.accentStyle && !state.dynamicColor, enabled = !state.dynamicColor) { viewModel.setAccent(style) }
                }
            }

            SectionTitle(stringResource(R.string.theme_preview))
            ThemePreviewCard()
        }
    }
}

@Composable
private fun AccentSwatch(style: AccentStyle, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val color = style.previewColor()
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clip(MaterialTheme.shapes.medium).clickable(enabled = enabled, onClick = onClick).padding(4.dp)) {
        Box(
            Modifier.size(52.dp).clip(CircleShape).background(if (enabled) color else color.copy(alpha = 0.4f))
                .border(
                    width = if (selected) 3.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(stringResource(style.displayNameRes), style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

@Composable
private fun ThemePreviewCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CleaningServices, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.theme_live_preview), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(progress = { 0.65f }, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = {}) { Text(stringResource(R.string.theme_preview_button_one)) }
                OutlinedButton(onClick = {}) { Text(stringResource(R.string.theme_preview_button_two)) }
                Button(onClick = {}) { Text(stringResource(R.string.theme_preview_button_three)) }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
}

private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
    ThemeMode.SYSTEM -> R.string.theme_system
}

private fun ThemeMode.icon(): androidx.compose.ui.graphics.vector.ImageVector = when (this) {
    ThemeMode.LIGHT -> Icons.Rounded.LightMode
    ThemeMode.DARK -> Icons.Rounded.DarkMode
    ThemeMode.SYSTEM -> Icons.Rounded.SettingsBrightness
}

private fun AccentStyle.previewColor(): Color = when (this) {
    AccentStyle.OCEAN_BLUE -> Color(0xFF1565C0)
    AccentStyle.ROYAL_PURPLE -> Color(0xFF6A4CBA)
    AccentStyle.EMERALD_GREEN -> Color(0xFF006A60)
    AccentStyle.SUNSET_ORANGE -> Color(0xFFB8500A)
    AccentStyle.CRIMSON_RED -> Color(0xFFB3261E)
    AccentStyle.TURQUOISE_TEAL -> Color(0xFF006780)
    AccentStyle.ROSE_PINK -> Color(0xFF984061)
    AccentStyle.MIDNIGHT_INDIGO -> Color(0xFF3F51B5)
}
