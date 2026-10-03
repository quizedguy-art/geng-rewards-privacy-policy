package com.quizedguy.reelnearn.shared.ui.components
 
import com.quizedguy.reelnearn.shared.BuildConfig

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.LoadAdError
import android.util.Log

@Composable
fun BannerAdView(
    modifier: Modifier = Modifier,
    useTestAd: Boolean = false
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    
    // Do not load ads for minors
    if (com.quizedguy.reelnearn.shared.util.AgeSignalsHelper.isMinor.value) {
        return
    }

    // Calculate adaptive banner size based on current configuration orientation and screen width
    val adSize = remember(configuration.orientation, configuration.screenWidthDp) {
        val density = context.resources.displayMetrics.density
        val widthPixels = context.resources.displayMetrics.widthPixels
        val adWidth = (widthPixels / density).toInt()
        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)
    }

    val adView = remember(adSize) {
        AdView(context).apply {
            setAdSize(adSize)
            adUnitId = if (useTestAd) {
                context.getString(com.quizedguy.reelnearn.shared.R.string.test_ad_unit_banner)
            } else {
                context.getString(com.quizedguy.reelnearn.shared.R.string.ad_unit_banner)
            }
            
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    Log.d("AdMob", "Banner ad loaded successfully")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e("AdMob", "Banner ad failed to load: ${error.message} (Code: ${error.code})")
                }

                override fun onAdOpened() {
                    Log.d("AdMob", "Banner ad opened")
                }
            }
            
            if (com.quizedguy.reelnearn.shared.util.AppOpenAdManager.isAdSdkInitialized) {
                loadAd(AdRequest.Builder().build())
            } else {
                Log.d("AdMob", "BannerAdView: Ad SDK not initialized yet, will load once initialized.")
            }
        }
    }

    // Attempt reload if SDK wasn't initialized at first composition
    androidx.compose.runtime.LaunchedEffect(com.quizedguy.reelnearn.shared.util.AppOpenAdManager.isAdSdkInitialized) {
        if (com.quizedguy.reelnearn.shared.util.AppOpenAdManager.isAdSdkInitialized) {
            adView.loadAd(AdRequest.Builder().build())
        }
    }

    // Safely destroy the AdView when the Composable leaves the screen/hierarchy
    DisposableEffect(adView) {
        onDispose {
            Log.d("AdMob", "Destroying Banner AdView")
            adView.destroy()
        }
    }

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { adView }
    )
}


