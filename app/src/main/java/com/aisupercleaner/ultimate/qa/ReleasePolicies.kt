package com.aisupercleaner.ultimate.qa

object PremiumEntitlementPolicy {
    fun isPremium(purchasedProductIds: Set<String>, purchaseCompleted: Boolean): Boolean = purchaseCompleted && purchasedProductIds.any { it == "premium_monthly" || it == "premium_lifetime" }
}

object AdFrequencyPolicy {
    const val COOLDOWN_MILLIS: Long = 15 * 60 * 1000L
    fun canShowInterstitial(lastShownAt: Long, now: Long): Boolean = now - lastShownAt >= COOLDOWN_MILLIS
}
