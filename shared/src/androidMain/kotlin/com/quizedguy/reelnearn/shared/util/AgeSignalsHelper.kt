package com.quizedguy.reelnearn.shared.util

import android.content.Context
import android.util.Log
import com.google.android.play.agesignals.*
import com.google.android.play.agesignals.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object AgeSignalsHelper {
    private const val TAG = "AgeSignalsHelper"

    private val _isMinor = MutableStateFlow(false)
    val isMinor: StateFlow<Boolean> = _isMinor

    fun checkAge(context: Context) {
        try {
            val ageSignalsManager = AgeSignalsManagerFactory.create(context.applicationContext) ?: return
            val request = AgeSignalsRequest.builder().build() ?: return
            ageSignalsManager.checkAgeSignals(request)
                .addOnSuccessListener { result ->
                    try {
                        val status = result.userStatus()
                        val upperLimit = result.ageUpper()
                        
                        Log.d(TAG, "Age Signals Result: status=$status, upperLimit=$upperLimit")
                        
                        val detectedMinor = when (status) {
                            AgeSignalsVerificationStatus.SUPERVISED,
                            AgeSignalsVerificationStatus.SUPERVISED_APPROVAL_PENDING,
                            AgeSignalsVerificationStatus.SUPERVISED_APPROVAL_DENIED -> true
                            AgeSignalsVerificationStatus.VERIFIED -> false
                            else -> {
                                upperLimit != null && upperLimit <= 17
                            }
                        }
                        
                        _isMinor.value = detectedMinor
                        Log.d(TAG, "Is user minor? $detectedMinor")
                    } catch (e: Throwable) {
                        Log.e(TAG, "Error parsing age result: ${e.message}")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Failed to check Age Signals: ${exception.message}", exception)
                }
        } catch (e: Throwable) {
            Log.e(TAG, "Error initializing AgeSignalsManager: ${e.message}", e)
        }
    }
}


