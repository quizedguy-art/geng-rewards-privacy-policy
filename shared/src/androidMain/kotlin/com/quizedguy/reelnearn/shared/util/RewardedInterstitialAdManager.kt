package com.quizedguy.reelnearn.shared.util

import android.app.Activity
import android.content.Context
import android.util.Log
import com.quizedguy.reelnearn.shared.BuildConfig
import android.widget.Toast
import android.os.Handler
import android.os.Looper
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Manages the loading and showing of rewarded interstitial ads globally.
 */
object RewardedInterstitialAdManager {
    private const val TAG = "RewardedInterstitial"
    private fun getAdUnitId(context: Context): String {
        val resId = context.resources.getIdentifier("ad_unit_rewarded_interstitial", "string", context.packageName)
        return if (resId != 0) context.getString(resId) else "ca-app-pub-1444583349758700/1089904088"
    }
    
    private var rewardedInterstitialAd: RewardedInterstitialAd? = null
    private var isLoading = false
    private var loadTime: Long = 0
    private var lastFailedLoadTime: Long = 0
    private var lastShowTime: Long = 0
    private var retryAttempt = 0
    
    private val _isAdLoaded = MutableStateFlow(false)
    val isAdLoaded: StateFlow<Boolean> = _isAdLoaded
    
    private var onAdLoadedCallback: ((Boolean) -> Unit)? = null
    
    /**
     * Checks if a loaded ad is still valid.
     */
    fun isAdAvailable(): Boolean {
        val now = System.currentTimeMillis()
        val fourHoursInMillis = 4 * 60 * 60 * 1000
        val isExpired = now - loadTime > fourHoursInMillis
        
        return rewardedInterstitialAd != null && !isExpired
    }

    /**
     * Loads a rewarded interstitial ad.
     */
    fun loadAd(context: Context, force: Boolean = false) {
        if (!AppOpenAdManager.isAdSdkInitialized) {
            Log.d(TAG, "Ad SDK is not initialized yet. Skipping load.")
            onAdLoadedCallback?.invoke(false)
            onAdLoadedCallback = null
            return
        }
        if (AgeSignalsHelper.isMinor.value) {
            Log.d(TAG, "Blocking ad load: User is a minor according to Play Age Signals API.")
            rewardedInterstitialAd = null
            _isAdLoaded.value = false
            onAdLoadedCallback?.invoke(false)
            onAdLoadedCallback = null
            return
        }
        if (isAdAvailable()) {
            onAdLoadedCallback?.invoke(true)
            onAdLoadedCallback = null
            return
        }
        if (isLoading) {
            return
        }

        val now = System.currentTimeMillis()
        if (!force && (now - lastFailedLoadTime < 15000)) {
            Log.d(TAG, "Throttling rewarded interstitial ad load attempt.")
            onAdLoadedCallback?.invoke(false)
            onAdLoadedCallback = null
            return
        }
        
        isLoading = true
        Log.d(TAG, "Loading rewarded interstitial ad...")
        
        val adRequest = AdRequest.Builder().build()
        val currentAdUnitId = getAdUnitId(context)
        
        RewardedInterstitialAd.load(context, currentAdUnitId, adRequest, object : RewardedInterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e(TAG, "Ad failed to load: ${adError.message} (Code: ${adError.code})")

                if (adError.code == AdRequest.ERROR_CODE_NETWORK_ERROR) {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(
                            context.applicationContext,
                            "âš ï¸ Could not connect to Ad server. If you use Private DNS (AdGuard/NextDNS) or an AdBlocker, please set Private DNS to 'Off' in Android Network Settings.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                rewardedInterstitialAd = null
                isLoading = false
                _isAdLoaded.value = false
                lastFailedLoadTime = System.currentTimeMillis()
                
                onAdLoadedCallback?.invoke(false)
                onAdLoadedCallback = null

                if (retryAttempt < 3) {
                    val backoffDelay = (15000L * (1 shl retryAttempt)).coerceAtMost(120000L)
                    retryAttempt++
                    Handler(Looper.getMainLooper()).postDelayed({
                        Log.d(TAG, "Retrying to load rewarded interstitial ad ($backoffDelay ms, Attempt $retryAttempt)...")
                        loadAd(context, force = true)
                    }, backoffDelay)
                }
            }
 
            override fun onAdLoaded(ad: RewardedInterstitialAd) {
                Log.d(TAG, "Ad was loaded successfully.")
                rewardedInterstitialAd = ad
                isLoading = false
                loadTime = System.currentTimeMillis()
                lastFailedLoadTime = 0
                retryAttempt = 0
                _isAdLoaded.value = true
                
                rewardedInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Log.d(TAG, "Ad dismissed.")
                        rewardedInterstitialAd = null
                        _isAdLoaded.value = false
                        loadAd(context) // Preload next
                    }
 
                    override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                        Log.e(TAG, "Ad failed to show: ${adError.message}")
                        rewardedInterstitialAd = null
                        _isAdLoaded.value = false
                        loadAd(context) // Preload next
                    }
                }

                onAdLoadedCallback?.invoke(true)
                onAdLoadedCallback = null
            }
        })
    }

    /**
     * Shows the ad if it's available and cooldown has passed.
     */
    fun showAd(activity: Activity, force: Boolean = false, onRewardEarned: (() -> Unit)? = null) {
        if (AgeSignalsHelper.isMinor.value) {
            Log.w(TAG, "Blocking ad show: User is a minor.")
            return
        }
        val now = System.currentTimeMillis()
        // Minimum 60 seconds between ads to avoid spamming and AdMob policy violations
        if (!force && now - lastShowTime < 60000) {
            Log.d(TAG, "Cooldown active. Skipping ad show.")
            return
        }

        if (isAdAvailable()) {
            lastShowTime = now
            rewardedInterstitialAd?.show(activity) { rewardItem ->
                Log.d(TAG, "User completed interstitial ad: ${rewardItem.amount} ${rewardItem.type}")
                onRewardEarned?.invoke()
            }
        } else {
            Log.d(TAG, "The ad wasn't ready.")
            loadAd(activity)
        }
    }

    /**
     * Loads a rewarded interstitial ad on demand and calls the callback when done.
     */
    fun loadAdOnDemand(context: Context, callback: (Boolean) -> Unit) {
        if (isAdAvailable()) {
            callback(true)
            return
        }
        onAdLoadedCallback = callback
        loadAd(context)
    }
}


