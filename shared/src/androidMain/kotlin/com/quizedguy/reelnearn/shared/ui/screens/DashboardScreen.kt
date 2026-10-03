package com.quizedguy.reelnearn.shared.ui.screens

import android.app.Activity
import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.quizedguy.reelnearn.shared.R
import com.quizedguy.reelnearn.shared.data.UsageStatsHelper
import com.quizedguy.reelnearn.shared.ui.components.BannerAdView
import com.quizedguy.reelnearn.shared.ui.navigation.Screen
import com.quizedguy.reelnearn.shared.ui.theme.*
import com.quizedguy.reelnearn.shared.ui.viewmodel.DashboardViewModel
import com.quizedguy.reelnearn.shared.ui.viewmodel.PointsViewModel
import com.quizedguy.reelnearn.shared.util.RewardedInterstitialAdManager
import kotlinx.coroutines.delay
import android.net.Uri

@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = viewModel(),
    pointsViewModel: PointsViewModel = viewModel()
) {
    val context = LocalContext.current
    val screenTime by viewModel.screenTimeMillis.collectAsState()
    val appBreakdown by viewModel.appBreakdown.collectAsState()
    val userPoints by pointsViewModel.userPoints.collectAsState()
    val updateNotice by viewModel.updateNotice.collectAsState()
    var dismissedNoticeKey by rememberSaveable(updateNotice?.updatedAt, updateNotice?.message) { mutableStateOf("") }
    val isNoticeDismissed = remember(dismissedNoticeKey, updateNotice) {
        updateNotice != null && dismissedNoticeKey == "${updateNotice?.updatedAt}_${updateNotice?.message}"
    }

    val pointCreditEvent by pointsViewModel.pointCreditEvent.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showReferralPopup by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(pointCreditEvent) {
        pointCreditEvent?.let { amount ->
            snackbarHostState.showSnackbar("You've been credited $amount points! 🎉")
            pointsViewModel.clearPointCreditEvent()
        }
    }

    if (updateNotice != null && updateNotice?.isActive == true && !isNoticeDismissed) {
        val notice = updateNotice!!
        AlertDialog(
            onDismissRequest = {
                if (!notice.isMandatory) {
                    dismissedNoticeKey = "${notice.updatedAt}_${notice.message}"
                }
            },
            title = { Text(notice.title, fontWeight = FontWeight.Bold) },
            text = { Text(notice.message) },
            confirmButton = {
                Button(
                    onClick = {
                        if (!notice.isMandatory) {
                            dismissedNoticeKey = "${notice.updatedAt}_${notice.message}"
                        }
                        if (notice.downloadUrl.isNotEmpty() && notice.downloadUrl.startsWith("http")) {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(notice.downloadUrl))
                                context.startActivity(intent)
                            } catch (e: Exception) {}
                        }
                    }
                ) {
                    Text(if (notice.downloadUrl.isNotEmpty() && notice.downloadUrl.startsWith("http")) "Open Link 🚀" else "OK", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                if (!notice.isMandatory) {
                    TextButton(onClick = { dismissedNoticeKey = "${notice.updatedAt}_${notice.message}" }) {
                        Text("Dismiss")
                    }
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = ReelBlack
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(ReelBlack)
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Top Bar / Header ────────────────────────────────
            item {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = null,
                        modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Reel n Earn", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = TextWhite)
                        Text("Watch. Earn. Repeat. 🎬", fontSize = 11.sp, color = TextGray)
                    }
                }
            }

            // ── Live Activity & Social Proof Ticker ──────────────
            item {
                LiveActivityTicker()
                Spacer(Modifier.height(14.dp))
            }

            // ── High-Energy Radial Hero Gauge Card ──────────────
            item {
                HeroWatchGaugeCard(
                    screenTimeMillis = screenTime,
                    formattedTime = viewModel.formatTime(screenTime),
                    onLaunchApp = { pkg ->
                        launchApp(context, pkg)
                    }
                )
                Spacer(Modifier.height(16.dp))
            }

            // ── Per-App Screen Time Breakdown Card ───────────────
            item {
                PerAppBreakdownCard(
                    breakdown = appBreakdown,
                    viewModel = viewModel
                )
                Spacer(Modifier.height(16.dp))
            }

            // ── Earn Hub Shortcut Card ───────────────────────────
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, NeonPink.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = ReelSurface),
                    onClick = {
                        val activity = context as? Activity
                        if (activity != null) {
                            RewardedInterstitialAdManager.showAd(activity, force = true)
                        }
                        navController.navigate(Screen.Earn.route)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🎁", fontSize = 34.sp)
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Earn Hub & Daily Games", fontWeight = FontWeight.ExtraBold, color = TextWhite, fontSize = 15.sp)
                            Text("Spin & win, daily check-in, 5-ad quest & tasks", style = MaterialTheme.typography.bodySmall, color = TextGray)
                        }
                        Button(
                            onClick = {
                                val activity = context as? Activity
                                if (activity != null) {
                                    RewardedInterstitialAdManager.showAd(activity, force = true)
                                }
                                navController.navigate(Screen.Earn.route)
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPink)
                        ) {
                            Text("Earn", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── Refer & Earn ─────────────────────────────────────
            item {
                ReferAndEarnCard(onReferClick = {
                    navController.navigate(Screen.Referrals.route)
                })
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

/**
 * Live Activity & Social Proof Ticker:
 * Auto-cycles real-time payouts, streak bonuses, and earning events to create social proof and excitement.
 */
@Composable
fun LiveActivityTicker() {
    val events = remember {
        listOf(
            "⚡ Priya just earned +50 Pts from YouTube Shorts",
            "🎉 Rahul successfully redeemed ₹50 UPI Cashback",
            "🔥 Alex unlocked a 7-day Daily Streak Boost (+20%)",
            "🎁 Sarah won 100 Pts on Lucky Daily Spin",
            "🏆 John reached Reel Master Milestone (+200 Pts)",
            "⚡ Amit claimed +50 Pts from 5-Ad Video Quest"
        )
    }

    var currentIndex by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(3500)
            currentIndex = (currentIndex + 1) % events.size
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, NeonCyan.copy(alpha = 0.25f), RoundedCornerShape(14.dp)),
        color = ReelSurface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pulsing live indicator
            val infiniteTransition = rememberInfiniteTransition()
            val alphaPulse by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(NeonCyan.copy(alpha = alphaPulse), CircleShape)
            )
            Spacer(Modifier.width(10.dp))

            AnimatedContent(
                targetState = events[currentIndex],
                transitionSpec = {
                    fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
                },
                modifier = Modifier.weight(1f)
            ) { eventText ->
                Text(
                    text = eventText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextWhite,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * High-Energy "Hero" Watch-Time Radial Gauge Card:
 * Features a circular neon speedometer arc, center live timer, multiplier boost,
 * and quick-launch shortcuts for Instagram, YouTube, and TikTok.
 */
@Composable
fun HeroWatchGaugeCard(
    screenTimeMillis: Long,
    formattedTime: String,
    onLaunchApp: (String) -> Unit
) {
    val hours = screenTimeMillis / 3_600_000.0

    val (tierLabel, tierTargetHours, tierPoints) = when {
        hours >= 4.0 -> Triple("Reel Master 🏆", 4.0, 100)
        hours >= 3.0 -> Triple("Reel Master 🏆", 4.0, 100)
        hours >= 2.0 -> Triple("Reel Pro 🔥", 3.0, 60)
        else         -> Triple("Reel Starter ⭐", 2.0, 40)
    }

    val progressPercent = (hours / tierTargetHours).coerceIn(0.0, 1.0).toFloat()

    val animatedProgress by animateFloatAsState(
        targetValue = progressPercent,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.5.dp,
                Brush.horizontalGradient(listOf(NeonCyan, NeonPink, NeonPurple)),
                RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = ReelSurface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📱", fontSize = 18.sp)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Reel Screen Time",
                            fontWeight = FontWeight.ExtraBold,
                            color = TextWhite,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Instagram • YouTube",
                            fontSize = 11.sp,
                            color = TextGray
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NeonCyan.copy(alpha = 0.12f),
                    modifier = Modifier.border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(7.dp).background(NeonCyan, CircleShape))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Active",
                            color = NeonCyan,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // BIG, BOLD Prominent Timer Display
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(NeonPink.copy(alpha = 0.12f), Color.Transparent),
                            radius = 350f
                        ),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "⏱️ TODAY'S WATCH TIME",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextGray,
                    letterSpacing = 1.2.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = formattedTime,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.2.sp
                )
                Spacer(Modifier.height(10.dp))

                // Goal Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NeonPink.copy(alpha = 0.18f),
                    modifier = Modifier.border(1.dp, NeonPink.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🎯", fontSize = 12.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "${(progressPercent * 100).toInt()}% of ${tierTargetHours.toInt()}h Milestone ($tierPoints Pts)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Glowing Neon Multi-Tier Progress Bar
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(ReelSurfaceHigh)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(NeonCyan, NeonPink, NeonPurple)
                                )
                            )
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Milestone Tier Markers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(
                        Triple("2h", "40 pts", hours >= 2.0),
                        Triple("3h", "60 pts", hours >= 3.0),
                        Triple("4h", "100 pts", hours >= 4.0)
                    ).forEach { (h, pts, achieved) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(if (achieved) NeonPink else TextDimmed, CircleShape)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "$h ($pts)",
                                fontSize = 11.sp,
                                fontWeight = if (achieved) FontWeight.Bold else FontWeight.Normal,
                                color = if (achieved) NeonPink else TextGray
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // Quick App Launchers Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚡ TAP TO OPEN & EARN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextGray,
                    letterSpacing = 0.8.sp
                )
            }
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppLauncherButton(
                    modifier = Modifier.weight(1f),
                    title = "Instagram",
                    subtitle = "Reels",
                    iconEmoji = "📸",
                    accentColor = Color(0xFFE1306C),
                    onClick = { onLaunchApp("com.instagram.android") }
                )
                AppLauncherButton(
                    modifier = Modifier.weight(1f),
                    title = "YouTube",
                    subtitle = "Shorts",
                    iconEmoji = "▶️",
                    accentColor = Color(0xFFFF0000),
                    onClick = { onLaunchApp("com.google.android.youtube") }
                )
            }
        }
    }
}

@Composable
fun AppLauncherButton(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    iconEmoji: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        color = ReelSurfaceHigh,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(iconEmoji, fontSize = 22.sp)
            Spacer(Modifier.height(4.dp))
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextWhite)
            Text(subtitle, fontSize = 10.sp, color = accentColor, fontWeight = FontWeight.SemiBold)
        }
    }
}

/**
 * Per-App Screen Time Breakdown Card:
 * Shows detailed breakdown for YouTube and Instagram with individual progress bars and times.
 */
@Composable
fun PerAppBreakdownCard(
    breakdown: UsageStatsHelper.AppWatchBreakdown,
    viewModel: DashboardViewModel
) {
    val total = breakdown.totalMillis.coerceAtLeast(1L).toFloat()
    val ytPercent = (breakdown.youtubeMillis / total).coerceIn(0f, 1f)
    val igPercent = (breakdown.instagramMillis / total).coerceIn(0f, 1f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ReelDivider, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ReelSurface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📊 App Usage Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Text(
                    text = viewModel.formatShortTime(breakdown.totalMillis),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
            }
            Spacer(Modifier.height(14.dp))

            // YouTube Row
            AppBreakdownRow(
                icon = "▶️",
                appName = "YouTube (Shorts & Videos)",
                timeFormatted = viewModel.formatShortTime(breakdown.youtubeMillis),
                percentage = ytPercent,
                barColor = Color(0xFFFF334B)
            )
            Spacer(Modifier.height(12.dp))

            // Instagram Row
            AppBreakdownRow(
                icon = "📸",
                appName = "Instagram (Reels)",
                timeFormatted = viewModel.formatShortTime(breakdown.instagramMillis),
                percentage = igPercent,
                barColor = Color(0xFFE1306C)
            )
        }
    }
}

@Composable
fun AppBreakdownRow(
    icon: String,
    appName: String,
    timeFormatted: String,
    percentage: Float,
    barColor: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 14.sp)
                Spacer(Modifier.width(8.dp))
                Text(appName, fontSize = 12.sp, color = TextWhite, fontWeight = FontWeight.SemiBold)
            }
            Text(timeFormatted, fontSize = 12.sp, color = TextGray, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(ReelSurfaceHigh)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percentage)
                    .fillMaxHeight()
                    .background(barColor)
            )
        }
    }
}

@Composable
fun ReferAndEarnCard(onReferClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ReelSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, ReelDivider)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("👥", fontSize = 34.sp)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Refer & Earn +250 Pts", fontWeight = FontWeight.ExtraBold, color = TextWhite, fontSize = 15.sp)
                Text("Invite friends to Reel n Earn and get instant rewards", style = MaterialTheme.typography.bodySmall, color = TextGray)
            }
            Button(
                onClick = onReferClick,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text("Invite", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

fun launchApp(context: Context, packageName: String) {
    try {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            context.startActivity(launchIntent)
        } else {
            val playStoreIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
            context.startActivity(playStoreIntent)
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open app", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun PermissionRequiredScreen(
    hasUsage: Boolean,
    hasNotification: Boolean,
    onRequestUsage: () -> Unit,
    onRequestNotification: () -> Unit
) {
    var showUsageDisclosure by rememberSaveable { mutableStateOf(false) }

    if (showUsageDisclosure) {
        AlertDialog(
            onDismissRequest = { showUsageDisclosure = false },
            title = { Text("Data Usage Disclosure", fontWeight = FontWeight.Bold) },
            text = { Text("Reel n Earn needs access to your App Usage data to track your screen time and calculate your engagement bonuses. This data never leaves your device and is only used to award points.") },
            confirmButton = {
                Button(onClick = {
                    showUsageDisclosure = false
                    onRequestUsage()
                }) {
                    Text("I Agree")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUsageDisclosure = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Permissions Required",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Reel n Earn needs usage and notification access to track your screen time and reward your engagement.",
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        if (!hasUsage) {
            Button(onClick = { showUsageDisclosure = true }, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Grant Usage Access")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (!hasNotification) {
            Button(onClick = onRequestNotification, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Grant Notification Access")
            }
        }
    }
}
