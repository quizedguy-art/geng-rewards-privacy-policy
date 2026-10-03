package com.quizedguy.reelnearn.shared.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.quizedguy.reelnearn.shared.data.UsageStatsHelper
import com.quizedguy.reelnearn.shared.util.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import android.util.Log
import java.time.LocalDate

class UsageNotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val totalMillis = UsageStatsHelper.getReelWatchTime(applicationContext)
        val hours = totalMillis / 3600000.0

        // 1. Goal Completion Notifications (2h = 40 pts, 3h = 60 pts, 4h = 100 pts)
        checkGoalCompletions(hours)

        // 2. Daily Lucky Spin Notification (local check)
        NotificationHelper.notifyDailySpinAvailable(applicationContext)

        // 3. Online sync operations (only if network is connected to save battery)
        if (isNetworkAvailable(applicationContext)) {
            // Check Daily Check-In
            checkDailyCheckIn()

            // Point Credit Check
            checkPointCredits()

            // Sync live usage to Firestore
            syncUsageToFirestore(totalMillis)
        }

        return Result.success()
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            val activeNetwork = cm?.activeNetworkInfo
            activeNetwork != null && activeNetwork.isConnectedOrConnecting
        } catch (e: Exception) {
            false
        }
    }

    private fun checkGoalCompletions(hours: Double) {
        if (hours >= 2.0) {
            NotificationHelper.notifyGoalCompleted(applicationContext, tier = 1, hours = 2, points = 40)
        }
        if (hours >= 3.0) {
            NotificationHelper.notifyGoalCompleted(applicationContext, tier = 2, hours = 3, points = 60)
        }
        if (hours >= 4.0) {
            NotificationHelper.notifyGoalCompleted(applicationContext, tier = 3, hours = 4, points = 100)
        }
    }

    private suspend fun checkDailyCheckIn() {
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        val today = LocalDate.now().toString()

        try {
            val userDoc = db.collection("users").document(userId).get().await()
            if (userDoc.exists()) {
                val lastCheckInDate = userDoc.getString("lastCheckInDate") ?: ""
                if (lastCheckInDate != today) {
                    val streak = (userDoc.getLong("checkInStreak")?.toInt() ?: 0)
                    val nextStreakDay = (streak % 7) + 1
                    val points = getCheckInPointsForDay(nextStreakDay)
                    NotificationHelper.notifyDailyCheckInWaiting(
                        applicationContext,
                        streakDay = nextStreakDay,
                        points = points
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("UsageWorker", "Error checking daily checkin: ${e.message}")
        }
    }

    private fun getCheckInPointsForDay(day: Int): Int {
        return when (day) {
            1 -> 20
            2 -> 30
            3 -> 40
            4 -> 50
            5 -> 70
            6 -> 100
            7 -> 150
            else -> 20
        }
    }

    private suspend fun checkPointCredits() {
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        
        try {
            val usageList = db.collection("daily_usage")
                .whereEqualTo("userId", userId)
                .whereEqualTo("isApproved", true)
                .whereEqualTo("isCollected", false)
                .get().await()
            
            if (!usageList.isEmpty) {
                val prefs = applicationContext.getSharedPreferences("points_prefs", Context.MODE_PRIVATE)
                val notifiedIds = prefs.getStringSet("notified_approved_ids", emptySet()) ?: emptySet()
                val currentIds = usageList.documents.map { it.id }.toSet()
                
                val newApprovedIds = currentIds.subtract(notifiedIds)
                if (newApprovedIds.isNotEmpty()) {
                    val totalPoints = usageList.documents
                        .filter { newApprovedIds.contains(it.id) }
                        .sumOf { it.getLong("pointsPotential") ?: 0L }
                    
                    if (totalPoints > 0) {
                        NotificationHelper.notifyPointsCredited(
                            applicationContext,
                            totalPoints.toInt(),
                            reason = "Watch Milestones"
                        )
                        prefs.edit().putStringSet("notified_approved_ids", notifiedIds + newApprovedIds).apply()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("UsageWorker", "Error checking points: ${e.message}")
        }
    }

    private fun calculatePoints(millis: Long): Int {
        val hours = millis / 3600000.0
        var pts = 0
        if (hours >= 2.0) pts += 40
        if (hours >= 3.0) pts += 60
        if (hours >= 4.0) pts += 100
        return pts
    }

    private suspend fun syncUsageToFirestore(totalMillis: Long) {
        val today = LocalDate.now().toString()
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        try {
            val pointsPotential = calculatePoints(totalMillis)
            val docId = "${userId}_$today"
            val usageRef = db.collection("daily_usage").document(docId)
            val usageSnapshot = usageRef.get().await()

            if (usageSnapshot.exists()) {
                usageRef.update(
                    "totalMillis", totalMillis,
                    "pointsPotential", pointsPotential
                ).await()
            } else {
                val record = hashMapOf(
                    "userId" to userId,
                    "date" to today,
                    "totalMillis" to totalMillis,
                    "pointsPotential" to pointsPotential,
                    "pointsCredited" to 0,
                    "isCollected" to false,
                    "isApproved" to false,
                    "approvedAt" to 0L,
                    "collectedAt" to 0L
                )
                usageRef.set(record).await()
            }
        } catch (e: Exception) {
            Log.e("UsageWorker", "Error syncing usage: ${e.message}")
        }
    }
}
