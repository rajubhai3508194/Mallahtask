package com.example.data

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.*
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback

object AdMobManager {
    private const val TAG = "AdMobManager"

    // --- Ad Unit IDs ---
    const val APP_OPEN_AD_ID = "ca-app-pub-9300882058550239/2907716009"
    const val BANNER_AD_ID = "ca-app-pub-9300882058550239/8115810659"
    const val NATIVE_AD_ID = "ca-app-pub-9300882058550239/9663861899"
    const val REWARDED_VIDEO_AD_ID = "ca-app-pub-9300882058550239/4272347108"
    const val INTERSTITIAL_AD_ID = "ca-app-pub-9300882058550239/8160042680"
    const val REWARDED_INTERSTITIAL_AD_ID = "ca-app-pub-9300882058550239/7070322298"

    private var appOpenAd: AppOpenAd? = null
    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var rewardedInterstitialAd: RewardedInterstitialAd? = null

    private var isInitializing = false
    private var isInitialized = false

    fun initialize(context: Context, onInitComplete: () -> Unit = {}) {
        if (isInitialized || isInitializing) {
            onInitComplete()
            return
        }
        isInitializing = true
        Log.d(TAG, "Initializing Google Mobile Ads SDK...")
        MobileAds.initialize(context) { status ->
            isInitialized = true
            isInitializing = false
            Log.d(TAG, "Google Mobile Ads SDK Initialized successfully.")
            
            // Prefetch ads
            loadAppOpenAd(context)
            loadInterstitial(context)
            loadRewardedAd(context)
            loadRewardedInterstitial(context)
            
            onInitComplete()
        }
    }

    // --- App Open Ad ---
    fun loadAppOpenAd(context: Context) {
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            context,
            APP_OPEN_AD_ID,
            request,
            AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    Log.d(TAG, "App Open Ad loaded successfully.")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "App Open Ad failed to load: ${loadAdError.message}")
                }
            }
        )
    }

    fun showAppOpenAd(activity: Activity, onAdClosed: () -> Unit = {}) {
        val ad = appOpenAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    appOpenAd = null
                    Log.d(TAG, "App Open Ad dismissed.")
                    loadAppOpenAd(activity) // prefetch next
                    onAdClosed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    appOpenAd = null
                    Log.e(TAG, "App Open Ad failed to show: ${adError.message}")
                    onAdClosed()
                }
            }
            ad.show(activity)
        } else {
            Log.d(TAG, "App Open Ad was not ready yet.")
            onAdClosed()
        }
    }

    // --- Interstitial Ad ---
    fun loadInterstitial(context: Context) {
        val request = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_ID,
            request,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    Log.d(TAG, "Interstitial Ad loaded successfully.")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "Interstitial Ad failed to load: ${loadAdError.message}")
                }
            }
        )
    }

    fun showInterstitial(activity: Activity, onAdClosed: () -> Unit) {
        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    Log.d(TAG, "Interstitial Ad dismissed.")
                    loadInterstitial(activity) // prefetch
                    onAdClosed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    interstitialAd = null
                    Log.e(TAG, "Interstitial Ad failed to show: ${adError.message}")
                    onAdClosed()
                }
            }
            ad.show(activity)
        } else {
            Log.d(TAG, "Interstitial Ad not ready. Executing callback directly.")
            onAdClosed()
            loadInterstitial(activity) // retry loading
        }
    }

    // --- Rewarded Video Ad ---
    fun loadRewardedAd(context: Context) {
        val request = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            REWARDED_VIDEO_AD_ID,
            request,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    Log.d(TAG, "Rewarded Video Ad loaded successfully.")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "Rewarded Video Ad failed to load: ${loadAdError.message}")
                }
            }
        )
    }

    fun showRewardedAd(activity: Activity, onEarnedReward: (Double) -> Unit) {
        val ad = rewardedAd
        if (ad != null) {
            var rewardEarned = false
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    Log.d(TAG, "Rewarded Video Ad dismissed.")
                    loadRewardedAd(activity) // prefetch
                    if (rewardEarned) {
                        onEarnedReward(0.35) // Reward amount in PKR
                    }
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    rewardedAd = null
                    Log.e(TAG, "Rewarded Video Ad failed to show: ${adError.message}")
                    // Fallback to guarantee user progress isn't locked
                    onEarnedReward(0.35)
                }
            }
            ad.show(activity) { rewardItem ->
                rewardEarned = true
                Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
            }
        } else {
            Log.d(TAG, "Rewarded Video Ad not ready. Simulating reward fallback.")
            onEarnedReward(0.35)
            loadRewardedAd(activity)
        }
    }

    // --- Rewarded Interstitial Ad ---
    fun loadRewardedInterstitial(context: Context) {
        val request = AdRequest.Builder().build()
        RewardedInterstitialAd.load(
            context,
            REWARDED_INTERSTITIAL_AD_ID,
            request,
            object : RewardedInterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedInterstitialAd) {
                    rewardedInterstitialAd = ad
                    Log.d(TAG, "Rewarded Interstitial Ad loaded successfully.")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "Rewarded Interstitial Ad failed to load: ${loadAdError.message}")
                }
            }
        )
    }

    fun showRewardedInterstitial(activity: Activity, onAdClosed: () -> Unit) {
        val ad = rewardedInterstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedInterstitialAd = null
                    Log.d(TAG, "Rewarded Interstitial Ad dismissed.")
                    loadRewardedInterstitial(activity) // prefetch
                    onAdClosed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    rewardedInterstitialAd = null
                    Log.e(TAG, "Rewarded Interstitial Ad failed to show: ${adError.message}")
                    onAdClosed()
                }
            }
            ad.show(activity) { rewardItem ->
                Log.d(TAG, "User earned reward from rewarded interstitial: ${rewardItem.amount}")
            }
        } else {
            Log.d(TAG, "Rewarded Interstitial Ad not ready. Running callback.")
            onAdClosed()
            loadRewardedInterstitial(activity)
        }
    }
}

/**
 * Jetpack Compose wrapper for Google Mobile Ads Banner.
 */
@Composable
fun AdMobBannerAd(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = AdMobManager.BANNER_AD_ID
                loadAd(AdRequest.Builder().build())
            }
        },
        update = { adView ->
            adView.loadAd(AdRequest.Builder().build())
        }
    )
}

/**
 * Jetpack Compose native ads row placeholder styled beautifully to act as a native advanced ad row.
 */
@Composable
fun AdMobNativeAdRow(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(Color.Black.copy(alpha = 0.05f)),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { context ->
                AdView(context).apply {
                    setAdSize(AdSize.LARGE_BANNER)
                    adUnitId = AdMobManager.BANNER_AD_ID // Using banner as fallback for layout compatibility
                    loadAd(AdRequest.Builder().build())
                }
            }
        )
    }
}
