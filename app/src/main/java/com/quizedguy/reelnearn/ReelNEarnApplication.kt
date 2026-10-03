package com.quizedguy.reelnearn

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.quizedguy.reelnearn.shared.util.AppOpenAdManager

import com.quizedguy.reelnearn.shared.util.RewardedAdManager
import com.quizedguy.reelnearn.shared.util.RewardedInterstitialAdManager

class ReelNEarnApplication : Application() {

    companion object {
        var isAdSdkInitialized = false
            internal set
    }

    lateinit var appOpenAdManager: AppOpenAdManager
        private set

    override fun onCreate() {
        super.onCreate()
        appOpenAdManager = AppOpenAdManager(this)
        
        // Initialize notification channels and schedule background workers
        com.quizedguy.reelnearn.shared.util.NotificationHelper.createNotificationChannels(this)
        com.quizedguy.reelnearn.shared.worker.WorkScheduler.schedulePeriodicWorkers(this)
    }

    fun initializeMobileAds() {
        if (isAdSdkInitialized) return
        MobileAds.initialize(this) {
            isAdSdkInitialized = true
            AppOpenAdManager.isAdSdkInitialized = true
            
            // Preload ads once SDK is initialized
            appOpenAdManager.fetchAd()
            RewardedAdManager.loadAd(this)
            RewardedInterstitialAdManager.loadAd(this)
        }
    }
}

