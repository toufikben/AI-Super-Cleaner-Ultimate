package com.aisupercleaner.ultimate.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Route(val path: String) {
    data object Splash : Route("splash")
    data object Onboarding : Route("onboarding")
    data object Permissions : Route("permissions")
    data object Main : Route("main")
    data object Premium : Route("premium")
    data object Dashboard : Route("dashboard")
    data object Junk : Route("junk")
    data object Tools : Route("tools")
    data object Settings : Route("settings")
    data object Duplicates : Route("duplicates")
    data object LargeFiles : Route("large_files")
    data object Vault : Route("vault")
    data object Shredder : Route("shredder")
    data object Privacy : Route("privacy")
    data object Wifi : Route("wifi")
    data object Scheduler : Route("scheduler")
    data object History : Route("history")
    data object Rules : Route("rules")
    data object Theme : Route("theme")
    data object Language : Route("language")
    data object RamBooster : Route("tools/ram")
    data object Battery : Route("tools/battery")
}

enum class BottomTab(val route: Route, val labelRes: Int, val icon: ImageVector) {
    DASHBOARD(Route.Dashboard, com.aisupercleaner.ultimate.R.string.tab_dashboard, Icons.Rounded.Dashboard),
    JUNK(Route.Junk, com.aisupercleaner.ultimate.R.string.tab_junk, Icons.Rounded.CleaningServices),
    TOOLS(Route.Tools, com.aisupercleaner.ultimate.R.string.tab_tools, Icons.Rounded.Build),
    SETTINGS(Route.Settings, com.aisupercleaner.ultimate.R.string.tab_settings, Icons.Rounded.Settings),
}
