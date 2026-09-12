package com.aisupercleaner.ultimate.billing

import android.app.Activity

import android.content.Context

import com.android.billingclient.api.*

import kotlinx.coroutines.flow.MutableStateFlow

import kotlinx.coroutines.flow.asStateFlow

private const val MONTHLY="premium_monthly"

private const val LIFETIME="premium_lifetime"

data class BillingCatalog(val monthly:ProductDetails?=null,val lifetime:ProductDetails?=null)

class BillingManager(context:Context):BillingClientStateListener,PurchasesUpdatedListener{
    
 private val _isPremium=MutableStateFlow(false);val isPremium=_isPremium.asStateFlow();private val _catalog=MutableStateFlow(BillingCatalog());val catalog=_catalog.asStateFlow();private val _message=MutableStateFlow<String?>(null);val message=_message.asStateFlow()
 
 private val client=BillingClient.newBuilder(context.applicationContext).setListener(this).enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()).build()
 
 init{client.startConnection(this)}
 
 override fun onBillingSetupFinished(r:BillingResult){if(r.responseCode==BillingClient.BillingResponseCode.OK)refresh()}
 
 override fun onBillingServiceDisconnected(){}
 
 private fun refresh(){queryProducts();queryPurchases(BillingClient.ProductType.INAPP);queryPurchases(BillingClient.ProductType.SUBS)}
 
 private fun queryProducts(){val one=QueryProductDetailsParams.newBuilder().setProductList(listOf(QueryProductDetailsParams.Product.newBuilder().setProductId(LIFETIME).setProductType(BillingClient.ProductType.INAPP).build())).build();client.queryProductDetailsAsync(one){r,d->if(r.responseCode==BillingClient.BillingResponseCode.OK)_catalog.value=_catalog.value.copy(lifetime=d.productDetailsList.firstOrNull())};val sub=QueryProductDetailsParams.newBuilder().setProductList(listOf(QueryProductDetailsParams.Product.newBuilder().setProductId(MONTHLY).setProductType(BillingClient.ProductType.SUBS).build())).build();client.queryProductDetailsAsync(sub){r,d->if(r.responseCode==BillingClient.BillingResponseCode.OK)_catalog.value=_catalog.value.copy(monthly=d.productDetailsList.firstOrNull())}}
 
 private fun queryPurchases(t:String){client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(t).build()){r,p->if(r.responseCode==BillingClient.BillingResponseCode.OK)process(p)}}
 
 override fun onPurchasesUpdated(r:BillingResult,p:MutableList<Purchase>?){if(r.responseCode==BillingClient.BillingResponseCode.OK&&p!=null)process(p)}
 
 private fun process(ps:List<Purchase>){ps.filter{it.purchaseState==Purchase.PurchaseState.PURCHASED}.forEach{p->_isPremium.value=true;if(!p.isAcknowledged)client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build()){}}}
 
 fun launchMonthly(a:Activity){val d=_catalog.value.monthly?:return;val o=d.subscriptionOfferDetails?.firstOrNull()?:return;launch(a,BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(d).setOfferToken(o.offerToken).build())}
 
 fun launchLifetime(a:Activity){val d=_catalog.value.lifetime?:return;launch(a,BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(d).build())}
 
 private fun launch(a:Activity,p:BillingFlowParams.ProductDetailsParams){client.launchBillingFlow(a,BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(p)).build())}
 
 fun clearMessage(){_message.value=null}
 
}
















