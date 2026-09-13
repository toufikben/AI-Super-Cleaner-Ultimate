package com.aisupercleaner.ultimate.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aisupercleaner.ultimate.core.permissions.PermissionManager
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

data class MainUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentStyle: AccentStyle = AccentStyle.OCEAN_BLUE,
    val dynamicColor: Boolean = true,
    val onboardingCompleted: Boolean = false,
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val preferences: AppPreferences,
    private val permissionManager: PermissionManager,
) : ViewModel() {

    val uiState: StateFlow<MainUiState> = combine(
        preferences.themeMode,
        preferences.accentStyle,
        preferences.dynamicColor,
        preferences.onboardingCompleted,
    ) { themeMode, accentStyle, dynamicColor, onboardingCompleted ->
        MainUiState(themeMode, accentStyle, dynamicColor, onboardingCompleted)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState(),
    )

    fun onThemeChanged(
        mode: ThemeMode? = null,
        accent: AccentStyle? = null,
        dynamic: Boolean? = null,
    ) = viewModelScope.launch {
        mode?.let { preferences.setThemeMode(it) }
        accent?.let { preferences.setAccentStyle(it) }
        dynamic?.let { preferences.setDynamicColor(it) }
    }

    fun onOnboardingCompleted() = viewModelScope.launch {
        preferences.setOnboardingCompleted(true)
    }

    fun needsPermissions(): Boolean = !permissionManager.canReadFiles()
}
