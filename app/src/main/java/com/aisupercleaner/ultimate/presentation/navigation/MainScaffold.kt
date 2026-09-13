package com.aisupercleaner.ultimate.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavHostController
import com.aisupercleaner.ultimate.R
import com.aisupercleaner.ultimate.presentation.screens.battery.BatteryScreen
import com.aisupercleaner.ultimate.presentation.screens.dashboard.DashboardScreen
import com.aisupercleaner.ultimate.presentation.screens.duplicates.DuplicatesScreen
import com.aisupercleaner.ultimate.presentation.screens.history.HistoryScreen
import com.aisupercleaner.ultimate.presentation.screens.junk.JunkScreen
import com.aisupercleaner.ultimate.presentation.screens.largefiles.LargeFilesScreen
import com.aisupercleaner.ultimate.presentation.screens.privacy.PrivacyScreen
import com.aisupercleaner.ultimate.presentation.screens.ram.RamBoosterScreen
import com.aisupercleaner.ultimate.presentation.screens.rules.RulesScreen
import com.aisupercleaner.ultimate.presentation.screens.scheduler.SchedulerScreen
import com.aisupercleaner.ultimate.presentation.screens.settings.SettingsScreen
import com.aisupercleaner.ultimate.presentation.screens.settings.language.LanguageScreen
import com.aisupercleaner.ultimate.presentation.screens.settings.theme.ThemeScreen
import com.aisupercleaner.ultimate.presentation.screens.shredder.ShredderScreen
import com.aisupercleaner.ultimate.presentation.screens.tools.ToolsScreen
import com.aisupercleaner.ultimate.presentation.billing.BillingViewModel
import com.aisupercleaner.ultimate.ads.AdManager
import com.aisupercleaner.ultimate.billing.BillingManager
import com.aisupercleaner.ultimate.presentation.screens.vault.VaultScreen
import com.aisupercleaner.ultimate.presentation.screens.wifi.WifiSecurityScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(adManager: AdManager, billingManager: BillingManager, onNavigateToPremium: () -> Unit, navController: NavHostController = rememberNavController()) {
    val billingViewModel: BillingViewModel = hiltViewModel()
    val isPremium by billingViewModel.billing.isPremium.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val currentTab = remember(currentDestination) {
        BottomTab.entries.firstOrNull { tab -> currentDestination?.hierarchy?.any { it.route == tab.route.path } == true } ?: BottomTab.DASHBOARD
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(currentTab.labelRes), fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = onNavigateToPremium) {
                        BadgedBox(badge = { Badge(containerColor = MaterialTheme.colorScheme.tertiary) { Text("PRO") } }) {
                            Icon(Icons.Rounded.WorkspacePremium, contentDescription = stringResource(R.string.go_premium), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface, titleContentColor = MaterialTheme.colorScheme.onSurface),
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
                BottomTab.entries.forEach { tab ->
                    val selected = currentTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(tab.route.path) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = stringResource(tab.labelRes)) },
                        label = { Text(stringResource(tab.labelRes), fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.TopCenter) {
            NavHost(navController = navController, startDestination = Route.Dashboard.path, modifier = Modifier.fillMaxSize()) {
                composable(Route.Dashboard.path) {
                    DashboardScreen(
                        onNavigateToDuplicates = { navController.navigate(Route.Duplicates.path) },
                        onNavigateToJunk = { navController.navigate(Route.Junk.path) },
                        onNavigateToVault = { navController.navigate(Route.Vault.path) },
                        onNavigateToLargeFiles = { navController.navigate(Route.LargeFiles.path) },
                    )
                }
                composable(Route.Junk.path) { JunkScreen(adManager = adManager, isPremium = isPremium) }
                composable(Route.Tools.path) {
                    ToolsScreen(isPremium = isPremium, onNavigateToPremium = onNavigateToPremium, onNavigate = { id ->
                        when (id) {
                            "duplicates" -> navController.navigate(Route.Duplicates.path)
                            "large_files" -> navController.navigate(Route.LargeFiles.path)
                            "shredder" -> navController.navigate(Route.Shredder.path)
                            "vault" -> navController.navigate(Route.Vault.path)
                            "privacy" -> navController.navigate(Route.Privacy.path)
                            "wifi" -> navController.navigate(Route.Wifi.path)
                            "ram" -> navController.navigate(Route.RamBooster.path)
                            "battery" -> navController.navigate(Route.Battery.path)
                            "rules" -> navController.navigate(Route.Rules.path)
                            "scheduler" -> navController.navigate(Route.Scheduler.path)
                            "history" -> navController.navigate(Route.History.path)
                        }
                    })
                }
                composable(Route.Settings.path) {
                    SettingsScreen(
                        onNavigateToTheme = { navController.navigate(Route.Theme.path) },
                        onNavigateToScheduler = { navController.navigate(Route.Scheduler.path) },
                        onNavigateToHistory = { navController.navigate(Route.History.path) },
                        onNavigateToVault = { navController.navigate(Route.Vault.path) },
                        onNavigateToPremium = onNavigateToPremium,
                        onNavigateToLanguage = { navController.navigate(Route.Language.path) },
                        onNavigateToRules = { navController.navigate(Route.Rules.path) },
                    )
                }
                composable(Route.Duplicates.path) { DuplicatesScreen(onBack = { navController.popBackStack() }, adManager = adManager, isPremium = isPremium) }
                composable(Route.LargeFiles.path) { LargeFilesScreen(onBack = { navController.popBackStack() }, adManager = adManager, isPremium = isPremium) }
                composable(Route.Vault.path) { VaultScreen(onBack = { navController.popBackStack() }) }
                composable(Route.Shredder.path) { ShredderScreen(onBack = { navController.popBackStack() }) }
                composable(Route.Privacy.path) { PrivacyScreen(onBack = { navController.popBackStack() }) }
                composable(Route.Wifi.path) { WifiSecurityScreen(onBack = { navController.popBackStack() }) }
                composable(Route.Scheduler.path) { SchedulerScreen(onBack = { navController.popBackStack() }) }
                composable(Route.History.path) { HistoryScreen(onBack = { navController.popBackStack() }) }
                composable(Route.Rules.path) { RulesScreen(onBack = { navController.popBackStack() }) }
                composable(Route.Theme.path) { ThemeScreen(onBack = { navController.popBackStack() }) }
                composable(Route.RamBooster.path) { RamBoosterScreen(onBack = { navController.popBackStack() }) }
                composable(Route.Battery.path) { BatteryScreen(onBack = { navController.popBackStack() }) }
                composable(Route.Language.path) { LanguageScreen(onBack = { navController.popBackStack() }) }
            }
        }
    }
}
