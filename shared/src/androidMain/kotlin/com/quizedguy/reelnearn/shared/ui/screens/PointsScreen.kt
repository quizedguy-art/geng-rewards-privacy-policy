package com.quizedguy.reelnearn.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quizedguy.reelnearn.shared.ui.theme.*
import com.quizedguy.reelnearn.shared.ui.viewmodel.PointsViewModel
import com.quizedguy.reelnearn.shared.ui.viewmodel.WithdrawalRequest
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PointsScreen(viewModel: PointsViewModel = viewModel()) {
    val points by viewModel.userPoints.collectAsState()
    val history by viewModel.withdrawalHistory.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ReelBlack)
            .padding(16.dp)
    ) {
        item {
            Text(text = "My Points", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = TextWhite)
            Spacer(modifier = Modifier.height(20.dp))
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .border(1.dp, NeonPink.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ReelSurface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(NeonPink.copy(alpha = 0.1f), ReelSurface))),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "$points Points", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = NeonGold)
                        Spacer(Modifier.height(4.dp))
                        Text(text = "Watch Reels & Videos to earn daily points!", style = MaterialTheme.typography.bodyMedium, color = TextGray)
                    }
                }
            }
            Spacer(modifier = Modifier.height(28.dp))
        }

        item {
            Text(text = "Points History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextWhite)
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (history.isEmpty()) {
            item {
                Text(text = "No history yet. Watch reels to earn points!", color = TextGray)
            }
        } else {
            items(history) { request ->
                WithdrawalHistoryItem(request)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun WithdrawalHistoryItem(request: WithdrawalRequest) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ReelDivider, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ReelSurface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                val rewardNameStr = if (request.rewardName.isNotEmpty()) request.rewardName else "Reward"
                Text(text = "Withdrawal ${request.formattedAmount} ($rewardNameStr)", fontWeight = FontWeight.Bold, color = TextWhite)
                val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(request.createdAt))
                Text(text = dateStr, style = MaterialTheme.typography.bodySmall, color = TextGray)
            }
            Text(
                text = request.status,
                color = when (request.status) {
                    "Pending" -> AlertOrange
                    "Approved" -> SuccessGreen
                    else -> ErrorRed
                },
                fontWeight = FontWeight.Bold
            )
        }
    }
}


