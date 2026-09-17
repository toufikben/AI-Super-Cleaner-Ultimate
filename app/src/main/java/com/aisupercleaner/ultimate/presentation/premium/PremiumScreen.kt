package com.aisupercleaner.ultimate.presentation.premium

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.aisupercleaner.ultimate.R
import com.aisupercleaner.ultimate.billing.BillingManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class PremiumScreenStatus { CHECKING, FREE, PREMIUM, ERROR }

data class PremiumUiState(
    val status: PremiumScreenStatus = PremiumScreenStatus.CHECKING,
    val isPremium: Boolean = false,
    val monthlyPrice: String? = null,
    val lifetimePrice: String? = null,
    val monthlyAvailable: Boolean = false,
    val lifetimeAvailable: Boolean = false,
    val isBusy: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val billing: BillingManager,
) : ViewModel() {
    val uiState: StateFlow<PremiumUiState> = combine(
        billing.isPremium,
        billing.catalog,
        billing.message,
        billing.purchaseInProgress,
    ) { isPremium, catalog, message, purchaseInProgress ->
        val monthlyPrice = catalog.monthly?.subscriptionOfferDetails
            ?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
        val lifetimePrice = catalog.lifetime?.oneTimePurchaseOfferDetails?.formattedPrice
        PremiumUiState(
            status = when {
                isPremium -> PremiumScreenStatus.PREMIUM
                message != null && catalog.monthly == null && catalog.lifetime == null -> PremiumScreenStatus.ERROR
                else -> PremiumScreenStatus.FREE
            },
            isPremium = isPremium,
            monthlyPrice = monthlyPrice,
            lifetimePrice = lifetimePrice,
            monthlyAvailable = catalog.monthly != null,
            lifetimeAvailable = catalog.lifetime != null,
            isBusy = purchaseInProgress,
            message = message,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PremiumUiState())

    init { refreshPurchases() }

    fun refreshPurchases() {
        billing.refresh()
    }

    fun purchaseMonthly(activity: Activity) {
        if (!uiState.value.isPremium && !uiState.value.isBusy && uiState.value.monthlyAvailable) {
            billing.launchMonthly(activity)
        }
    }

    fun purchaseLifetime(activity: Activity) {
        if (!uiState.value.isPremium && !uiState.value.isBusy && uiState.value.lifetimeAvailable) {
            billing.launchLifetime(activity)
        }
    }

    fun clearMessage() = billing.clearMessage()

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
    onBack: () -> Unit = {},
    viewModel: PremiumViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalContext.current as? Activity
    val monthlyFallback = stringResource(R.string.premium_plan_monthly_price)
    val lifetimeFallback = stringResource(R.string.premium_plan_lifetime_price)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.premium_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.Close, contentDescription = null) } },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                Modifier.size(120.dp).clip(CircleShape).background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary))),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.WorkspacePremium, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(64.dp)) }

            Text(stringResource(R.string.premium_headline), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(stringResource(R.string.premium_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            PremiumFeature(Icons.Rounded.Block, stringResource(R.string.premium_feat_1))
            PremiumFeature(Icons.Rounded.Speed, stringResource(R.string.premium_feat_2))
            PremiumFeature(Icons.Rounded.Lock, stringResource(R.string.premium_feat_3))
            PremiumFeature(Icons.Rounded.AutoAwesome, stringResource(R.string.premium_feat_4))
            PremiumFeature(Icons.Rounded.CloudOff, stringResource(R.string.premium_feat_5))

            when (state.status) {
                PremiumScreenStatus.CHECKING -> CircularProgressIndicator()
                PremiumScreenStatus.PREMIUM -> PremiumActiveCard()
                PremiumScreenStatus.FREE, PremiumScreenStatus.ERROR -> {
                    PlanCard(
                        title = stringResource(R.string.premium_plan_monthly),
                        price = state.monthlyPrice ?: monthlyFallback,
                        subtitle = stringResource(R.string.premium_plan_monthly_sub),
                        highlighted = false,
                        enabled = state.monthlyAvailable && !state.isBusy && activity != null,
                        onClick = { activity?.let(viewModel::purchaseMonthly) },
                    )
                    PlanCard(
                        title = stringResource(R.string.premium_plan_lifetime),
                        price = state.lifetimePrice ?: lifetimeFallback,
                        subtitle = stringResource(R.string.premium_plan_lifetime_sub),
                        highlighted = true,
                        enabled = state.lifetimeAvailable && !state.isBusy && activity != null,
                        onClick = { activity?.let(viewModel::purchaseLifetime) },
                    )
                    TextButton(onClick = viewModel::refreshPurchases, enabled = !state.isBusy) {
                        Text(stringResource(R.string.premium_restore))
                    }
                    state.message?.let { message ->
                        Text(message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                        TextButton(onClick = viewModel::clearMessage) { Text(stringResource(R.string.cancel)) }
                    }
                }
            }
            Text(stringResource(R.string.premium_legal), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun PremiumActiveCard() {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.premium_active_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.premium_active_sub), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun PremiumFeature(icon: ImageVector, text: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun PlanCard(
    title: String,
    price: String,
    subtitle: String,
    highlighted: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        enabled = enabled,
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        ),
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (highlighted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = if (highlighted) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(price, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = if (highlighted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
        }
    }
}
