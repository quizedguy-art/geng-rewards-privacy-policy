package com.quizedguy.reelnearn.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
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
import androidx.navigation.NavController
import com.quizedguy.reelnearn.shared.ui.theme.*
import com.quizedguy.reelnearn.shared.ui.viewmodel.PointsViewModel
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyRewardsHistoryScreen(
    navController: NavController,
    pointsViewModel: PointsViewModel = viewModel()
) {
    val withdrawalHistory by pointsViewModel.withdrawalHistory.collectAsState()

    Scaffold(
        containerColor = ReelBlack,
        topBar = {
            TopAppBar(
                title = { Text("My Rewards", fontWeight = FontWeight.Bold, color = TextWhite) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ReelBlack)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(ReelBlack)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "History & Codes",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = NeonPink
                    )
                    Text(
                        text = "All your claimed payouts & gift cards",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextGray
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }

                if (withdrawalHistory.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = ReelDivider
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No rewards requested yet.", 
                                    color = TextGray,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                } else {
                    items(withdrawalHistory) { request ->
                        IssuedRewardCard(request)
                    }
                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun IssuedRewardCard(request: com.quizedguy.reelnearn.shared.ui.viewmodel.WithdrawalRequest) {
    val isApproved = request.status == "Approved"
    
    val statusColor = if (isApproved) SuccessGreen else AlertOrange
    val borderColor = if (isApproved) SuccessGreen.copy(alpha = 0.4f) else AlertOrange.copy(alpha = 0.4f)
    val rewardDisplayName = if (request.rewardName.isNotEmpty()) request.rewardName else "Reward"
    val dateStr = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(request.createdAt))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ReelSurface)
    ) {
        Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(statusColor.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                            .border(1.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isApproved) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "${request.formattedAmount} $rewardDisplayName", 
                            fontWeight = FontWeight.ExtraBold, 
                            fontSize = 17.sp,
                            color = TextWhite
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = dateStr, 
                            style = MaterialTheme.typography.bodySmall, 
                            color = TextGray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isApproved && !request.giftCardCode.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ReelSurfaceHigh, RoundedCornerShape(14.dp))
                        .border(1.dp, SuccessGreen.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                        .padding(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "YOUR REWARD CODE / DETAILS", 
                            style = MaterialTheme.typography.labelSmall, 
                            color = TextGray,
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = request.giftCardCode,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = SuccessGreen,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        val clipboardManager = LocalClipboardManager.current
                        val context = LocalContext.current
                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(request.giftCardCode))
                                Toast.makeText(context, "Code copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Copy Code", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (request.status == "Pending") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AlertOrange.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                        .border(1.dp, AlertOrange.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = AlertOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Under review by admin. Payment will be processed within 24-48 hours.", 
                        style = MaterialTheme.typography.bodySmall, 
                        color = TextWhite,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}


