package com.quizedguy.reelnearn.shared.data

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Process
import android.provider.Settings
import java.time.LocalDate
import java.time.ZoneId

object UsageStatsHelper {

    /** Apps whose combined watch time earns points */
    val TRACKED_APPS = setOf(
        "com.instagram.android",         // Instagram (Reels)
        "com.google.android.youtube",    // YouTube (Shorts + videos)
        "com.zhiliaoapp.musically",      // TikTok
        "com.ss.android.ugc.trill"       // TikTok (some regions)
    )

    /** Returns which of TRACKED_APPS are actually installed on this device */
    fun getInstalledTrackedApps(context: Context): List<String> {
        val pm = context.packageManager
        return TRACKED_APPS.filter { pkg ->
            try {
                pm.getPackageInfo(pkg, 0)
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    data class AppWatchBreakdown(
        val instagramMillis: Long = 0L,
        val youtubeMillis: Long = 0L,
        val tiktokMillis: Long = 0L
    ) {
        val totalMillis: Long get() = instagramMillis + youtubeMillis + tiktokMillis
    }

    /**
     * Returns today's combined foreground time (millis) for Instagram + YouTube + TikTok.
     * This is the value shown on the dashboard and used for reward tier calculation.
     */
    fun getReelWatchTime(context: Context): Long {
        return getReelWatchTimeForDate(context, LocalDate.now())
    }

    /**
     * Returns individual watch breakdown for Instagram, YouTube, and TikTok today.
     */
    fun getReelWatchBreakdown(context: Context): AppWatchBreakdown {
        return getReelWatchBreakdownForDate(context, LocalDate.now())
    }

    fun getReelWatchBreakdownForDate(context: Context, date: LocalDate): AppWatchBreakdown {
        if (!hasUsageStatsPermission(context)) return AppWatchBreakdown()

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val start = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val end   = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val events = usageStatsManager.queryEvents(start, end)
        val event  = UsageEvents.Event()

        var igTime = 0L
        var ytTime = 0L
        var ttTime = 0L

        var currentApp: String? = null
        var currentAppStartTime = 0L

        fun addTimeToApp(pkg: String?, delta: Long) {
            if (delta <= 0L || pkg == null) return
            when {
                pkg == "com.instagram.android" -> igTime += delta
                pkg == "com.google.android.youtube" -> ytTime += delta
                pkg == "com.zhiliaoapp.musically" || pkg == "com.ss.android.ugc.trill" -> ttTime += delta
            }
        }

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg  = event.packageName ?: continue
            val time = event.timeStamp

            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    if (pkg in TRACKED_APPS) {
                        if (currentApp != null && currentApp != pkg) {
                            addTimeToApp(currentApp, time - currentAppStartTime)
                        }
                        currentApp = pkg
                        currentAppStartTime = time
                    } else {
                        if (currentApp != null) {
                            addTimeToApp(currentApp, time - currentAppStartTime)
                            currentApp = null
                            currentAppStartTime = 0L
                        }
                    }
                }
                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    if (currentApp != null && currentApp == pkg) {
                        addTimeToApp(currentApp, time - currentAppStartTime)
                        currentApp = null
                        currentAppStartTime = 0L
                    }
                }
                16, 17 -> { // SCREEN_NON_INTERACTIVE / KEYGUARD_SHOWN
                    if (currentApp != null) {
                        addTimeToApp(currentApp, time - currentAppStartTime)
                        currentApp = null
                        currentAppStartTime = 0L
                    }
                }
            }
        }

        if (currentApp != null && currentAppStartTime > 0L) {
            val queryEnd = minOf(System.currentTimeMillis(), end)
            if (queryEnd > currentAppStartTime) {
                addTimeToApp(currentApp, queryEnd - currentAppStartTime)
            }
        }

        return AppWatchBreakdown(
            instagramMillis = igTime,
            youtubeMillis = ytTime,
            tiktokMillis = ttTime
        )
    }

    /**
     * Returns the combined foreground time (millis) for tracked apps on the given date.
     */
    fun getReelWatchTimeForDate(context: Context, date: LocalDate): Long {
        if (!hasUsageStatsPermission(context)) return 0L

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val start = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val end   = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val events = usageStatsManager.queryEvents(start, end)
        val event  = UsageEvents.Event()

        var totalTime      = 0L
        var currentApp: String?  = null
        var currentAppStartTime  = 0L

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg  = event.packageName ?: continue
            val time = event.timeStamp

            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    if (pkg in TRACKED_APPS) {
                        if (currentApp != null && currentApp != pkg) {
                            totalTime += time - currentAppStartTime
                        }
                        currentApp          = pkg
                        currentAppStartTime = time
                    } else {
                        // Another app came to foreground — pause tracking
                        if (currentApp != null) {
                            totalTime      += time - currentAppStartTime
                            currentApp      = null
                            currentAppStartTime = 0L
                        }
                    }
                }
                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    if (currentApp != null && currentApp == pkg) {
                        totalTime      += time - currentAppStartTime
                        currentApp      = null
                        currentAppStartTime = 0L
                    }
                }
                16, 17 -> { // SCREEN_NON_INTERACTIVE / KEYGUARD_SHOWN
                    if (currentApp != null) {
                        totalTime      += time - currentAppStartTime
                        currentApp      = null
                        currentAppStartTime = 0L
                    }
                }
            }
        }

        // App still in foreground at query end
        if (currentApp != null && currentAppStartTime > 0L) {
            val queryEnd = minOf(System.currentTimeMillis(), end)
            if (queryEnd > currentAppStartTime) {
                totalTime += queryEnd - currentAppStartTime
            }
        }

        return totalTime
    }

    fun getDeviceId(context: Context): String {
        return try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun getTodayTotalScreenTime(context: Context): Long {
        return getUsageForDate(context, LocalDate.now())
    }

    private fun getLauncherPackages(context: Context): Set<String> {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        
        val launcherPackages = mutableSetOf<String>()
        try {
            val list = pm.queryIntentActivities(launcherIntent, 0)
            for (resolveInfo in list) {
                resolveInfo.activityInfo?.packageName?.let {
                    launcherPackages.add(it)
                }
            }
        } catch (e: Exception) {
            // Fallback
        }
        
        // Also add the default home launcher
        try {
            val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val defaultLauncherInfo = pm.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
            defaultLauncherInfo?.activityInfo?.packageName?.let {
                launcherPackages.add(it)
            }
        } catch (e: Exception) {
            // Fallback
        }
        
        return launcherPackages
    }

    fun getUsageForDate(context: Context, date: LocalDate): Long {
        if (!hasUsageStatsPermission(context)) return 0L

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        
        // Start and end of the specified date
        val start = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        
        val launcherPackages = getLauncherPackages(context)
        
        // Initial state carryover check
        // Query events from 1 hour before midnight to midnight to determine what was in foreground
        val preEvents = usageStatsManager.queryEvents(start - 3600000, start)
        val preEvent = UsageEvents.Event()
        var initialApp: String? = null
        var initialAppStartTime = 0L
        
        while (preEvents.hasNextEvent()) {
            preEvents.getNextEvent(preEvent)
            val eventType = preEvent.eventType
            val pkg = preEvent.packageName
            val time = preEvent.timeStamp
            
            when (eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    if (pkg != null && (pkg == context.packageName || launcherPackages.contains(pkg))) {
                        initialApp = pkg
                        initialAppStartTime = time
                    } else {
                        initialApp = null
                        initialAppStartTime = 0L
                    }
                }
                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    if (initialApp != null && initialApp == pkg) {
                        initialApp = null
                        initialAppStartTime = 0L
                    }
                }
                16, 17 -> { // SCREEN_NON_INTERACTIVE, KEYGUARD_SHOWN
                    initialApp = null
                    initialAppStartTime = 0L
                }
            }
        }

        val events = usageStatsManager.queryEvents(start, end)
        val event = UsageEvents.Event()
        
        var totalTime = 0L
        var currentApp: String? = null
        var currentAppStartTime = 0L

        // If there was an app carryover from the previous day, initialize it
        if (initialApp != null) {
            currentApp = initialApp
            currentAppStartTime = start
        }
        
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val eventType = event.eventType
            val pkg = event.packageName
            val time = event.timeStamp
            
            when (eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    if (pkg != null && (pkg == context.packageName || launcherPackages.contains(pkg))) {
                        if (currentApp != null) {
                            if (currentApp != pkg) {
                                totalTime += (time - currentAppStartTime)
                                currentApp = pkg
                                currentAppStartTime = time
                            }
                        } else {
                            currentApp = pkg
                            currentAppStartTime = time
                        }
                    } else {
                        if (currentApp != null) {
                            totalTime += (time - currentAppStartTime)
                            currentApp = null
                            currentAppStartTime = 0L
                        }
                    }
                }
                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    if (currentApp != null && currentApp == pkg) {
                        totalTime += (time - currentAppStartTime)
                        currentApp = null
                        currentAppStartTime = 0L
                    }
                }
                16 -> { // UsageEvents.Event.SCREEN_NON_INTERACTIVE
                    if (currentApp != null) {
                        totalTime += (time - currentAppStartTime)
                        currentApp = null
                        currentAppStartTime = 0L
                    }
                }
                17 -> { // UsageEvents.Event.KEYGUARD_SHOWN
                    if (currentApp != null) {
                        totalTime += (time - currentAppStartTime)
                        currentApp = null
                        currentAppStartTime = 0L
                    }
                }
            }
        }
        
        // If an app is still in the foreground at the end of the query range
        if (currentApp != null && currentAppStartTime > 0L) {
            val queryEnd = Math.min(System.currentTimeMillis(), end)
            if (queryEnd > currentAppStartTime) {
                totalTime += (queryEnd - currentAppStartTime)
            }
        }
        
        return totalTime
    }
}


