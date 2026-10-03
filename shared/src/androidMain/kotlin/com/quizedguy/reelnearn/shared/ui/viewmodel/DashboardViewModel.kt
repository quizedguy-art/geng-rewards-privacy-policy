package com.quizedguy.reelnearn.shared.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quizedguy.reelnearn.shared.data.UsageStatsHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

data class NoticeData(
    val title: String = "",
    val message: String = "",
    val isActive: Boolean = false,
    val isMandatory: Boolean = false,
    val targetVersionCode: Long = 0,
    val downloadUrl: String = "",
    val updatedAt: String = ""
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FirebaseFirestore.getInstance()
    private var noticeListener: ListenerRegistration? = null

    private val _updateNotice = MutableStateFlow<NoticeData?>(null)
    val updateNotice = _updateNotice.asStateFlow()

    private val _screenTimeMillis = MutableStateFlow(0L)
    val screenTimeMillis = _screenTimeMillis.asStateFlow()

    private val _appBreakdown = MutableStateFlow(UsageStatsHelper.AppWatchBreakdown())
    val appBreakdown = _appBreakdown.asStateFlow()

    private val _hasPermission = MutableStateFlow(UsageStatsHelper.hasUsageStatsPermission(application))
    val hasPermission = _hasPermission.asStateFlow()

    /** Which of the tracked apps (Instagram/YouTube/TikTok) are installed on this device */
    private val _installedTrackedApps = MutableStateFlow<List<String>>(emptyList())
    val installedTrackedApps = _installedTrackedApps.asStateFlow()

    private var timerJob: Job? = null

    init {
        listenToUpdateNotice()
        _installedTrackedApps.value = UsageStatsHelper.getInstalledTrackedApps(application)
    }

    private fun listenToUpdateNotice() {
        noticeListener?.remove()
        noticeListener = db.collection("app_config").document("update_notice")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    _updateNotice.value = null
                    return@addSnapshotListener
                }
                val isActive = snapshot.getBoolean("isActive") ?: false
                if (isActive) {
                    val title = snapshot.getString("title") ?: "Notice"
                    val message = snapshot.getString("message") ?: ""
                    val downloadUrl = snapshot.getString("downloadUrl") ?: ""
                    val isMandatory = snapshot.getBoolean("isMandatory") ?: false
                    val targetVersionCode = snapshot.getLong("targetVersionCode") ?: 0L
                    val updatedAt = snapshot.get("updatedAt")?.toString() ?: ""
                    _updateNotice.value = NoticeData(
                        title = title,
                        message = message,
                        isActive = isActive,
                        isMandatory = isMandatory,
                        targetVersionCode = targetVersionCode,
                        downloadUrl = downloadUrl,
                        updatedAt = updatedAt
                    )
                } else {
                    _updateNotice.value = null
                }
            }
    }

    fun checkPermission() {
        _hasPermission.value = UsageStatsHelper.hasUsageStatsPermission(getApplication())
        if (_hasPermission.value) {
            startTracking()
        }
    }

    private fun startTracking() {
        timerJob?.cancel()

        // Initial snapshot — Instagram + YouTube + TikTok combined time today & breakdown
        val breakdown = UsageStatsHelper.getReelWatchBreakdown(getApplication())
        _appBreakdown.value = breakdown
        _screenTimeMillis.value = breakdown.totalMillis
        checkGoalsAndNotify(breakdown.totalMillis)

        timerJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(3000)
                val currentBreakdown = UsageStatsHelper.getReelWatchBreakdown(getApplication())
                _appBreakdown.value = currentBreakdown
                _screenTimeMillis.value = currentBreakdown.totalMillis
                checkGoalsAndNotify(currentBreakdown.totalMillis)
            }
        }
    }

    private fun checkGoalsAndNotify(millis: Long) {
        val hours = millis / 3600000.0
        val app = getApplication<Application>()
        if (hours >= 2.0) {
            com.quizedguy.reelnearn.shared.util.NotificationHelper.notifyGoalCompleted(app, 1, 2, 40)
        }
        if (hours >= 3.0) {
            com.quizedguy.reelnearn.shared.util.NotificationHelper.notifyGoalCompleted(app, 2, 3, 60)
        }
        if (hours >= 4.0) {
            com.quizedguy.reelnearn.shared.util.NotificationHelper.notifyGoalCompleted(app, 3, 4, 100)
        }
    }

    fun stopTracking() {
        timerJob?.cancel()
    }

    fun formatTime(millis: Long): String {
        val hours   = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        return String.format("%02dh %02dm %02ds", hours, minutes, seconds)
    }

    fun formatShortTime(millis: Long): String {
        val hours   = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        noticeListener?.remove()
    }
}


