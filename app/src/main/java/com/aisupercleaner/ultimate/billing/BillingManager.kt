package com.aisupercleaner.ultimate.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val PREMIUM_MONTHLY = "premium_monthly"
private const val PREMIUM_LIFETIME = "premium_lifetime"

data class BillingCatalog(val monthly: ProductDetails? = null, val lifetime: ProductDetails? = null)

class BillingManager(context: Context) : BillingClientStateListener, PurchasesUpdatedListener {
    private val _isPremium = MutableStateFlow(false)
    val isPremium = _isPremium.asStateFlow()
    private val _catalog = MutableStateFlow(BillingCatalog())
    val catalog = _catalog.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()
    private val client = BillingClient.newBuilder(context.applicationContext).setListener(this).enablePendingPurchases().build()

    init { connect() }

    private fun connect() { if (!client.isReady) client.startConnection(this) else refresh() }

    override fun onBillingSetupFinished(result: BillingResult) { if (result.responseCode == BillingClient.BillingResponseCode.OK) refresh() else _message.value = "Google Play Billing is unavailable right now." }
    override fun onBillingServiceDisconnected() { _message.value = "Google Play connection was interrupted. Premium will retry automatically." }

    private fun refresh() { queryProducts(); queryPurchases(BillingClient.ProductType.INAPP); queryPurchases(BillingClient.ProductType.SUBS) }

    private fun queryProducts() {
        val oneTime = QueryProductDetailsParams.newBuilder().setProductList(listOf(QueryProductDetailsParams.Product.newBuilder().setProductId(PREMIUM_LIFETIME).setProductType(BillingClient.ProductType.INAPP).build())).build()
        client.queryProductDetailsAsync(oneTime) { result, details -> if (result.responseCode == BillingClient.BillingResponseCode.OK) _catalog.value = _catalog.value.copy(lifetime = details.firstOrNull()) }
        val subscription = QueryProductDetailsParams.newBuilder().setProductList(listOf(QueryProductDetailsParams.Product.newBuilder().setProductId(PREMIUM_MONTHLY).setProductType(BillingClient.ProductType.SUBS).build())).build()
        client.queryProductDetailsAsync(subscription) { result, details -> if (result.responseCode == BillingClient.BillingResponseCode.OK) _catalog.value = _catalog.value.copy(monthly = details.firstOrNull()) }
    }

    private fun queryPurchases(type: String) { client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(type).build()) { result, purchases -> if (result.responseCode == BillingClient.BillingResponseCode.OK) processPurchases(purchases) } }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) processPurchases(purchases)
        else if (result.responseCode != BillingClient.BillingResponseCode.USER_CANCELED) _message.value = "Purchase could not be completed. Your Free features remain available."
    }

    private fun processPurchases(purchases: List<Purchase>) { purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }.forEach { purchase -> _isPremium.value = true; if (!purchase.isAcknowledged) client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()) { result -> if (result.responseCode != BillingClient.BillingResponseCode.OK) _message.value = "Premium purchase needs acknowledgement. Please reopen Google Play." } } }

    fun launchMonthly(activity: Activity) { val details = _catalog.value.monthly ?: return setMessage("Monthly Premium is not available yet."); val offer = details.subscriptionOfferDetails?.firstOrNull() ?: return setMessage("No subscription offer is available."); launch(activity, details, BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).setOfferToken(offer.offerToken).build()) }
    fun launchLifetime(activity: Activity) { val details = _catalog.value.lifetime ?: return setMessage("Lifetime Premium is not available yet."); launch(activity, details, BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).build()) }
    private fun launch(activity: Activity, details: ProductDetails, params: BillingFlowParams.ProductDetailsParams) { val result = client.launchBillingFlow(activity, BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(params)).build()); if (result.responseCode != BillingClient.BillingResponseCode.OK) _message.value = "Google Play could not open the purchase screen." }
    private fun setMessage(value: String) { _message.value = value }
    fun clearMessage() { _message.value = null }
}
