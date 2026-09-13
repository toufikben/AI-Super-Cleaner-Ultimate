package com.aisupercleaner.ultimate.presentation.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aisupercleaner.ultimate.presentation.main.MainViewModel
import com.aisupercleaner.ultimate.ads.AdManager
import com.aisupercleaner.ultimate.billing.BillingManager
import com.aisupercleaner.ultimate.presentation.onboarding.OnboardingScreen
import com.aisupercleaner.ultimate.presentation.permissions.PermissionsScreen
import com.aisupercleaner.ultimate.presentation.premium.PremiumScreen
import com.aisupercleaner.ultimate.presentation.splash.SplashScreen

private const val ANIM = 300

@Composable
fun AppNavHost(
    adManager: AdManager,
    billingManager: BillingManager,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    mainViewModel: MainViewModel = hiltViewModel(),
) {
    val state by mainViewModel.uiState.collectAsStateWithLifecycle()

    val startDestination = remember(state.onboardingCompleted) {
        if (state.onboardingCompleted) Route.Splash.path else Route.Onboarding.path
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { slideInHorizontally(initialOffsetX = { it / 4 }, animationSpec = tween(ANIM)) + fadeIn(animationSpec = tween(ANIM)) },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 6 }, animationSpec = tween(ANIM)) + fadeOut(animationSpec = tween(ANIM)) },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 4 }, animationSpec = tween(ANIM)) + fadeIn(animationSpec = tween(ANIM)) },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it / 6 }, animationSpec = tween(ANIM)) + fadeOut(animationSpec = tween(ANIM)) },
    ) {
        composable(Route.Onboarding.path) {
            OnboardingScreen(
                onFinished = {
                    mainViewModel.onOnboardingCompleted()
                    navController.navigate(Route.Permissions.path) { popUpTo(Route.Onboarding.path) { inclusive = true } }
                }
            )
        }
        composable(Route.Permissions.path) {
            PermissionsScreen(
                onAllGranted = {
                    navController.navigate(Route.Splash.path) { popUpTo(Route.Permissions.path) { inclusive = true } }
                }
            )
        }
        composable(Route.Splash.path) {
            SplashScreen(
                onReady = {
                    navController.navigate(Route.Main.path) { popUpTo(Route.Splash.path) { inclusive = true } }
                }
            )
        }
        composable(Route.Main.path) {
            MainScaffold(
                adManager = adManager,
                billingManager = billingManager,
                onNavigateToPremium = { navController.navigate(Route.Premium.path) },
            )
        }
        composable(Route.Premium.path) {
            PremiumScreen(onBack = { navController.popBackStack() })
        }
    }
}
