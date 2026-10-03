package com.quizedguy.reelnearn.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quizedguy.reelnearn.shared.ui.viewmodel.PointsViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.quizedguy.reelnearn.shared.ui.navigation.Screen
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Star
import android.app.Activity
import com.quizedguy.reelnearn.shared.util.RewardedAdManager
import com.quizedguy.reelnearn.shared.ui.theme.*

data class RewardItem(
    val title: String,
    val description: String,
    val points: Int,
    val amountRs: Int,
    val isUpi: Boolean = false
)

@Composable
fun RewardsScreen(
    navController: NavController,
    pointsViewModel: PointsViewModel = viewModel()
) {
    val userPoints by pointsViewModel.userPoints.collectAsState()
    val dailyUsage by pointsViewModel.dailyUsageHistory.collectAsState()
    val context = LocalContext.current
    
    val withdrawalHistory by pointsViewModel.withdrawalHistory.collectAsState()
    
    // Check if there are any uncollected points
    val hasUncollectedPoints = dailyUsage.any { it.isApproved && !it.isCollected && it.pointsPotential > 0 }
    
    var selectedRewardForClaim by remember { mutableStateOf<RewardItem?>(null) }
    var upiNameInput by remember { mutableStateOf("") }
    var upiIdInput by remember { mutableStateOf("") }

    val rewards = remember {
        listOf(
            RewardItem("UPI Cash Transfer", "₹50 Instant Cash", 1000, 50, isUpi = true)
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ReelBlack)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            item {
                Text(
                    text = "Redeem Rewards",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = NeonPink
                )
                Text(
                    text = "Turn your screen time into real value",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGray
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                BalanceCard(points = userPoints)
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        CollectionGatewayCard(
                            hasPoints = hasUncollectedPoints,
                            onClick = {
                                val activity = context as? Activity
                                if (activity != null) {
                                    RewardedAdManager.showAdWithoutPoints(activity) {
                                        navController.navigate(Screen.Collection.route)
                                    }
                                } else {
                                    navController.navigate(Screen.Collection.route)
                                }
                            }
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        RewardHistoryGatewayCard(
                            onClick = {
                                val activity = context as? Activity
                                if (activity != null) {
                                    RewardedAdManager.showAdWithoutPoints(activity) {
                                        navController.navigate(Screen.MyRewardsHistory.route)
                                    }
                                } else {
                                    navController.navigate(Screen.MyRewardsHistory.route)
                                }
                            }
                        )
                    }
                }
            }
                
            item {
                val minThreshold = 1000
                if (userPoints < minThreshold) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "💡 Earn at least $minThreshold points to unlock rewards. You need ${minThreshold - userPoints} more points.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = AlertOrange
                            )
                        }
                    }
                }
                
                Text(
                    text = "Available Rewards",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            items(rewards) { reward ->
                RewardCard(
                    reward = reward, 
                    canAfford = userPoints >= 1000 && userPoints >= reward.points,
                    onClaim = {
                        selectedRewardForClaim = reward
                        upiNameInput = ""
                        upiIdInput = ""
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            item {
                Text(
                    text = "We will add more reward options soon! 🎁",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGray,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    selectedRewardForClaim?.let { reward ->
        AlertDialog(
            onDismissRequest = { selectedRewardForClaim = null },
            containerColor = ReelSurfaceHigh,
            titleContentColor = TextWhite,
            textContentColor = TextGray,
            title = {
                Text(text = if (reward.isUpi) "Enter UPI ID for Payout" else "Confirm Reward Claim", color = TextWhite)
            },
            text = {
                Column {
                    Text(
                        text = "You are redeeming ${reward.title} (${reward.description}) for ${reward.points} Points.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextGray
                    )
                    if (reward.isUpi) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = upiNameInput,
                            onValueChange = { upiNameInput = it },
                            label = { Text("Account Holder Name", color = TextGray) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonPink,
                                unfocusedBorderColor = ReelDivider,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                cursorColor = NeonPink
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = upiIdInput,
                            onValueChange = { upiIdInput = it },
                            label = { Text("UPI ID (e.g. name@upi or phone@paytm)", color = TextGray) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonPink,
                                unfocusedBorderColor = ReelDivider,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                cursorColor = NeonPink
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val paymentInfo = if (reward.isUpi) "Name: ${upiNameInput.trim()} | UPI ID: ${upiIdInput.trim()}" else null
                        val activity = context as? Activity
                        if (activity != null) {
                            RewardedAdManager.showAdWithoutPoints(activity) {
                                pointsViewModel.requestWithdrawal(context, reward.amountRs, reward.points, reward.title, paymentInfo)
                            }
                        } else {
                            pointsViewModel.requestWithdrawal(context, reward.amountRs, reward.points, reward.title, paymentInfo)
                        }
                        selectedRewardForClaim = null
                    },
                    enabled = !reward.isUpi || (upiNameInput.trim().isNotEmpty() && upiIdInput.trim().isNotEmpty()),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Confirm & Withdraw 🎬", color = TextWhite, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedRewardForClaim = null }) {
                    Text("Cancel", color = NeonCyan)
                }
            }
        )
    }
}

@Composable
fun BalanceCard(points: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, NeonPink.copy(alpha = 0.4f), RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = ReelSurface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(NeonPink.copy(alpha = 0.08f), ReelSurface)))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(text = "Your Balance", style = MaterialTheme.typography.labelLarge, color = TextGray)
                Text(
                    text = "$points Points",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = NeonGold
                )
            }
        }
    }
}

@Composable
fun RewardCard(
    reward: RewardItem,
    canAfford: Boolean,
    onClaim: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, NeonPink.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ReelSurface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = reward.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = TextWhite)
                Text(text = reward.description, style = MaterialTheme.typography.bodySmall, color = TextGray)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${reward.points} Points",
                    style = MaterialTheme.typography.labelLarge,
                    color = NeonGold,
                    fontWeight = FontWeight.Bold
                )
            }
            Button(
                onClick = onClaim,
                enabled = canAfford,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonPink,
                    disabledContainerColor = ReelDivider,
                    contentColor = TextWhite,
                    disabledContentColor = TextGray
                )
            ) {
                Text(text = "Withdraw", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CollectionGatewayCard(hasPoints: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (hasPoints) NeonCyan.copy(alpha = 0.5f) else ReelDivider,
                RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (hasPoints) NeonCyan.copy(alpha = 0.08f) else ReelSurface
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (hasPoints) "✨ Points Waiting!" else "Daily Usage History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (hasPoints) NeonCyan else TextWhite
                )
                Text(
                    text = if (hasPoints) "Collect your screen time rewards now" else "View your daily progress",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray
                )
            }
            Icon(
                imageVector = Icons.Default.List,
                contentDescription = null,
                tint = if (hasPoints) NeonCyan else TextGray
            )
        }
    }
}

@Composable
fun RewardHistoryGatewayCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .border(1.dp, NeonPurple.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = NeonPurple.copy(alpha = 0.08f)
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = NeonPurple,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "My Rewards",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
            Text(
                text = "View your history",
                style = MaterialTheme.typography.bodySmall,
                color = TextGray
            )
        }
    }
}
