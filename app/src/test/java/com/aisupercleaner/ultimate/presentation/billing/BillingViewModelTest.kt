package com.aisupercleaner.ultimate.presentation.billing

import com.aisupercleaner.ultimate.billing.BillingManager
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import org.junit.Test

class BillingViewModelTest {

    @Test
    fun `billing view model exposes the injected billing manager`() {
        val billing = mockk<BillingManager>()

        val viewModel = BillingViewModel(billing)

        assertThat(viewModel.billing).isSameInstanceAs(billing)
    }
}
