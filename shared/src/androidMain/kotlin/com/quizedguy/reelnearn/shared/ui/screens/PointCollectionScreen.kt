package com.quizedguy.reelnearn.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.quizedguy.reelnearn.shared.ui.theme.*
import com.quizedguy.reelnearn.shared.ui.viewmodel.DailyUsageRecord
import com.quizedguy.reelnearn.shared.ui.viewmodel.PointsViewModel
import com.quizedguy.reelnearn.shared.data.UsageStatsHelper
import java.util.concurrent.TimeUnit
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PointCollectionScreen(
    navController: NavController,
    pointsViewModel: PointsViewModel = viewModel()
) {
    val dailyUsage by pointsViewModel.dailyUsageHistory.collectAsState()
    val collectingRecordIds by pointsViewModel.collectingRecordIds.collectAsState()
    val context = LocalContext.current

    // Show a snack when points are credited
    val snackbarHostState = remember { SnackbarHostState() }
    val pointCreditEvent by pointsViewModel.pointCreditEvent.collectAsState()
    LaunchedEffect(pointCreditEvent) {
        pointCreditEvent?.let { pts ->
            snackbarHostState.showSnackbar("+$pts pts added to your balance! 🎉")
            pointsViewModel.clearPointCreditEvent()
        }
    }

    LaunchedEffect(Unit) {
        pointsViewModel.syncUsageHistory(context)
    }

    Scaffold(
        containerColor = ReelBlack,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Collect Points", fontWeight = FontWeight.Bold, color = TextWhite) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ReelBlack)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(ReelBlack)
                .padding(16.dp)
        ) {
            item {
                InfoCard()
                Spacer(modifier = Modifier.height(24.dp))
                WeeklyUsageGraph(usageHistory = dailyUsage)
                Text(
                    text = "Daily Usage History",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (dailyUsage.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NeonPink)
                    }
                }
            } else {
                items(dailyUsage) { record ->
                    UsageHistoryItem(
                        record = record,
                        pointsViewModel = pointsViewModel
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun InfoCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, NeonPink.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ReelSurface)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.Info, contentDescription = null, tint = NeonPink, modifier = Modifier.padding(top = 2.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Daily Reel Rewards 🎬💰",
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Complete your daily Instagram & YouTube watch goals and collect points by watching a short video ad:\n\n• ⭐ 2h Watch: 40 pts\n• 🔥 3h Watch: 60 pts\n• 🏆 4h Watch: 100 pts\n\nCollect all 3 goals to earn 200 points daily!",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray
                )
            }
        }
    }
}

@Composable
fun UsageHistoryItem(
    record: DailyUsageRecord,
    pointsViewModel: PointsViewModel
) {
    val context = LocalContext.current
    val isToday = record.date == java.time.LocalDate.now().toString()
    val displayMillis = if (isToday) {
        val liveReelTime = UsageStatsHelper.getReelWatchTime(context)
        if (liveReelTime > 0L) liveReelTime else record.totalMillis
    } else {
        record.totalMillis
    }
    val hours = displayMillis / 3600000.0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (isToday) NeonCyan.copy(alpha = 0.4f) else ReelDivider, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ReelSurface)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left — date + screen time
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = record.date,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextWhite
                        )
                        if (isToday) {
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = NeonCyan.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "TODAY",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = NeonCyan
                                )
                            }
                        }
                    }
                    Text(
                        text = "Total: ${formatDuration(displayMillis)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray
                    )
                }

                // Right summary
                val totalCollectedForDay = (if (record.tier1Collected) 40 else 0) +
                                           (if (record.tier2Collected) 60 else 0) +
                                           (if (record.tier3Collected) 100 else 0)
                if (totalCollectedForDay > 0) {
                    Text(
                        text = "$totalCollectedForDay / 200 pts",
                        color = SuccessGreen,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = ReelDivider)
            Spacer(Modifier.height(8.dp))

            // 3 Goal Tier Rows
            MilestoneTierRow(
                title = "⭐ 2h Goal (40 Pts)",
                isCompleted = hours >= 2.0,
                isCollected = record.tier1Collected,
                points = 40,
                onCollect = {
                    pointsViewModel.collectMilestoneReward(context, 1, record.date)
                }
            )

            MilestoneTierRow(
                title = "🔥 3h Goal (60 Pts)",
                isCompleted = hours >= 3.0,
                isCollected = record.tier2Collected,
                points = 60,
                onCollect = {
                    pointsViewModel.collectMilestoneReward(context, 2, record.date)
                }
            )

            MilestoneTierRow(
                title = "🏆 4h Goal (100 Pts)",
                isCompleted = hours >= 4.0,
                isCollected = record.tier3Collected,
                points = 100,
                onCollect = {
                    pointsViewModel.collectMilestoneReward(context, 3, record.date)
                }
            )
        }
    }
}

@Composable
fun MilestoneTierRow(
    title: String,
    isCompleted: Boolean,
    isCollected: Boolean,
    points: Int,
    onCollect: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, fontSize = 12.sp, color = if (isCompleted) TextWhite else TextGray, fontWeight = FontWeight.SemiBold)

        when {
            isCollected -> {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SuccessGreen.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "✓ Collected",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        color = SuccessGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }
            isCompleted -> {
                Button(
                    onClick = onCollect,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Collect 🎬",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp
                    )
                }
            }
            else -> {
                Text(
                    text = "🔒 Locked",
                    color = TextDimmed,
                    fontSize = 11.sp
                )
            }
        }
    }
}

// ── Expiry helpers ─────────────────────────────────────────────────────────

private fun daysUntilExpiry(record: DailyUsageRecord): Int {
    if (!record.isApproved || record.isCollected) return 7
    if (record.approvedAt == 0L) return 7
    val daysSince = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - record.approvedAt)
    return (7 - daysSince).toInt().coerceAtLeast(0)
}

private fun isExpiredRecord(record: DailyUsageRecord): Boolean {
    if (!record.isApproved || record.isCollected) return false
    if (record.approvedAt == 0L) return false
    return daysUntilExpiry(record) <= 0
}

@Composable
fun ExpiryCountdownBar(daysLeft: Int) {
    val expiryColor = when {
        daysLeft <= 1 -> ErrorRed
        daysLeft <= 3 -> AlertOrange
        else          -> SuccessGreen
    }
    val progress by animateFloatAsState(
        targetValue = (daysLeft / 7f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800),
        label = "expiryProgress"
    )
    val expiryLabel = when {
        daysLeft <= 0 -> "⚠️ Expires today!"
        daysLeft == 1 -> "⚠️ Expires tomorrow"
        else          -> "⏳ Expires in $daysLeft days"
    }
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = expiryLabel,
                style = MaterialTheme.typography.labelSmall,
                color = expiryColor,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "$daysLeft / 7 days left",
                style = MaterialTheme.typography.labelSmall,
                color = TextGray
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = expiryColor,
            trackColor = expiryColor.copy(alpha = 0.15f)
        )
    }
}

private fun formatDuration(millis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    return "Reel Time: ${hours}h ${minutes}m"
}

@Composable
fun WeeklyUsageGraph(usageHistory: List<DailyUsageRecord>) {
    if (usageHistory.isEmpty()) return

    val weeklyData = usageHistory.take(7).reversed()
    val maxUsage = weeklyData.maxOfOrNull { it.totalMillis }?.coerceAtLeast(1) ?: 1

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
            .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ReelSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Weekly Overview", 
                style = MaterialTheme.typography.titleMedium, 
                fontWeight = FontWeight.Bold,
                color = NeonCyan
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                weeklyData.forEach { record ->
                    val heightRatio = (record.totalMillis.toFloat() / maxUsage.toFloat()).coerceIn(0.05f, 1f)
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(28.dp)
                                    .fillMaxHeight(heightRatio)
                                    .background(
                                        Brush.verticalGradient(
                                            if (record.isCollected) listOf(NeonPink.copy(alpha = 0.4f), NeonPurple.copy(alpha = 0.4f))
                                            else listOf(NeonPink, NeonPurple)
                                        ),
                                        shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                    )
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = record.date.takeLast(2),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextGray
                        )
                    }
                }
            }
        }
    }
}


