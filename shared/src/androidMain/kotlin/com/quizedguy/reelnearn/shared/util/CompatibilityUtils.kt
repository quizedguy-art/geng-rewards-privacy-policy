package com.quizedguy.reelnearn.shared.util

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

object CompatibilityUtils {
    
    /**
     * Check if Google Play Services is available on the device.
     * Required for Fire OS (Amazon) compatibility checks.
     */
    fun isGooglePlayServicesAvailable(context: Context): Boolean {
        val availability = GoogleApiAvailability.getInstance()
        val resultCode = availability.isGooglePlayServicesAvailable(context)
        return resultCode == ConnectionResult.SUCCESS
    }

    /**
     * Returns a user-friendly message if Play Services are missing.
     */
    fun getCompatibilityMessage(context: Context): String? {
        if (!isGooglePlayServicesAvailable(context)) {
            return "Reel n Earn noticed that Google Play Services is missing. Some features like Real-time Sync and Ads may be limited on this device."
        }
        return null
    }
}


