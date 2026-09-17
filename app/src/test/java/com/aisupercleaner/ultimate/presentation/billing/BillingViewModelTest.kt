package com.aisupercleaner.ultimate.presentation.billing

import com.aisupercleaner.ultimate.billing.BillingCatalog
import com.aisupercleaner.ultimate.billing.BillingManager
import com.aisupercleaner.ultimate.presentation.premium.PremiumScreenStatus
import com.aisupercleaner.ultimate.presentation.premium.PremiumViewModel
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Test

class BillingViewModelTest {

    @Test
    fun `billing view model exposes the injected billing manager`() {
        val billing = mockk<BillingManager>()

        val viewModel = BillingViewModel(billing)

        assertThat(viewModel.billing).isSameInstanceAs(billing)
    }

    @Test
    fun `premium view model refreshes purchases on initialization and explicit restore`() = runTest {
        val billing = fakeBillingManager()

        val viewModel = PremiumViewModel(billing)
        viewModel.uiState.first()
        viewModel.refreshPurchases()

        verify(exactly = 2) { billing.refresh() }
    }

    @Test
    fun `premium view model reflects premium and purchase progress states`() = runTest {
        val isPremium = MutableStateFlow(false)
        val purchaseInProgress = MutableStateFlow(false)
        val billing = fakeBillingManager(isPremium, purchaseInProgress)
        val viewModel = PremiumViewModel(billing)

        viewModel.uiState.first()

        isPremium.value = true
        val premiumState = viewModel.uiState.first { it.isPremium }
        assertThat(premiumState.status).isEqualTo(PremiumScreenStatus.PREMIUM)

        purchaseInProgress.value = true
        val busyState = viewModel.uiState.first { it.isBusy }
        assertThat(busyState.isBusy).isTrue()
        advanceTimeBy(5_001)
    }

    private fun fakeBillingManager(
        isPremium: MutableStateFlow<Boolean> = MutableStateFlow(false),
        purchaseInProgress: MutableStateFlow<Boolean> = MutableStateFlow(false),
    ): BillingManager {
        val billing = mockk<BillingManager>()
        every { billing.isPremium } returns isPremium
        every { billing.catalog } returns MutableStateFlow(BillingCatalog())
        every { billing.message } returns MutableStateFlow(null)
        every { billing.purchaseInProgress } returns purchaseInProgress
        every { billing.refresh() } just runs
        return billing
    }
}
