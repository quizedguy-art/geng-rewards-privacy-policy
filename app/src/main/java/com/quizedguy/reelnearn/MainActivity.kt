package com.quizedguy.reelnearn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.quizedguy.reelnearn.shared.ui.MainComposeApp
import com.quizedguy.reelnearn.shared.ui.theme.Geng_healthTheme
import com.quizedguy.reelnearn.shared.util.ConsentManager
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : ComponentActivity() {
    private var consentManager: ConsentManager? = null
    private var isMobileAdsInitializeCalled = AtomicBoolean(false)

    private val navigationTarget = androidx.compose.runtime.mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        navigationTarget.value = intent?.getStringExtra(com.quizedguy.reelnearn.shared.util.NotificationHelper.EXTRA_NAVIGATE_TO)

        consentManager = ConsentManager(this)
        consentManager?.gatherConsent { error ->
            if (error != null) {
                android.util.Log.w("MainActivity", "Consent gathering failed: $error")
            }
            if (consentManager?.canRequestAds == true) {
                initializeMobileAdsSdk()
            }
        }
        if (consentManager?.canRequestAds == true) {
            initializeMobileAdsSdk()
        }

        setContent {
            com.quizedguy.reelnearn.shared.ui.theme.ReelNEarnTheme {
                MainComposeApp(
                    deepLinkRoute = navigationTarget.value,
                    onDeepLinkConsumed = { navigationTarget.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val target = intent.getStringExtra(com.quizedguy.reelnearn.shared.util.NotificationHelper.EXTRA_NAVIGATE_TO)
        if (target != null) {
            navigationTarget.value = target
        }
    }

    private fun initializeMobileAdsSdk() {
        if (isMobileAdsInitializeCalled.getAndSet(true)) return
        (application as? ReelNEarnApplication)?.initializeMobileAds()
    }
}

