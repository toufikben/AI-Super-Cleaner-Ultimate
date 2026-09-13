package com.aisupercleaner.ultimate.privacy

import android.app.Activity
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class ConsentManager @Inject constructor(@ApplicationContext context: Context) {
    private val appContext = context.applicationContext
    private val information = UserMessagingPlatform.getConsentInformation(appContext)
    private val _privacyOptionsRequired = MutableStateFlow(false)
    val privacyOptionsRequired = _privacyOptionsRequired.asStateFlow()
    private val _canRequestAds = MutableStateFlow(false)
    val canRequestAds = _canRequestAds.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun requestConsent(activity: Activity, onComplete: () -> Unit) {
        val parameters = ConsentRequestParameters.Builder().build()
        information.requestConsentInfoUpdate(activity, parameters, {
            updateStatus()
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                if (formError != null) _error.value = formError.message
                updateStatus()
                onComplete()
            }
        }, { requestError ->
            _error.value = requestError.message
            updateStatus()
            onComplete()
        })
    }

    fun showPrivacyOptions(activity: Activity, onComplete: () -> Unit) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            if (formError != null) _error.value = formError.message
            updateStatus()
            onComplete()
        }
    }

    fun clearError() { _error.value = null }
    private fun updateStatus() { _privacyOptionsRequired.value = information.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED; _canRequestAds.value = information.canRequestAds() }
}
