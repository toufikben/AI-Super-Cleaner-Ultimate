package com.aisupercleaner.ultimate.qa

object PremiumEntitlementPolicy {
    private val premiumProducts = setOf("premium_monthly", "premium_lifetime")

    fun isPremium(purchasedProductIds: Set<String>, purchaseCompleted: Boolean): Boolean =
        purchaseCompleted && purchasedProductIds.any(premiumProducts::contains)

    fun activeProductIds(purchasedProductIds: Set<String>, purchaseCompleted: Boolean): Set<String> =
        if (purchaseCompleted) purchasedProductIds intersect premiumProducts else emptySet()
}

object AdFrequencyPolicy {
    const val COOLDOWN_MILLIS: Long = 15 * 60 * 1000L

    fun canRequestAds(consentAllowsAds: Boolean, premium: Boolean): Boolean = consentAllowsAds && !premium

    fun canShowInterstitial(lastShownAt: Long, now: Long): Boolean =
        now >= lastShownAt && now - lastShownAt >= COOLDOWN_MILLIS
}
