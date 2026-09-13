package com.aisupercleaner.ultimate.presentation.screens.settings.theme

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aisupercleaner.ultimate.data.preferences.AppPreferences
import com.aisupercleaner.ultimate.presentation.theme.AccentStyle
import com.aisupercleaner.ultimate.presentation.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ThemeUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentStyle: AccentStyle = AccentStyle.OCEAN_BLUE,
    val dynamicColor: Boolean = true,
    val dynamicAvailable: Boolean = false,
)

@HiltViewModel
class ThemeViewModel @Inject constructor(private val preferences: AppPreferences) : ViewModel() {

    val uiState: StateFlow<ThemeUiState> = combine(
        preferences.themeMode,
        preferences.accentStyle,
        preferences.dynamicColor,
    ) { mode, accent, dynamic ->
        ThemeUiState(mode, accent, dynamic, Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeUiState())

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { preferences.setThemeMode(mode) }
    fun setAccent(style: AccentStyle) = viewModelScope.launch { preferences.setAccentStyle(style) }
    fun setDynamic(enabled: Boolean) = viewModelScope.launch { preferences.setDynamicColor(enabled) }
}
