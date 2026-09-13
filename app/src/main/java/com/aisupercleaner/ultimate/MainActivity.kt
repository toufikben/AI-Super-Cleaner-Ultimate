package com.aisupercleaner.ultimate

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aisupercleaner.ultimate.ads.AdManager
import com.aisupercleaner.ultimate.billing.BillingManager
import com.aisupercleaner.ultimate.presentation.main.MainViewModel
import com.aisupercleaner.ultimate.presentation.navigation.AppNavHost
import com.aisupercleaner.ultimate.presentation.theme.AISuperCleanerTheme
import com.aisupercleaner.ultimate.privacy.ConsentManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject lateinit var consentManager: ConsentManager
    @Inject lateinit var adManager: AdManager
    @Inject lateinit var billingManager: BillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestAdConsent()
        billingManager.refresh()

        setContent {
            val viewModel: MainViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            AISuperCleanerTheme(themeMode = state.themeMode, accentStyle = state.accentStyle, dynamicColor = state.dynamicColor) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavHost(adManager = adManager, billingManager = billingManager)
                }
            }
        }
    }

    private fun requestAdConsent() {
        consentManager.requestConsent(this) {
            adManager.setCanRequestAds(consentManager.canRequestAds.value)
        }
    }
}
