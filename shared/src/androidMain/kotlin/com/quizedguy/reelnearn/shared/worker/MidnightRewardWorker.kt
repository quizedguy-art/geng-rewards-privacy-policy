package com.quizedguy.reelnearn.shared.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.quizedguy.reelnearn.shared.R
import com.quizedguy.reelnearn.shared.data.UsageStatsHelper
import java.time.LocalDate
import java.time.ZoneId

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue

import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import android.util.Log

class MidnightRewardWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        // Runs just after midnight — finalise PREVIOUS day
        val yesterday    = LocalDate.now().minusDays(1)
        val yesterdayStr = yesterday.toString()

        val auth   = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: return Result.success()
        val db     = FirebaseFirestore.getInstance()

        try {
            val userDoc        = db.collection("users").document(userId).get().await()
            val lastRewardDate = userDoc.getString("lastRewardDate")

            val reelWatchMillis = UsageStatsHelper.getReelWatchTimeForDate(applicationContext, yesterday)
            val totalMillis = reelWatchMillis
            val pointsPotential = calculatePoints(totalMillis)

            val docId    = "${userId}_$yesterdayStr"
            val usageRef = db.collection("daily_usage").document(docId)
            val usageSnapshot = usageRef.get().await()
            val now = System.currentTimeMillis()
            var creditedDelta = 0
            
            if (usageSnapshot.exists()) {
                val isCollected = usageSnapshot.getBoolean("isCollected") ?: false
                val pointsCollected = usageSnapshot.getLong("pointsCollected")?.toInt() ?: 0
                val pointsCreditedInDoc = usageSnapshot.getLong("pointsCredited")?.toInt()
                val alreadyCreditedOrCollected = pointsCreditedInDoc ?: if (isCollected) (usageSnapshot.getLong("pointsPotential")?.toInt() ?: 0) else pointsCollected

                if (pointsPotential > alreadyCreditedOrCollected) {
                    creditedDelta = pointsPotential - alreadyCreditedOrCollected
                    val userRef = db.collection("users").document(userId)

                    val batch = db.batch()
                    batch.update(userRef, "points", FieldValue.increment(creditedDelta.toLong()))
                    batch.update(
                        usageRef,
                        mapOf(
                            "totalMillis" to totalMillis,
                            "pointsPotential" to pointsPotential,
                            "pointsCredited" to pointsPotential,
                            "pointsCollected" to pointsPotential,
                            "isApproved" to true,
                            "isCollected" to true,
                            "approvedAt" to (usageSnapshot.getLong("approvedAt")?.takeIf { it > 0L } ?: now),
                            "collectedAt" to now
                        )
                    )
                    batch.commit().await()
                } else {
                    usageRef.update("totalMillis", totalMillis).await()
                }
            } else {
                if (pointsPotential > 0) {
                    creditedDelta = pointsPotential
                    val userRef = db.collection("users").document(userId)
                    val batch = db.batch()
                    batch.update(userRef, "points", FieldValue.increment(pointsPotential.toLong()))
                    val record = hashMapOf(
                        "userId" to userId,
                        "date" to yesterdayStr,
                        "totalMillis" to totalMillis,
                        "pointsPotential" to pointsPotential,
                        "pointsCredited" to pointsPotential,
                        "pointsCollected" to pointsPotential,
                        "isCollected" to true,
                        "isApproved" to true,
                        "approvedAt" to now,
                        "collectedAt" to now
                    )
                    batch.set(usageRef, record)
                    batch.commit().await()
                } else {
                    val record = hashMapOf(
                        "userId" to userId,
                        "date" to yesterdayStr,
                        "totalMillis" to totalMillis,
                        "pointsPotential" to 0,
                        "pointsCredited" to 0,
                        "isCollected" to false,
                        "isApproved" to false,
                        "approvedAt" to 0L,
                        "collectedAt" to 0L
                    )
                    usageRef.set(record).await()
                }
            }
            
            // Mark lastRewardDate so we don't double-process the same day
            db.collection("users").document(userId)
                .update("lastRewardDate", yesterdayStr)
                .await()

            if (creditedDelta > 0) {
                Log.d("MidnightReward", "Finalised $yesterdayStr — credited +$creditedDelta pts.")
                sendNotification(creditedDelta, pointsPotential)
            } else {
                Log.d("MidnightReward", "$yesterdayStr already fully credited or 0 pts.")
            }

        } catch (e: Exception) {
            Log.e("MidnightReward", "Error awarding points: ${e.message}")
            return Result.retry() // Retry if transient error
        }

        return Result.success()
    }

    private fun calculatePoints(millis: Long): Int {
        val hours = millis / 3_600_000.0
        return when {
            hours >= 4.0 -> 200  // 40 + 60 + 100
            hours >= 3.0 -> 100  // 40 + 60
            hours >= 2.0 -> 40   // 40
            else         -> 0
        }
    }

    private fun sendNotification(creditedAmount: Int, totalPoints: Int) {
        com.quizedguy.reelnearn.shared.util.NotificationHelper.createNotificationChannels(applicationContext)
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val intent = Intent(applicationContext, Class.forName("com.quizedguy.reelnearn.MainActivity")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val message = "+$creditedAmount points for yesterday's watch goals have been added directly to your balance (Total: $totalPoints pts)."
        val notification = NotificationCompat.Builder(applicationContext, com.quizedguy.reelnearn.shared.util.NotificationHelper.CHANNEL_REWARDS)
            .setContentTitle("Yesterday's Points Added! 🎉")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }
}


