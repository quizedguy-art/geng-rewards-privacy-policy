package com.quizedguy.reelnearn.shared.util

import android.app.Activity
import android.util.Log
import android.widget.Toast
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

class InAppUpdateHelper(private val activity: Activity) {

    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(activity)
    private var installStateUpdatedListener: InstallStateUpdatedListener? = null

    companion object {
        const val REQ_CODE_APP_UPDATE = 8991
        private const val TAG = "InAppUpdateHelper"
    }

    /**
     * Checks Google Play Store for an available update.
     * If the user is already on the latest version, this does nothing.
     * If a newer version is published on Google Play, it prompts the user with the official Play Store bottom sheet.
     */
    fun checkForAppUpdate() {
        try {
            appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                    Log.i(TAG, "Update available on Play Store! Available version code: ${appUpdateInfo.availableVersionCode()}")
                    
                    if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                        registerInstallListener()
                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            AppUpdateType.FLEXIBLE,
                            activity,
                            REQ_CODE_APP_UPDATE
                        )
                    } else if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            AppUpdateType.IMMEDIATE,
                            activity,
                            REQ_CODE_APP_UPDATE
                        )
                    }
                } else {
                    Log.d(TAG, "App is up to date (no newer version on Play Store).")
                }
            }.addOnFailureListener { e ->
                Log.w(TAG, "Could not check for in-app update: ${e.message}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error initiating app update check: ${e.message}")
        }
    }

    private fun registerInstallListener() {
        if (installStateUpdatedListener != null) return
        installStateUpdatedListener = InstallStateUpdatedListener { state ->
            if (state.installStatus() == InstallStatus.DOWNLOADED) {
                notifyUpdateDownloaded()
            }
        }
        installStateUpdatedListener?.let { appUpdateManager.registerListener(it) }
    }

    private fun notifyUpdateDownloaded() {
        Toast.makeText(
            activity,
            "🎉 Update downloaded! Restarting to apply changes...",
            Toast.LENGTH_LONG
        ).show()
        appUpdateManager.completeUpdate()
    }

    /**
     * Call this in Activity onResume to ensure downloaded updates are completed
     * or ongoing immediate updates are resumed.
     */
    fun onResume() {
        try {
            appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                    notifyUpdateDownloaded()
                } else if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    // Resume ongoing immediate update
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        AppUpdateType.IMMEDIATE,
                        activity,
                        REQ_CODE_APP_UPDATE
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error in onResume update check: ${e.message}")
        }
    }

    fun onDestroy() {
        installStateUpdatedListener?.let {
            appUpdateManager.unregisterListener(it)
        }
    }
}
