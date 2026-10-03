package com.quizedguy.reelnearn.shared.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.time.LocalDate

object NotificationHelper {

    const val CHANNEL_GOALS = "channel_goals"
    const val CHANNEL_REMINDERS = "channel_reminders"
    const val CHANNEL_REWARDS = "channel_rewards"

    const val EXTRA_NAVIGATE_TO = "navigate_to"

    // Destination routes matching Screen definitions
    const val DEST_COLLECTION = "collection"
    const val DEST_SPONSORED_TASKS = "sponsored_tasks"
    const val DEST_REWARDS = "rewards"

    private const val PREFS_NOTIFICATIONS = "reelnearn_notifications_prefs"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            // 1. Goal & Milestone Alerts Channel (High Priority)
            val goalsChannel = NotificationChannel(
                CHANNEL_GOALS,
                "Goal Completions & Milestones",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies you as soon as you complete 2h, 3h, and 4h daily watch goals"
                enableVibration(true)
                setShowBadge(true)
            }

            // 2. Daily Reminders (Spin & Check-In)
            val remindersChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "Daily Rewards & Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for your free Daily Spin and Daily Check-In streak bonus"
                enableVibration(true)
                setShowBadge(true)
            }

            // 3. Points & Reward Payouts
            val rewardsChannel = NotificationChannel(
                CHANNEL_REWARDS,
                "Points & Rewards",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Updates on points credited, approvals, and payout delivery"
                setShowBadge(true)
            }

            notificationManager.createNotificationChannels(listOf(goalsChannel, remindersChannel, rewardsChannel))
        }
    }

    private fun getLaunchPendingIntent(context: Context, navigateTo: String? = null, requestCode: Int = 0): PendingIntent {
        val intent = Intent(context, Class.forName("com.quizedguy.reelnearn.MainActivity")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (navigateTo != null) {
                putExtra(EXTRA_NAVIGATE_TO, navigateTo)
            }
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            flags
        )
    }

    /**
     * Sends a rich notification when a user completes a daily watch goal.
     * Includes direct deep-linking and action button to claim points immediately.
     */
    fun notifyGoalCompleted(context: Context, tier: Int, hours: Int, points: Int) {
        val today = LocalDate.now().toString()
        val prefs = context.getSharedPreferences(PREFS_NOTIFICATIONS, Context.MODE_PRIVATE)
        val key = "goal_completed_${today}_tier_$tier"
        
        if (prefs.getBoolean(key, false)) {
            return // Already notified today for this goal tier
        }

        createNotificationChannels(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val (iconHeader, title) = when (tier) {
            1 -> Pair("⭐", "2h Goal Completed! (+40 Pts)")
            2 -> Pair("🔥", "3h Goal Completed! (+60 Pts)")
            3 -> Pair("🏆", "4h Goal Completed! (+100 Pts)")
            else -> Pair("🎉", "${hours}h Goal Completed! (+$points Pts)")
        }

        val message = "$iconHeader Congratulations! You've unlocked $points bonus points for your ${hours}h watch goal. Tap below to claim now! 🎬"
        val contentPendingIntent = getLaunchPendingIntent(context, DEST_COLLECTION, 100 + tier)
        val actionPendingIntent = getLaunchPendingIntent(context, DEST_COLLECTION, 110 + tier)

        val notification = NotificationCompat.Builder(context, CHANNEL_GOALS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.ic_media_play, "🎬 Claim +$points Pts", actionPendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationId = 100 + tier
        notificationManager.notify(notificationId, notification)

        prefs.edit().putBoolean(key, true).apply()
    }

    /**
     * Sends a reminder notification that the user's free Daily Lucky Spin is available.
     * Includes direct deep-linking and action button to open the wheel.
     */
    fun notifyDailySpinAvailable(context: Context) {
        val today = LocalDate.now().toString()
        val spinPrefs = context.getSharedPreferences("lucky_spin_prefs", Context.MODE_PRIVATE)
        val lastSpinDate = spinPrefs.getString("last_spin_date", "") ?: ""

        // Only notify if user hasn't spun today
        if (lastSpinDate == today) return

        val notifPrefs = context.getSharedPreferences(PREFS_NOTIFICATIONS, Context.MODE_PRIVATE)
        val key = "spin_available_notified_$today"
        if (notifPrefs.getBoolean(key, false)) return

        createNotificationChannels(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val title = "🎰 Free Daily Spin is Ready!"
        val message = "Your daily Lucky Wheel spin is available! Spin now to win up to 250 bonus reward points."
        val contentPendingIntent = getLaunchPendingIntent(context, DEST_SPONSORED_TASKS, 201)
        val actionPendingIntent = getLaunchPendingIntent(context, DEST_SPONSORED_TASKS, 211)

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.ic_menu_rotate, "🎡 Spin Wheel Now", actionPendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(201, notification)
        notifPrefs.edit().putBoolean(key, true).apply()
    }

    /**
     * Sends a reminder notification that the user's Daily Check-In bonus is waiting.
     * Includes direct deep-linking and action button to claim streak points.
     */
    fun notifyDailyCheckInWaiting(context: Context, streakDay: Int = 1, points: Int = 20) {
        val today = LocalDate.now().toString()
        val notifPrefs = context.getSharedPreferences(PREFS_NOTIFICATIONS, Context.MODE_PRIVATE)
        val key = "checkin_waiting_notified_$today"
        if (notifPrefs.getBoolean(key, false)) return

        createNotificationChannels(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val title = "📅 Your Daily Check-In is Waiting!"
        val message = "Don't break your Day $streakDay streak! Open Reel n Earn to claim your +$points points bonus today."
        val contentPendingIntent = getLaunchPendingIntent(context, DEST_SPONSORED_TASKS, 202)
        val actionPendingIntent = getLaunchPendingIntent(context, DEST_SPONSORED_TASKS, 212)

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.ic_menu_agenda, "📅 Check In (+${points} Pts)", actionPendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(202, notification)
        notifPrefs.edit().putBoolean(key, true).apply()
    }

    /**
     * Sends notification when points have been approved or credited.
     */
    fun notifyPointsCredited(context: Context, amount: Int, reason: String = "Watch Milestones") {
        createNotificationChannels(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val title = "Points Credited! 🎉"
        val message = "You received +$amount points from your $reason! Open app to check your updated balance."
        val contentPendingIntent = getLaunchPendingIntent(context, DEST_REWARDS, 301)
        val actionPendingIntent = getLaunchPendingIntent(context, DEST_REWARDS, 311)

        val notification = NotificationCompat.Builder(context, CHANNEL_REWARDS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.ic_menu_view, "💰 View Balance", actionPendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(301, notification)
    }
}
