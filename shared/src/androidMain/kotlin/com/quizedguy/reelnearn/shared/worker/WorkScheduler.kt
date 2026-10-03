package com.quizedguy.reelnearn.shared.worker

import android.content.Context
import androidx.work.*
import java.util.Calendar
import java.util.concurrent.TimeUnit

object WorkScheduler {

    private const val USAGE_WORK_NAME = "reelnearn_usage_notifications"
    private const val MIDNIGHT_WORK_NAME = "reelnearn_midnight_rewards"

    fun schedulePeriodicWorkers(context: Context) {
        val workManager = WorkManager.getInstance(context)

        // 1. Periodic Usage & Goal Tracking (every 15 minutes)
        val usageConstraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false)
            .build()

        val usageWorkRequest = PeriodicWorkRequestBuilder<UsageNotificationWorker>(
            15, TimeUnit.MINUTES,
            5, TimeUnit.MINUTES
        )
            .setConstraints(usageConstraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            USAGE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            usageWorkRequest
        )

        // 2. Midnight Daily Finalizer Worker (requires Network connectivity)
        val midnightConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val now = Calendar.getInstance()
        val midnight = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 5)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val initialDelay = midnight.timeInMillis - now.timeInMillis

        val midnightWorkRequest = PeriodicWorkRequestBuilder<MidnightRewardWorker>(
            24, TimeUnit.HOURS
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(midnightConstraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            MIDNIGHT_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            midnightWorkRequest
        )
    }
}
