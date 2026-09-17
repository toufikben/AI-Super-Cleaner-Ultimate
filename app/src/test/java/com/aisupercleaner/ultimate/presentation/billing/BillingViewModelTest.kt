package com.aisupercleaner.ultimate.presentation.billing

import app.cash.turbine.test
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
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
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

        viewModel.uiState.test {
            awaitItem()

            isPremium.value = true
            runCurrent()
            awaitItem().let { state ->
                assertThat(state.status).isEqualTo(PremiumScreenStatus.PREMIUM)
                assertThat(state.isPremium).isTrue()
            }

            purchaseInProgress.value = true
            runCurrent()
            awaitItem().let { state ->
                assertThat(state.isBusy).isTrue()
            }
            cancelAndIgnoreRemainingEvents()
        }
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
