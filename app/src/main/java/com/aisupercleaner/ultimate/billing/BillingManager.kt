package com.aisupercleaner.ultimate.billing

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import dagger.hilt.android.qualifiers.ApplicationContext
import com.aisupercleaner.ultimate.data.preferences.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.aisupercleaner.ultimate.qa.PremiumEntitlementPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val MONTHLY = "premium_monthly"
private const val LIFETIME = "premium_lifetime"

data class BillingCatalog(val monthly: ProductDetails? = null, val lifetime: ProductDetails? = null)

@Singleton
class BillingManager @Inject constructor(@ApplicationContext context: Context, private val preferences: AppPreferences) : BillingClientStateListener, PurchasesUpdatedListener {
    private val _isPremium = MutableStateFlow(false)
    val isPremium = _isPremium.asStateFlow()
    private val _catalog = MutableStateFlow(BillingCatalog())
    val catalog = _catalog.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()
    private val _purchaseInProgress = MutableStateFlow(false)
    val purchaseInProgress = _purchaseInProgress.asStateFlow()
    private val purchasesByType = mutableMapOf<String, List<Purchase>>()
    private val handler = Handler(Looper.getMainLooper())
    private var reconnectScheduled = false
    private val client = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    init { connect() }

    private fun connect() {
        if (!client.isReady) client.startConnection(this)
    }

    override fun onBillingSetupFinished(result: BillingResult) {
        reconnectScheduled = false
        if (result.responseCode == BillingClient.BillingResponseCode.OK) refresh()
        else _message.value = "Google Play Billing is unavailable: ${result.debugMessage}"
    }

    override fun onBillingServiceDisconnected() {
        if (reconnectScheduled) return
        reconnectScheduled = true
        handler.postDelayed({ reconnectScheduled = false; connect() }, RECONNECT_DELAY_MILLIS)
    }

    fun refresh() {
        if (!client.isReady) { connect(); return }
        synchronized(purchasesByType) { purchasesByType.clear() }
        _isPremium.value = false
        queryProducts()
        queryPurchases(BillingClient.ProductType.INAPP)
        queryPurchases(BillingClient.ProductType.SUBS)
    }

    private fun queryProducts() {
        val oneTime = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(QueryProductDetailsParams.Product.newBuilder().setProductId(LIFETIME).setProductType(BillingClient.ProductType.INAPP).build()))
            .build()
        client.queryProductDetailsAsync(oneTime) { result, details ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) _catalog.value = _catalog.value.copy(lifetime = details.productDetailsList.firstOrNull())
            else if (result.responseCode != BillingClient.BillingResponseCode.SERVICE_DISCONNECTED) _message.value = "Lifetime product is unavailable: ${result.debugMessage}"
        }
        val subscription = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(QueryProductDetailsParams.Product.newBuilder().setProductId(MONTHLY).setProductType(BillingClient.ProductType.SUBS).build()))
            .build()
        client.queryProductDetailsAsync(subscription) { result, details ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) _catalog.value = _catalog.value.copy(monthly = details.productDetailsList.firstOrNull())
            else if (result.responseCode != BillingClient.BillingResponseCode.SERVICE_DISCONNECTED) _message.value = "Monthly product is unavailable: ${result.debugMessage}"
        }
    }

    private fun queryPurchases(type: String) {
        client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(type).build()) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                synchronized(purchasesByType) { purchasesByType[type] = purchases }
                process(synchronized(purchasesByType) { purchasesByType.values.flatten() })
            }
            else if (result.responseCode == BillingClient.BillingResponseCode.SERVICE_DISCONNECTED) connect()
            else _message.value = "Could not restore purchases: ${result.debugMessage}"
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        _purchaseInProgress.value = false
        when {
            result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null -> process(purchases)
            result.responseCode == BillingClient.BillingResponseCode.USER_CANCELED -> _message.value = "Purchase canceled."
            result.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> refresh()
            result.responseCode == BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> connect()
            result.responseCode != BillingClient.BillingResponseCode.OK -> _message.value = "Purchase failed: ${result.debugMessage}"
        }
    }

    private fun process(purchases: List<Purchase>) {
        val valid = purchases.filter { purchase -> purchase.products.any { it == MONTHLY || it == LIFETIME } }
        _isPremium.value = PremiumEntitlementPolicy.isPremium(
            purchasedProductIds = valid.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }.flatMap { it.products }.toSet(),
            purchaseCompleted = valid.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        )
        CoroutineScope(Dispatchers.IO).launch { preferences.setPremium(_isPremium.value) }
        valid.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged }.forEach { acknowledge(it) }
        if (valid.any { it.purchaseState == Purchase.PurchaseState.PENDING }) _message.value = "Purchase is pending confirmation by Google Play."
    }

    private fun acknowledge(purchase: Purchase) {
        client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()) { result ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK && result.responseCode != BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
                _message.value = "Purchase received but could not be confirmed: ${result.debugMessage}"
            }
        }
    }

    fun launchMonthly(activity: Activity) {
        val details = _catalog.value.monthly ?: return
        val offer = details.subscriptionOfferDetails?.firstOrNull() ?: return
        launch(activity, BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).setOfferToken(offer.offerToken).build())
    }

    fun launchLifetime(activity: Activity) {
        val details = _catalog.value.lifetime ?: return
        launch(activity, BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).build())
    }

    private fun launch(activity: Activity, product: BillingFlowParams.ProductDetailsParams) {
        if (!client.isReady) { _message.value = "Google Play Billing is connecting. Please try again."; connect(); return }
        _purchaseInProgress.value = true
        val result = client.launchBillingFlow(activity, BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(product)).build())
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _purchaseInProgress.value = false
            _message.value = "Could not open Google Play checkout: ${result.debugMessage}"
        }
    }

    fun clearMessage() { _message.value = null }

    companion object { private const val RECONNECT_DELAY_MILLIS = 2_000L }
}
