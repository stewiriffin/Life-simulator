// app/src/main/java/com/maisha/game/ads/AdManager.kt
package com.maisha.game.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdManager @Inject constructor() {

    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var isLoadingInterstitial = false
    private var isLoadingRewarded = false

    fun preloadInterstitial(context: Context) {
        if (interstitialAd != null || isLoadingInterstitial) return
        isLoadingInterstitial = true
        InterstitialAd.load(
            context,
            AdUnitConfig.INTERSTITIAL,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoadingInterstitial = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isLoadingInterstitial = false
                }
            }
        )
    }

    fun showInterstitialIfReady(activity: Activity, onDismissed: () -> Unit) {
        val ad = interstitialAd
        if (ad == null) {
            preloadInterstitial(activity.applicationContext)
            onDismissed()
            return
        }
        interstitialAd = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                preloadInterstitial(activity.applicationContext)
                onDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                preloadInterstitial(activity.applicationContext)
                onDismissed()
            }
        }
        ad.show(activity)
    }

    fun preloadRewarded(context: Context) {
        if (rewardedAd != null || isLoadingRewarded) return
        isLoadingRewarded = true
        RewardedAd.load(
            context,
            AdUnitConfig.REWARDED,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoadingRewarded = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isLoadingRewarded = false
                }
            }
        )
    }

    fun showRewardedIfReady(
        activity: Activity,
        onReward: () -> Unit,
        onDismissedNoReward: () -> Unit
    ) {
        val ad = rewardedAd
        if (ad == null) {
            preloadRewarded(activity.applicationContext)
            onDismissedNoReward()
            return
        }
        rewardedAd = null
        var rewardEarned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                preloadRewarded(activity.applicationContext)
                if (!rewardEarned) {
                    onDismissedNoReward()
                }
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                preloadRewarded(activity.applicationContext)
                onDismissedNoReward()
            }
        }
        ad.show(activity) { _ ->
            rewardEarned = true
            onReward()
        }
    }
}
