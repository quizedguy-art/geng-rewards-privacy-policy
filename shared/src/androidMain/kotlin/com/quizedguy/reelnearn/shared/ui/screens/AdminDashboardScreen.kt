package com.quizedguy.reelnearn.shared.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.quizedguy.reelnearn.shared.ui.viewmodel.AdminViewModel
import com.quizedguy.reelnearn.shared.ui.viewmodel.AuthViewModel
import com.quizedguy.reelnearn.shared.ui.viewmodel.WithdrawalRequest
import com.quizedguy.reelnearn.shared.ui.viewmodel.SponsoredTask
import com.quizedguy.reelnearn.shared.ui.viewmodel.TaskCompletion
import com.quizedguy.reelnearn.shared.ui.components.BannerAdView
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.ads.MobileAds
import com.quizedguy.reelnearn.shared.ui.viewmodel.SupportTicket
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminDashboardScreen(
    navController: NavController,
    viewModel: AdminViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableStateOf(0) }
    
    val pendingWithdrawals by viewModel.pendingWithdrawals.collectAsState()
    val withdrawalHistory by viewModel.withdrawalHistory.collectAsState()
    val usageRecords by viewModel.usageRecords.collectAsState()
    val allTasks by viewModel.allTasks.collectAsState()
    val pendingTaskCompletions by viewModel.pendingTaskCompletions.collectAsState()
    val supportTickets by viewModel.supportTickets.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showApproveDialog by remember { mutableStateOf<WithdrawalRequest?>(null) }
    var giftCardCode by remember { mutableStateOf("") }
    
    var showCreditDialog by remember { mutableStateOf<com.quizedguy.reelnearn.shared.ui.viewmodel.DailyUsageRecord?>(null) }
    var pointsToCredit by remember { mutableStateOf("") }

    var showCreateTaskDialog by remember { mutableStateOf(false) }

    // Support ticket reply state
    var selectedTicketForReply by remember { mutableStateOf<com.quizedguy.reelnearn.shared.ui.viewmodel.SupportTicket?>(null) }
    var replyText by remember { mutableStateOf("") }
    var replyMarkResolved by remember { mutableStateOf(true) }
    var supportFilterStatus by remember { mutableStateOf("All") } // "All", "Open", "Resolved"

    val openTicketsCount = supportTickets.count { it.status.equals("Open", ignoreCase = true) }

    val filteredTickets = remember(supportTickets, supportFilterStatus) {
        when (supportFilterStatus) {
            "Open" -> supportTickets.filter { it.status.equals("Open", ignoreCase = true) }
            "Resolved" -> supportTickets.filter { it.status.equals("Resolved", ignoreCase = true) }
            else -> supportTickets
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp), 
            horizontalArrangement = Arrangement.SpaceBetween, 
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                text = "Admin Panel",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                val context = LocalContext.current
                IconButton(onClick = {
                    MobileAds.openAdInspector(context) { error ->
                        if (error != null) {
                            android.util.Log.e("AdminDashboard", "Ad Inspector failed to open: ${error.message}")
                        }
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Open Ad Inspector",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = { showLogoutDialog = true }) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Log Out", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
        
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            edgePadding = 12.dp
        ) {
            Tab(selected = selectedTabIndex == 0, onClick = { selectedTabIndex = 0 }, text = { Text("Requests (${pendingWithdrawals.size})") })
            Tab(selected = selectedTabIndex == 1, onClick = { selectedTabIndex = 1 }, text = { Text("Usage") })
            Tab(selected = selectedTabIndex == 2, onClick = { selectedTabIndex = 2 }, text = { Text("Tasks (${pendingTaskCompletions.size})") })
            Tab(selected = selectedTabIndex == 3, onClick = { selectedTabIndex = 3 }, text = { 
                Text("Support${if (openTicketsCount > 0) " ($openTicketsCount)" else ""}") 
            })
            Tab(selected = selectedTabIndex == 4, onClick = { selectedTabIndex = 4 }, text = { Text("History") })
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                
                // Monetization Summary Card
                item {
                    val vipUsersCount by viewModel.vipUsersCount.collectAsState()
                    val totalApprovedPayouts = withdrawalHistory.filter { it.status == "Approved" }.sumOf { it.amountRs }
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("📊 Monetization & Revenue Analytics", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Active VIP Members", style = MaterialTheme.typography.labelSmall)
                                    Text("👑 $vipUsersCount Users", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleSmall)
                                }
                                Column {
                                    Text("Total Paid Out", style = MaterialTheme.typography.labelSmall)
                                    Text("💵 $$totalApprovedPayouts", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }

                // Admin Email Verifier Tool
                item {
                    var adminVerifyEmailInput by remember { mutableStateOf("") }
                    var adminVerifyStatus by remember { mutableStateOf<String?>(null) }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("✉️ Admin Quick Email Verifier", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text("Manually verify any user's email to unblock support tickets immediately.", style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = adminVerifyEmailInput,
                                    onValueChange = { adminVerifyEmailInput = it },
                                    placeholder = { Text("e.g. user@gmail.com") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        viewModel.verifyUserEmailByAdmin(adminVerifyEmailInput) { success, msg ->
                                            adminVerifyStatus = msg
                                            if (success) adminVerifyEmailInput = ""
                                        }
                                    }
                                ) {
                                    Text("Verify")
                                }
                            }
                            adminVerifyStatus?.let { status ->
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = status,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (status.contains("Successfully", ignoreCase = true)) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                when (selectedTabIndex) {
                    0 -> { // Requests
                        if (pendingWithdrawals.isEmpty()) {
                            item { Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = androidx.compose.ui.Alignment.Center) { Text("No pending requests.") } }
                        } else {
                            items(pendingWithdrawals) { request ->
                                WithdrawalRequestCard(
                                    request = request,
                                    onApprove = { showApproveDialog = request },
                                    onReject = { viewModel.rejectRequest(request.id) }
                                )
                            }
                        }
                    }
                    1 -> { // Usage
                        if (usageRecords.isEmpty()) {
                            item { Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = androidx.compose.ui.Alignment.Center) { Text("No usage records.") } }
                        } else {
                            items(usageRecords) { record ->
                                UsageReviewCard(
                                    record = record,
                                    onCredit = { 
                                        showCreditDialog = record
                                        pointsToCredit = record.pointsPotential.toString()
                                    }
                                )
                            }
                        }
                    }
                    2 -> { // Sponsored Tasks & Completions
                        item {
                            Button(
                                onClick = { showCreateTaskDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Create New Sponsored Task")
                            }
                        }

                        item {
                            Spacer(Modifier.height(8.dp))
                            Text("Pending Submissions to Review", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }

                        if (pendingTaskCompletions.isEmpty()) {
                            item { Text("No pending task submissions from users.", color = Color.Gray) }
                        } else {
                            items(pendingTaskCompletions) { completion ->
                                TaskCompletionReviewCard(
                                    completion = completion,
                                    onApprove = { viewModel.approveTaskCompletion(completion) },
                                    onReject = { viewModel.rejectTaskCompletion(completion.id) }
                                )
                            }
                        }

                        item {
                            Spacer(Modifier.height(16.dp))
                            Text("All Created Tasks", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }

                        if (allTasks.isEmpty()) {
                            item { Text("No sponsored tasks created yet. Tap button above to create one!", color = Color.Gray) }
                        } else {
                            items(allTasks) { task ->
                                AdminTaskItemCard(
                                    task = task,
                                    onToggleActive = { viewModel.toggleTaskActive(task.id, task.isActive) }
                                )
                            }
                        }
                    }
                    3 -> { // Support Tickets
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = supportFilterStatus == "All",
                                    onClick = { supportFilterStatus = "All" },
                                    label = { Text("All (${supportTickets.size})") }
                                )
                                FilterChip(
                                    selected = supportFilterStatus == "Open",
                                    onClick = { supportFilterStatus = "Open" },
                                    label = { Text("Open ($openTicketsCount)") }
                                )
                                FilterChip(
                                    selected = supportFilterStatus == "Resolved",
                                    onClick = { supportFilterStatus = "Resolved" },
                                    label = { Text("Resolved (${supportTickets.count { it.status.equals("Resolved", ignoreCase = true) }})") }
                                )
                            }
                        }

                        if (filteredTickets.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                                    contentAlignment = androidx.compose.ui.Alignment.Center
                                ) {
                                    Text("No support tickets found.", color = Color.Gray)
                                }
                            }
                        } else {
                            items(filteredTickets) { ticket ->
                                SupportTicketAdminCard(
                                    ticket = ticket,
                                    onReply = {
                                        selectedTicketForReply = ticket
                                        replyText = ticket.adminReply ?: ""
                                    },
                                    onToggleResolve = {
                                        val newStatus = if (ticket.status.equals("Resolved", ignoreCase = true)) "Open" else "Resolved"
                                        viewModel.updateTicketStatus(ticket.id, newStatus)
                                    },
                                    onDelete = {
                                        viewModel.deleteSupportTicket(ticket.id)
                                    }
                                )
                            }
                        }
                    }
                    4 -> { // History
                        if (withdrawalHistory.isEmpty()) {
                            item { Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = androidx.compose.ui.Alignment.Center) { Text("No reward history.") } }
                        } else {
                            items(withdrawalHistory) { request ->
                                IssuedRewardCard(request) // Reusing from MyRewardsHistoryScreen
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    // Create Task Dialog
    if (showCreateTaskDialog) {
        CreateTaskDialog(
            onDismiss = { showCreateTaskDialog = false },
            onCreate = { title, desc, category, points, url, sponsor, minutes ->
                viewModel.createSponsoredTask(title, desc, category, points, url, sponsor, minutes)
                showCreateTaskDialog = false
            }
        )
    }

    // Approval Dialog
    if (showApproveDialog != null) {
        AlertDialog(
            onDismissRequest = { showApproveDialog = null },
            title = { Text("Issue Gift Card") },
            text = {
                Column {
                    Text("Enter the code for ${showApproveDialog?.formattedAmount ?: ""} ${showApproveDialog?.rewardName ?: "reward"}.")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = giftCardCode,
                        onValueChange = { giftCardCode = it },
                        label = { Text("Gift Card Code") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.approveRequest(showApproveDialog!!.id, giftCardCode)
                        showApproveDialog = null
                        giftCardCode = ""
                    },
                    enabled = giftCardCode.isNotBlank()
                ) {
                    Text("Approve & Send")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApproveDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Credit Points Dialog
    if (showCreditDialog != null) {
        AlertDialog(
            onDismissRequest = { showCreditDialog = null },
            title = { Text("Credit Points") },
            text = {
                Column {
                    Text("Enter points to credit for usage on ${showCreditDialog?.date}")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pointsToCredit,
                        onValueChange = { pointsToCredit = it },
                        label = { Text("Points") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val points = pointsToCredit.toIntOrNull()
                        if (points != null) {
                            viewModel.creditUsagePoints(showCreditDialog!!, points)
                            showCreditDialog = null
                            pointsToCredit = ""
                        }
                    },
                    enabled = pointsToCredit.isNotBlank() && pointsToCredit.toIntOrNull() != null
                ) {
                    Text("Credit User")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreditDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Support Ticket Reply Dialog
    if (selectedTicketForReply != null) {
        val ticket = selectedTicketForReply!!
        val context = LocalContext.current
        AlertDialog(
            onDismissRequest = { selectedTicketForReply = null },
            title = {
                Text("Reply to Support Ticket", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "User: ${ticket.userName.ifBlank { "User" }} (${ticket.userEmail})",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Category: ${ticket.category} • Balance: ${ticket.currentPoints} pts",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "\"${ticket.message}\"",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp),
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }

                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        label = { Text("Your Reply Message") },
                        placeholder = { Text("Write your response to the user...") },
                        modifier = Modifier.fillMaxWidth().height(130.dp),
                        maxLines = 6
                    )

                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = replyMarkResolved,
                            onCheckedChange = { replyMarkResolved = it }
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Mark ticket as Resolved", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.replyToSupportTicket(ticket, replyText, replyMarkResolved)
                        selectedTicketForReply = null
                        replyText = ""
                    },
                    enabled = replyText.isNotBlank()
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Send & Notify")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:${ticket.userEmail}")
                                putExtra(Intent.EXTRA_SUBJECT, "Reel n Earn Support: ${ticket.category}")
                                putExtra(Intent.EXTRA_TEXT, "Hi ${ticket.userName},\n\nRegarding your ticket:\n\"${ticket.message}\"\n\n")
                            }
                            try {
                                context.startActivity(Intent.createChooser(emailIntent, "Send Email via..."))
                            } catch (e: Exception) {
                                // Ignore if no email client
                            }
                        }
                    ) {
                        Text("Open Email Client")
                    }
                    Spacer(Modifier.width(4.dp))
                    TextButton(onClick = { selectedTicketForReply = null }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log Out?") },
            text = { Text("Are you sure you want to sign out of the Admin Dashboard?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        authViewModel.signOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Log Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CreateTaskDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String, Int, String, String, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Survey") }
    var points by remember { mutableStateOf("150") }
    var url by remember { mutableStateOf("") }
    var sponsor by remember { mutableStateOf("Attapoll") }
    var minutes by remember { mutableStateOf("5") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Sponsored Task", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Task Title (e.g. Attapoll Survey)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("Survey Link / URL") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = points, onValueChange = { points = it }, label = { Text("Reward Pts") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = minutes, onValueChange = { minutes = it }, label = { Text("Est Mins") }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category (Survey/App)") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = sponsor, onValueChange = { sponsor = it }, label = { Text("Sponsor Name") }, modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val pts = points.toIntOrNull() ?: 100
                    val mins = minutes.toIntOrNull() ?: 5
                    onCreate(title, description, category, pts, url, sponsor, mins)
                },
                enabled = title.isNotBlank() && url.isNotBlank()
            ) {
                Text("Create Task")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun TaskCompletionReviewCard(
    completion: TaskCompletion,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(completion.taskTitle, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text("User UID: ${completion.userId}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Text("Reward: +${completion.pointsReward} Points", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onReject, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("Reject")
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = onApprove) {
                    Text("Approve (+${completion.pointsReward} Pts)")
                }
            }
        }
    }
}

@Composable
fun AdminTaskItemCard(
    task: SponsoredTask,
    onToggleActive: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(task.title, fontWeight = FontWeight.Bold)
                Text("${task.category} • +${task.pointsReward} Pts • ${task.sponsorName}", style = MaterialTheme.typography.bodySmall)
            }
            Switch(
                checked = task.isActive,
                onCheckedChange = { onToggleActive() }
            )
        }
    }
}

@Composable
fun WithdrawalRequestCard(
    request: WithdrawalRequest,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    val rewardNameStr = if (request.rewardName.isNotEmpty()) request.rewardName else "Reward"
                    Text(text = "Amount: ${request.formattedAmount} ($rewardNameStr)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    Text(text = "Points: ${request.pointsDeducted}", style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    text = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(request.createdAt)),
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "User UID: ${request.userId}", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            if (!request.paymentDetails.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Payment Details (UPI/Info): ${request.paymentDetails}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onReject, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("Reject")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onApprove) {
                    Text("Issue Code")
                }
            }
        }
    }
}

@Composable
fun UsageReviewCard(
    record: com.quizedguy.reelnearn.shared.ui.viewmodel.DailyUsageRecord,
    onCredit: () -> Unit
) {
    val todayStr = remember { java.time.LocalDate.now().toString() }
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (record.isApproved) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = record.date, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = if (record.isCollected) "Collected" else if (record.isApproved) "Approved (Uncollected)" else "Pending Review",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (record.isCollected) Color(0xFF4CAF50) else if (record.isApproved) Color(0xFF2196F3) else MaterialTheme.colorScheme.secondary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            val hours = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(record.totalMillis)
            val minutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(record.totalMillis) % 60
            Text(text = "Screen Time: ${hours}h ${minutes}m", style = MaterialTheme.typography.bodyMedium)
            Text(text = "User UID: ${record.userId}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            
            if (!record.isApproved) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    if (record.date == todayStr) {
                        Text(
                            text = "Tracking in Progress",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFFF59E0B),
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Button(onClick = onCredit) {
                            Text("Credit Points")
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                val statusText = if (record.isCollected) "Collected: ${record.pointsPotential} pts" else "Approved: ${record.pointsPotential} pts"
                Text(text = statusText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun SupportTicketAdminCard(
    ticket: SupportTicket,
    onReply: () -> Unit,
    onToggleResolve: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val isResolved = ticket.status.equals("Resolved", ignoreCase = true)
    val isReplied = ticket.status.equals("Replied", ignoreCase = true)

    val categoryColor = when (ticket.category) {
        "Points / Rewards" -> Color(0xFFFFD700)
        "Ad Loading / Video Issue" -> Color(0xFFFF6B6B)
        "Account Verification" -> Color(0xFF4D96FF)
        "Withdrawal / Payment Issue" -> Color(0xFF6BCB77)
        else -> MaterialTheme.colorScheme.primary
    }

    val statusBg = when {
        isResolved -> Color(0xFF2E7D32)
        isReplied -> Color(0xFF1976D2)
        else -> Color(0xFFE65100)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: User Info & Status Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ticket.userName.ifBlank { "User" },
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = ticket.userEmail,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Surface(
                    color = statusBg,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = ticket.status,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Metadata Chips (Category, Device Model, Balance, Date)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Surface(
                    color = categoryColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, categoryColor.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = ticket.category,
                        color = categoryColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "🪙 ${ticket.currentPoints} pts",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            if (ticket.deviceModel.isNotBlank() || ticket.androidVersion.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "📱 ${ticket.deviceModel} (Android ${ticket.androidVersion})",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }

            if (ticket.createdAt > 0L) {
                Text(
                    text = "🕒 ${SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault()).format(Date(ticket.createdAt))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }

            Spacer(Modifier.height(12.dp))

            // User Message Box
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "USER MESSAGE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = ticket.message,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Admin Reply Section (if already replied)
            if (!ticket.adminReply.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = Color(0xFF00F5D4).copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, Color(0xFF00F5D4).copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "ADMIN REPLY",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00B4D8)
                            )
                            if (ticket.repliedAt != null && ticket.repliedAt > 0L) {
                                Text(
                                    text = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(ticket.repliedAt)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = ticket.adminReply,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Ticket",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:${ticket.userEmail}")
                                putExtra(Intent.EXTRA_SUBJECT, "Reel n Earn Support: ${ticket.category}")
                                putExtra(Intent.EXTRA_TEXT, "Hi ${ticket.userName},\n\nRegarding your ticket:\n\"${ticket.message}\"\n\n")
                            }
                            try {
                                context.startActivity(Intent.createChooser(emailIntent, "Send Email via..."))
                            } catch (e: Exception) {
                                // ignore
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Open Email",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onToggleResolve,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (isResolved) "Reopen" else "Resolve")
                    }

                    Button(
                        onClick = onReply,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Reply")
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Ticket?") },
            text = { Text("Are you sure you want to delete this support ticket permanently?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}



