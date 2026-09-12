package com.aisupercleaner.ultimate

import com.aisupercleaner.ultimate.qa.AdFrequencyPolicy
import com.aisupercleaner.ultimate.qa.PremiumEntitlementPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleasePoliciesTest {
    @Test fun lifetimePurchaseActivatesPremium() { assertTrue(PremiumEntitlementPolicy.isPremium(setOf("premium_lifetime"), true)) }
    @Test fun monthlyPurchaseActivatesPremium() { assertTrue(PremiumEntitlementPolicy.isPremium(setOf("premium_monthly"), true)) }
    @Test fun pendingPurchaseDoesNotActivatePremium() { assertFalse(PremiumEntitlementPolicy.isPremium(setOf("premium_monthly"), false)) }
    @Test fun unknownProductDoesNotActivatePremium() { assertFalse(PremiumEntitlementPolicy.isPremium(setOf("other_product"), true)) }
    @Test fun activeProductIdsExcludeUnknownProducts() { assertEquals(setOf("premium_lifetime"), PremiumEntitlementPolicy.activeProductIds(setOf("premium_lifetime", "other"), true)) }
    @Test fun consentIsRequiredBeforeAds() {
        assertFalse(AdFrequencyPolicy.canRequestAds(false, false))
        assertTrue(AdFrequencyPolicy.canRequestAds(true, false))
    }
    @Test fun premiumDisablesAdsEvenAfterConsent() { assertFalse(AdFrequencyPolicy.canRequestAds(true, true)) }
    @Test fun interstitialIsBlockedDuringCooldown() { assertFalse(AdFrequencyPolicy.canShowInterstitial(1_000L, 1_000L + AdFrequencyPolicy.COOLDOWN_MILLIS - 1)) }
    @Test fun interstitialIsAllowedAfterCooldown() { assertTrue(AdFrequencyPolicy.canShowInterstitial(1_000L, 1_000L + AdFrequencyPolicy.COOLDOWN_MILLIS)) }
    @Test fun clockMovingBackwardsDoesNotBypassCooldown() { assertFalse(AdFrequencyPolicy.canShowInterstitial(2_000L, 1_000L)) }
}
