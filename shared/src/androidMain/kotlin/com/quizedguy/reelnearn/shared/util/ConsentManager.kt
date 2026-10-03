package com.quizedguy.reelnearn.shared.util

import android.app.Activity
import android.util.Log
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

class ConsentManager(private val activity: Activity) {

    private val consentInformation: ConsentInformation?
        get() = try {
            UserMessagingPlatform.getConsentInformation(activity)
        } catch (e: Throwable) {
            Log.e("ConsentManager", "Failed to get consent information: ${e.message}")
            null
        }

    fun interface OnConsentGatheringCompleteListener {
        fun consentGatheringComplete(error: String?)
    }

    fun gatherConsent(onConsentGatheringCompleteListener: OnConsentGatheringCompleteListener) {
        try {
            val consentInfo = consentInformation
            if (consentInfo == null) {
                onConsentGatheringCompleteListener.consentGatheringComplete(null)
                return
            }

            val params = ConsentRequestParameters.Builder()
                .setTagForUnderAgeOfConsent(false)
                .build()

            consentInfo.requestConsentInfoUpdate(
                activity,
                params,
                {
                    try {
                        UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                            onConsentGatheringCompleteListener.consentGatheringComplete(formError?.message)
                        }
                    } catch (e: Throwable) {
                        Log.e("ConsentManager", "Form load error: ${e.message}")
                        onConsentGatheringCompleteListener.consentGatheringComplete(e.message)
                    }
                },
                { requestConsentError ->
                    Log.e("ConsentManager", "Consent failed to update: ${requestConsentError.message}")
                    onConsentGatheringCompleteListener.consentGatheringComplete(requestConsentError.message)
                }
            )
        } catch (e: Throwable) {
            Log.e("ConsentManager", "Error in gatherConsent: ${e.message}")
            onConsentGatheringCompleteListener.consentGatheringComplete(e.message)
        }
    }

    val canRequestAds: Boolean
        get() = try {
            consentInformation?.canRequestAds() ?: true
        } catch (e: Throwable) {
            true
        }
}


