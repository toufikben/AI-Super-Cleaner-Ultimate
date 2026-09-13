package com.aisupercleaner.ultimate.presentation.billing

import androidx.lifecycle.ViewModel
import com.aisupercleaner.ultimate.billing.BillingManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BillingViewModel @Inject constructor(
    val billing: BillingManager,
) : ViewModel()
