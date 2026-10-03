package com.quizedguy.reelnearn.shared.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.filled.Share
import androidx.navigation.NavController
import com.quizedguy.reelnearn.shared.ui.navigation.Screen
import com.quizedguy.reelnearn.shared.ui.theme.*
import com.quizedguy.reelnearn.shared.ui.viewmodel.PointsViewModel
import com.quizedguy.reelnearn.shared.ui.viewmodel.SponsoredTask
import com.quizedguy.reelnearn.shared.ui.viewmodel.SponsoredTaskViewModel
import com.quizedguy.reelnearn.shared.ui.viewmodel.TaskCompletion
import com.quizedguy.reelnearn.shared.util.AgeSignalsHelper
import com.quizedguy.reelnearn.shared.util.RewardedAdManager
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun SponsoredTasksScreen(
    navController: NavController? = null,
    pointsViewModel: PointsViewModel = viewModel(),
    taskViewModel: SponsoredTaskViewModel = viewModel()
) {
    val context = LocalContext.current
    val userPoints by pointsViewModel.userPoints.collectAsState()
    val tasks by taskViewModel.tasks.collectAsState()
    val userCompletions by taskViewModel.userCompletions.collectAsState()
    val isLoading by taskViewModel.isLoading.collectAsState()
    val isMinor by AgeSignalsHelper.isMinor.collectAsState()
    var isAdLoading by remember { mutableStateOf(false) }
    var showRewardedAdSuccessDialog by remember { mutableStateOf(false) }

    var selectedTaskToClaim by remember { mutableStateOf<SponsoredTask?>(null) }
    var showClaimSuccessDialog by remember { mutableStateOf(false) }
    var showCheckInSuccessPopup by remember { mutableStateOf(false) }
    var successPointsAwarded by remember { mutableStateOf(0) }
    var successStreakDay by remember { mutableStateOf(0) }

    if (showCheckInSuccessPopup) {
        AlertDialog(
            onDismissRequest = { showCheckInSuccessPopup = false },
            containerColor = ReelSurfaceHigh,
            titleContentColor = TextWhite,
            textContentColor = TextGray,
            title = { Text("Check-in Successful! 🎉", fontWeight = FontWeight.Bold, color = TextWhite) },
            text = {
                Text(
                    "Congratulations! You claimed your Day $successStreakDay bonus of $successPointsAwarded points.",
                    color = TextGray
                )
            },
            confirmButton = {
                Button(
                    onClick = { showCheckInSuccessPopup = false },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Awesome!", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (selectedTaskToClaim != null) {
        val task = selectedTaskToClaim!!
        AlertDialog(
            onDismissRequest = { selectedTaskToClaim = null },
            containerColor = ReelSurfaceHigh,
            titleContentColor = TextWhite,
            textContentColor = TextGray,
            title = { Text("Confirm Task Submission", fontWeight = FontWeight.Bold, color = TextWhite) },
            text = {
                Text(
                    "Have you completed \"${task.title}\"?\n\nOnce submitted, your completion will be reviewed and ${task.pointsReward} points will be added to your balance.",
                    color = TextGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val currentTask = task
                        selectedTaskToClaim = null
                        taskViewModel.submitTaskCompletion(currentTask) { success, errorMsg ->
                            if (success) {
                                showClaimSuccessDialog = true
                            } else {
                                Toast.makeText(context, errorMsg ?: "Submission failed", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Yes, I Completed It", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedTaskToClaim = null }) {
                    Text("Not Yet", color = NeonCyan)
                }
            }
        )
    }

    if (showClaimSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showClaimSuccessDialog = false },
            containerColor = ReelSurfaceHigh,
            titleContentColor = TextWhite,
            textContentColor = TextGray,
            title = { Text("Submitted for Review! 🎉", fontWeight = FontWeight.Bold, color = TextWhite) },
            text = {
                Text("Your task completion has been submitted. Points will be credited once approved by our team!", color = TextGray)
            },
            confirmButton = {
                Button(
                    onClick = { showClaimSuccessDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Great!", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showRewardedAdSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showRewardedAdSuccessDialog = false },
            containerColor = ReelSurfaceHigh,
            titleContentColor = TextWhite,
            textContentColor = TextGray,
            title = { Text("Reward Earned! 🎉", fontWeight = FontWeight.Bold, color = TextWhite) },
            text = {
                Text("You got 2 points for watching the video! Keep watching to earn more points.", color = TextGray)
            },
            confirmButton = {
                Button(
                    onClick = { showRewardedAdSuccessDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Awesome", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ReelBlack)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Earn Points",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = NeonPink
                )
                Text(
                    text = "Complete partner tasks, surveys & offers to earn bonus points!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGray
                )
                Spacer(Modifier.height(4.dp))
            }

            // Balance Header Card
            item {
                TaskBalanceCard(points = userPoints)
            }

            // Gamified Lucky Daily Spin Wheel Card
            item {
                LuckySpinWheelCard(pointsViewModel = pointsViewModel)
            }

            // Daily Check-in Bonus Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, NeonCyan.copy(alpha = 0.35f), RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = ReelSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Daily Check-in Bonus 🎁",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Spacer(Modifier.height(12.dp))
                        DailyCheckInWidget(
                            pointsViewModel = pointsViewModel,
                            onClaimSuccess = { points, streak ->
                                successPointsAwarded = points
                                successStreakDay = streak
                                showCheckInSuccessPopup = true
                            }
                        )
                    }
                }
            }

            // Daily 5-Ad Video Quest
            if (!isMinor) {
                item {
                    Daily5AdQuestCard(pointsViewModel = pointsViewModel)
                }
            }

            // Watch Videos & Earn Feature Card
            if (!isMinor) {
                item {
                    WatchAndEarnCard(
                        isAdLoading = isAdLoading,
                        onWatchClick = {
                            val activity = context as? Activity
                            if (activity != null) {
                                if (RewardedAdManager.isAdAvailable()) {
                                    RewardedAdManager.showAd(activity) {
                                        showRewardedAdSuccessDialog = true
                                    }
                                } else {
                                    isAdLoading = true
                                    RewardedAdManager.loadAdOnDemand(activity) { success ->
                                        isAdLoading = false
                                        if (success) {
                                            RewardedAdManager.showAd(activity) {
                                                showRewardedAdSuccessDialog = true
                                            }
                                        } else {
                                            Toast.makeText(context, "No ads available right now. Please try again later.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }

            // High-Yield Featured Survey Banner
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = ReelSurface),
                    onClick = {
                        com.quizedguy.reelnearn.shared.util.SurveyManager.showSurveys(context)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🎁", fontSize = 36.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Featured Partner Surveys", fontWeight = FontWeight.ExtraBold, color = TextWhite)
                            Text("Earn 500 - 3,000 Points in 5 minutes", style = MaterialTheme.typography.bodySmall, color = TextGray)
                        }
                        Button(
                            onClick = { com.quizedguy.reelnearn.shared.util.SurveyManager.showSurveys(context) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                        ) {
                            Text("Start", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Refer & Earn Shortcut Banner
            if (navController != null) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, NeonPurple.copy(alpha = 0.4f), RoundedCornerShape(18.dp)),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = ReelSurface),
                        onClick = {
                            navController.navigate(Screen.Referrals.route)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("👥", fontSize = 36.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Refer & Earn", fontWeight = FontWeight.ExtraBold, color = TextWhite)
                                Text("Earn 250 bonus points for every friend you invite", style = MaterialTheme.typography.bodySmall, color = TextGray)
                            }
                            Button(
                                onClick = { navController.navigate(Screen.Referrals.route) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                            ) {
                                Text("Invite", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Available Tasks & Surveys",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = NeonPink)
                    }
                }
            } else if (tasks.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().border(1.dp, ReelDivider, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = ReelSurface)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("✨", fontSize = 40.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "No tasks available right now",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextWhite
                            )
                            Text(
                                "New sponsored surveys and tasks are added regularly. Check back soon!",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextGray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(tasks) { task ->
                    val completion = userCompletions.find { it.taskId == task.id }
                    SponsoredTaskCard(
                        task = task,
                        completion = completion,
                        onStartTask = {
                            if (task.taskUrl.isNotBlank()) {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(task.taskUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onClaimTask = {
                            selectedTaskToClaim = task
                        }
                    )
                }

                // Native Feed Ad Card
                item {
                    com.quizedguy.reelnearn.shared.ui.components.NativeAdView(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                }
            }

            // User Submissions History Section
            if (userCompletions.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Your Task Submissions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                }

                items(userCompletions) { item ->
                    UserCompletionHistoryCard(completion = item)
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun TaskBalanceCard(points: Int) {
    val gradient = Brush.horizontalGradient(
        colors = listOf(NeonPink, NeonPurple)
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .background(gradient)
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "Available Balance",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp
                )
                Text(
                    text = "$points Pts",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 30.sp,
                    color = Color.White
                )
            }
            Text("🪙", fontSize = 40.sp)
        }
    }
}

@Composable
fun SponsoredTaskCard(
    task: SponsoredTask,
    completion: TaskCompletion?,
    onStartTask: () -> Unit,
    onClaimTask: () -> Unit
) {
    val isCompleted = completion?.status == "Approved"
    val isPending = completion?.status == "Pending"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isCompleted) SuccessGreen.copy(alpha = 0.3f) else NeonPink.copy(alpha = 0.25f),
                RoundedCornerShape(18.dp)
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ReelSurface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = NeonPink.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = task.category.uppercase(),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = NeonPink
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = NeonGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "+${task.pointsReward} Pts",
                        fontWeight = FontWeight.ExtraBold,
                        color = NeonGold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )

            if (task.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⏱️ Est. ${task.estimatedMinutes} mins • ${task.sponsorName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextGray
                )

                when {
                    isCompleted -> {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SuccessGreen.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Completed", color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                    isPending -> {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AlertOrange.copy(alpha = 0.15f)
                        ) {
                            Text(
                                "Under Review",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = AlertOrange,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                    else -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (task.taskUrl.isNotBlank()) {
                                OutlinedButton(
                                    onClick = onStartTask,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                                ) {
                                    Text("Open Link", color = NeonCyan)
                                }
                            }

                            Button(
                                onClick = onClaimTask,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonPink)
                            ) {
                                Text("Claim Pts", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserCompletionHistoryCard(completion: TaskCompletion) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ReelDivider, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ReelSurface)
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(completion.taskTitle, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = TextWhite)
                Text(
                    text = "+${completion.pointsReward} Points",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeonGold,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = completion.status,
                fontWeight = FontWeight.Bold,
                color = when (completion.status) {
                    "Approved" -> SuccessGreen
                    "Pending" -> AlertOrange
                    else -> ErrorRed
                }
            )
        }
    }
}

@Composable
fun WatchAndEarnCard(
    isAdLoading: Boolean,
    onWatchClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, NeonPink.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ReelSurface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🎬", fontSize = 28.sp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Watch Videos & Earn",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextWhite
                        )
                        Text(
                            text = "Instant 2 points per video",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = NeonPink.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "+2 Pts / Video",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = NeonPink
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Watch short video ads anytime to earn instant points with no daily limits!",
                style = MaterialTheme.typography.bodySmall,
                color = TextGray
            )

            Spacer(Modifier.height(14.dp))

            Button(
                onClick = onWatchClick,
                enabled = !isAdLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isAdLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Preparing Video...", color = Color.White, fontWeight = FontWeight.Bold)
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Watch Video (+2 Pts)", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun Daily5AdQuestCard(pointsViewModel: PointsViewModel) {
    val context = LocalContext.current
    val activity: Activity? = context as? Activity
    val questCount by pointsViewModel.dailyAdQuestCount.collectAsState()
    var isWatching by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, NeonPink.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ReelSurface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Daily 5-Ad Video Quest 🎬", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = TextWhite)
                    Text("Watch 5 short videos for +50 Bonus Pts!", style = MaterialTheme.typography.bodySmall, color = TextGray)
                }
                Box(
                    modifier = Modifier
                        .background(NeonPink, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "$questCount / 5",
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            // Neon progress bar
            Box(
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(ReelSurfaceHigh)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((questCount / 5f).coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(Brush.horizontalGradient(listOf(NeonPink, NeonPurple)))
                )
            }
            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    if (activity != null) {
                        isWatching = true
                        pointsViewModel.incrementAdQuestStep(activity) { success, msg ->
                            isWatching = false
                            if (msg != null) {
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                enabled = questCount < 5 && !isWatching,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonPink)
            ) {
                if (isWatching) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                } else if (questCount >= 5) {
                    Text("✓ Quest Completed Today!", fontWeight = FontWeight.Bold, color = Color.White)
                } else {
                    Text("Watch Video (${questCount + 1}/5)", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun DailyCheckInWidget(
    pointsViewModel: PointsViewModel,
    onClaimSuccess: (Int, Int) -> Unit
) {
    val context = LocalContext.current
    val isClaimingCheckIn by pointsViewModel.isClaimingCheckIn.collectAsState()
    val checkInStreak by pointsViewModel.checkInStreak.collectAsState()
    val lastCheckInDate by pointsViewModel.lastCheckInDate.collectAsState()
    
    val hasCheckedInToday = remember(lastCheckInDate) {
        lastCheckInDate == java.time.LocalDate.now().toString()
    }
    val todayCheckInDay = remember(lastCheckInDate, checkInStreak) {
        pointsViewModel.getTodayCheckInDay()
    }
    
    Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            for (day in 1..7) {
                val isCompleted = if (hasCheckedInToday) {
                    day <= checkInStreak
                } else {
                    day < todayCheckInDay
                }
                val isActive = day == todayCheckInDay && !hasCheckedInToday
                val points = pointsViewModel.getPointsForDay(day)
                
                CheckInDayCircle(
                    day = day,
                    points = points,
                    isCompleted = isCompleted,
                    isActive = isActive
                )
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        val activity = context as? Activity
        if (hasCheckedInToday) {
            Button(
                onClick = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    disabledContainerColor = NeonPink.copy(alpha = 0.2f),
                    disabledContentColor = NeonPink.copy(alpha = 0.6f)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Checked In Today (Streak: $checkInStreak/7 Days)",
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            val currentPointsToClaim = pointsViewModel.getPointsForDay(todayCheckInDay)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        if (activity != null) {
                            pointsViewModel.claimDailyCheckIn(activity) { success ->
                                if (success) {
                                    onClaimSuccess(currentPointsToClaim, todayCheckInDay)
                                } else {
                                    Toast.makeText(context, "Claim failed. Please try again.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    enabled = !isClaimingCheckIn,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPink)
                ) {
                    if (isClaimingCheckIn) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(text = "Claim $currentPointsToClaim Pts", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun CheckInDayCircle(day: Int, points: Int, isCompleted: Boolean, isActive: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isCompleted -> NeonPink
                        isActive    -> NeonPink.copy(alpha = 0.3f)
                        else        -> ReelSurfaceHigh
                    }
                )
                .then(
                    if (isActive) Modifier.border(1.dp, NeonPink, CircleShape) else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text(
                    text = day.toString(),
                    color = if (isActive) NeonPink else TextGray,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "+$points",
            fontSize = 12.sp,
            color = if (isActive || isCompleted) NeonPink else TextDimmed,
            fontWeight = FontWeight.SemiBold
        )
    }
}

data class WheelSlice(val points: Int, val label: String, val color: Color, val textColor: Color = Color.White)

@Composable
fun LuckySpinWheelCard(pointsViewModel: PointsViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val rotationAnim = remember { Animatable(0f) }
    var isSpinning by remember { mutableStateOf(false) }
    var showWinDialog by remember { mutableStateOf(false) }
    var wonPoints by remember { mutableStateOf(0) }

    val serverLastSpinDate by pointsViewModel.lastSpinDate.collectAsState()
    val prefs = remember { context.getSharedPreferences("lucky_spin_prefs", Context.MODE_PRIVATE) }
    val todayStr = remember { LocalDate.now().toString() }
    var lastSpinDate by remember { mutableStateOf(prefs.getString("last_spin_date", "") ?: "") }
    val canSpinToday = (serverLastSpinDate != todayStr) && (lastSpinDate != todayStr)

    val slices = remember {
        listOf(
            WheelSlice(10, "+10", NeonPink),
            WheelSlice(25, "+25", Color(0xFF262B40)),
            WheelSlice(50, "+50", NeonPurple),
            WheelSlice(15, "+15", Color(0xFF1B3B36)),
            WheelSlice(20, "+20", NeonCyan, Color.Black),
            WheelSlice(250, "⭐250", NeonGold, Color.Black)
        )
    }

    if (showWinDialog) {
        AlertDialog(
            onDismissRequest = { showWinDialog = false },
            containerColor = ReelSurfaceHigh,
            title = { Text("🎉 YOU WON $wonPoints POINTS!", fontWeight = FontWeight.ExtraBold, color = NeonGold) },
            text = { Text("Congratulations! $wonPoints points have been credited to your balance.", color = TextWhite) },
            confirmButton = {
                Button(
                    onClick = { showWinDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Awesome!", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    fun startSpin() {
        if (isSpinning) return
        isSpinning = true

        // Weighted random selection: 10 (30%), 15 (25%), 20 (20%), 25 (15%), 50 (8%), 250 (2%)
        val weightedPool = listOf(
            0, 0, 0, 0, 0, 0, // +10 pts (30%)
            3, 3, 3, 3, 3,    // +15 pts (25%)
            4, 4, 4, 4,       // +20 pts (20%)
            1, 1, 1,          // +25 pts (15%)
            2, 2,             // +50 pts (8%)
            5                 // ⭐250 pts (2% Jackpot)
        )
        val targetIndex = weightedPool.random()
        val sweepAngle = 360f / slices.size
        val targetMidAngle = (targetIndex + 0.5f) * sweepAngle
        val targetNormalizedRotation = (270f - targetMidAngle + 360f) % 360f

        val currentRotation = rotationAnim.value
        val currentNormalizedRotation = (currentRotation % 360f + 360f) % 360f

        var diff = (targetNormalizedRotation - currentNormalizedRotation + 360f) % 360f
        if (diff < 180f) {
            diff += 360f
        }
        val extraRounds = 360f * 5
        val targetAngle = currentRotation + extraRounds + diff

        coroutineScope.launch {
            rotationAnim.animateTo(
                targetValue = targetAngle,
                animationSpec = tween(durationMillis = 3500, easing = FastOutSlowInEasing)
            )
            isSpinning = false
            wonPoints = slices[targetIndex].points

            prefs.edit().putString("last_spin_date", todayStr).apply()
            lastSpinDate = todayStr

            pointsViewModel.claimLuckySpin(wonPoints) { success ->
                if (success) {
                    showWinDialog = true
                }
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.2.dp, Brush.horizontalGradient(listOf(NeonGold, NeonPink)), RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = ReelSurface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "🎰 Lucky Daily Spin",
                        fontWeight = FontWeight.ExtraBold,
                        color = TextWhite,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Spin once daily to win up to 250 Bonus Points!",
                        fontSize = 11.sp,
                        color = TextGray
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (canSpinToday) SuccessGreen.copy(alpha = 0.2f) else ReelDivider.copy(alpha = 0.3f)
                ) {
                    Text(
                        text = if (canSpinToday) "1 DAILY SPIN" else "SPUN TODAY",
                        color = if (canSpinToday) SuccessGreen else TextGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Wheel Canvas & Pointer
            Box(
                modifier = Modifier.size(190.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(rotationAnim.value)
                ) {
                    val radius = size.minDimension / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val sweepAngle = 360f / slices.size

                    slices.forEachIndexed { i, slice ->
                        val startAngle = i * sweepAngle
                        drawArc(
                            color = slice.color,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = true,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = Size(radius * 2, radius * 2)
                        )
                    }

                    // Outer border
                    drawCircle(
                        color = NeonGold.copy(alpha = 0.6f),
                        radius = radius,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 5.dp.toPx())
                    )

                    // Draw labels
                    val paint = Paint().apply {
                        textAlign = Paint.Align.CENTER
                        textSize = 32f
                        typeface = Typeface.DEFAULT_BOLD
                    }

                    slices.forEachIndexed { i, slice ->
                        val midAngle = Math.toRadians((i * sweepAngle + sweepAngle / 2f).toDouble())
                        val textRadius = radius * 0.65f
                        val x = (center.x + textRadius * Math.cos(midAngle)).toFloat()
                        val y = (center.y + textRadius * Math.sin(midAngle)).toFloat() + 10f
                        paint.color = slice.textColor.toArgb()
                        drawContext.canvas.nativeCanvas.drawText(slice.label, x, y, paint)
                    }
                }

                // Center Hub
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(ReelBlack)
                        .border(2.dp, NeonGold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("SPIN", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = NeonGold)
                }

                // Top Pointer Needle
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-4).dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(NeonGold)
                        .border(2.dp, ReelBlack, CircleShape)
                )
            }

            Spacer(Modifier.height(18.dp))

            // Spin Action Button
            Button(
                onClick = {
                    if (isSpinning || !canSpinToday) return@Button
                    val activity = context as? Activity
                    if (activity != null) {
                        RewardedAdManager.showAdWithoutPoints(activity) { adCompleted ->
                            if (adCompleted) {
                                startSpin()
                            }
                        }
                    } else {
                        startSpin()
                    }
                },
                enabled = !isSpinning && canSpinToday,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (canSpinToday) NeonPink else ReelDivider,
                    disabledContainerColor = ReelDivider.copy(alpha = 0.5f),
                    disabledContentColor = TextGray
                ),
                modifier = Modifier.fillMaxWidth().height(46.dp)
            ) {
                if (isSpinning) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Spinning...", color = Color.White, fontWeight = FontWeight.Bold)
                } else if (canSpinToday) {
                    Text("🎬 Watch Ad to Spin (1 Daily)", color = Color.White, fontWeight = FontWeight.Bold)
                } else {
                    Text("✓ Spun for Today (Come back tomorrow)", color = TextGray, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}




