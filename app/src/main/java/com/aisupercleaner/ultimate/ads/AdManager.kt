package com.aisupercleaner.ultimate.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.aisupercleaner.ultimate.qa.AdFrequencyPolicy

class AdManager(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences("ad_frequency", Context.MODE_PRIVATE)
    private var rewarded: RewardedAd? = null
    private var interstitial: InterstitialAd? = null

    fun initialize() {
        MobileAds.initialize(appContext)
        preloadRewarded()
        loadInterstitialIfAllowed()
    }

    fun preloadRewarded() {
        RewardedAd.load(appContext, REWARDED_PRODUCTION_UNIT, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) { rewarded = ad }
            override fun onAdFailedToLoad(error: LoadAdError) { rewarded = null }
        })
    }

    fun showRewardedAd(activity: Activity, isPremium: Boolean, onReward: () -> Unit, onUnavailable: () -> Unit) {
        if (isPremium) { onUnavailable(); return }
        val ad = rewarded
        if (ad == null) { onUnavailable(); preloadRewarded(); return }
        rewarded = null
        ad.show(activity, OnUserEarnedRewardListener { _ ->
            preferences.edit().putLong(KEY_LAST_REWARD, System.currentTimeMillis()).apply()
            onReward()
        })
        preloadRewarded()
    }

    fun canShowInterstitialAfterCleanup(now: Long = System.currentTimeMillis()): Boolean {
        val last = preferences.getLong(KEY_LAST_INTERSTITIAL, 0L)
        return AdFrequencyPolicy.canShowInterstitial(last, now)
    }

    fun markInterstitialShown() { preferences.edit().putLong(KEY_LAST_INTERSTITIAL, System.currentTimeMillis()).apply() }

    fun loadInterstitialIfAllowed() {
        if (!canShowInterstitialAfterCleanup()) return
        InterstitialAd.load(appContext, INTERSTITIAL_PRODUCTION_UNIT, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) { interstitial = ad }
            override fun onAdFailedToLoad(error: LoadAdError) { interstitial = null }
        })
    }

    fun showInterstitialAfterCleanup(activity: Activity, isPremium: Boolean, onUnavailable: () -> Unit) {
        if (isPremium) { onUnavailable(); return }
        val ad = interstitial
        if (ad == null || !canShowInterstitialAfterCleanup()) { onUnavailable(); return }
        interstitial = null
        ad.show(activity)
        markInterstitialShown()
        loadInterstitialIfAllowed()
    }

    companion object {
        private const val REWARDED_PRODUCTION_UNIT = "ca-app-pub-2934454612171100/7133782308"
        private const val INTERSTITIAL_PRODUCTION_UNIT = "ca-app-pub-2934454612171100/8721838321"
        private const val KEY_LAST_REWARD = "last_reward_at"
        private const val KEY_LAST_INTERSTITIAL = "last_interstitial_at"
    }
}
