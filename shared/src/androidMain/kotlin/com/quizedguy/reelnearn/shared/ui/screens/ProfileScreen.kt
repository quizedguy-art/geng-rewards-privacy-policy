package com.quizedguy.reelnearn.shared.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.quizedguy.reelnearn.shared.ui.navigation.Screen
import com.quizedguy.reelnearn.shared.ui.viewmodel.AuthViewModel
import com.quizedguy.reelnearn.shared.ui.viewmodel.PointsViewModel
import androidx.compose.ui.platform.LocalContext
import com.quizedguy.reelnearn.shared.ui.theme.*

@Composable
fun ProfileScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel(),
    pointsViewModel: PointsViewModel = viewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val userName by authViewModel.userName.collectAsState()
    val totalEarned by pointsViewModel.totalPointsEarned.collectAsState()
    val currentBalance by pointsViewModel.userPoints.collectAsState()

    var isPrivacyExpanded by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showPasswordResetDialog by remember { mutableStateOf(false) }
    var resetStatusMessage by remember { mutableStateOf<String?>(null) }
    var isResetSuccess by remember { mutableStateOf(false) }

    var showSupportDialog by remember { mutableStateOf(false) }
    var supportCategory by remember { mutableStateOf("Points / Tracking") }
    var supportMessage by remember { mutableStateOf("") }
    var supportStatusMsg by remember { mutableStateOf<String?>(null) }
    var isSubmittingSupport by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val gradient = Brush.horizontalGradient(listOf(NeonPink, NeonPurple))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ReelBlack)
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Creative Header
            item {
                HeaderSection(name = userName ?: "User", email = currentUser?.email ?: "", gradient = gradient)
            }

            // Stats Section
            item {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatCard(
                        title = "Lifetime Earned",
                        value = "$totalEarned",
                        icon = Icons.Default.Star,
                        modifier = Modifier.weight(1f),
                        color = NeonPink
                    )
                    StatCard(
                        title = "Current Balance",
                        value = "$currentBalance",
                        icon = Icons.Default.CheckCircle,
                        modifier = Modifier.weight(1f),
                        color = NeonCyan
                    )
                }
            }

            // Action Menu
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = "Account Actions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    
                    ActionItem(
                        title = "Transaction History",
                        subtitle = "View all your past redemptions",
                        icon = Icons.Default.List,
                        onClick = { navController.navigate(Screen.Points.route) }
                    )

                    ActionItem(
                        title = "My Referrals",
                        subtitle = "Refer friends & earn 250 points",
                        icon = Icons.Default.Face,
                        onClick = { navController.navigate(Screen.Referrals.route) }
                    )

                    ActionItem(
                        title = "Reset Password 🔑",
                        subtitle = "Send password reset link to your email",
                        icon = Icons.Default.Lock,
                        onClick = {
                            resetStatusMessage = null
                            showPasswordResetDialog = true
                        }
                    )
                    
                    ActionItem(
                        title = "Privacy Policy",
                        subtitle = "How we protect your data",
                        icon = Icons.Default.Info,
                        onClick = { isPrivacyExpanded = !isPrivacyExpanded },
                        trailingIcon = if (isPrivacyExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown
                    )

                    AnimatedVisibility(visible = isPrivacyExpanded) {
                        PrivacyPolicySection()
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    ActionItem(
                        title = "Contact Support 💬",
                        subtitle = "Open a support ticket or contact our help team",
                        icon = Icons.Default.Email,
                        onClick = {
                            supportStatusMsg = null
                            supportMessage = ""
                            showSupportDialog = true
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Logout button with neon gradient border
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .border(1.dp, ErrorRed.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                            .background(ErrorRed.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                            .clickable { showLogoutDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.ExitToApp, contentDescription = null, tint = ErrorRed)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Log Out", fontWeight = FontWeight.Bold, color = ErrorRed)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = ReelSurfaceHigh,
            titleContentColor = TextWhite,
            textContentColor = TextGray,
            title = { Text("Log Out?", color = TextWhite) },
            text = { Text("Are you sure you want to sign out of Reel n Earn?", color = TextGray) },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        authViewModel.signOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Log Out", color = TextWhite, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = NeonCyan)
                }
            }
        )
    }

    if (showPasswordResetDialog) {
        val userEmail = currentUser?.email ?: ""
        AlertDialog(
            onDismissRequest = {
                showPasswordResetDialog = false
                resetStatusMessage = null
            },
            containerColor = ReelSurfaceHigh,
            titleContentColor = TextWhite,
            textContentColor = TextGray,
            title = { Text("Reset Password 🔑", fontWeight = FontWeight.Bold, color = TextWhite) },
            text = {
                Column {
                    Text("We will send a password reset link to:", color = TextGray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = userEmail, fontWeight = FontWeight.Bold, color = NeonCyan)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Please check your inbox (and Spam folder) after tapping send.", style = MaterialTheme.typography.bodySmall, color = TextGray)

                    resetStatusMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = msg,
                            color = if (isResetSuccess) SuccessGreen else ErrorRed,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        authViewModel.sendPasswordResetForCurrentUser { success, message ->
                            isResetSuccess = success
                            resetStatusMessage = message
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Send Reset Link", color = TextWhite, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPasswordResetDialog = false
                    resetStatusMessage = null
                }) {
                    Text("Close", color = NeonCyan)
                }
            }
        )
    }

    if (showSupportDialog) {
        val userEmail = currentUser?.email ?: ""
        val userId = currentUser?.uid ?: ""
        val categories = listOf("Points / Tracking", "Withdrawal / Payout", "Referral Bonus", "App Bug", "Other")

        AlertDialog(
            onDismissRequest = {
                if (!isSubmittingSupport) {
                    showSupportDialog = false
                    supportStatusMsg = null
                }
            },
            containerColor = ReelSurfaceHigh,
            titleContentColor = TextWhite,
            textContentColor = TextGray,
            title = { Text("Contact Support 💬", fontWeight = FontWeight.Bold, color = TextWhite) },
            text = {
                Column {
                    Text("Choose a category and describe your question or issue below:", style = MaterialTheme.typography.bodySmall, color = TextGray)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Category chips
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categories.size) { index ->
                            val cat = categories[index]
                            val isSelected = cat == supportCategory
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) NeonPink else ReelSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) NeonPink else ReelDivider),
                                modifier = Modifier.clickable { supportCategory = cat }
                            ) {
                                Text(
                                    text = cat,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) Color.White else TextGray,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = supportMessage,
                        onValueChange = { supportMessage = it },
                        placeholder = { Text("Please provide details so we can help quickly...", color = TextDimmed, style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonPink,
                            unfocusedBorderColor = ReelDivider,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedContainerColor = ReelSurface,
                            unfocusedContainerColor = ReelSurface
                        )
                    )

                    supportStatusMsg?.let { msg ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = msg,
                            color = if (msg.contains("successfully", ignoreCase = true)) SuccessGreen else ErrorRed,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Direct email option with prefilled diagnostics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TextButton(
                            onClick = {
                                val subject = "[Support - $supportCategory] User $userId"
                                val body = "--- Diagnostics ---\nUser ID: $userId\nEmail: $userEmail\nDevice: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\nAndroid OS: ${android.os.Build.VERSION.RELEASE}\nPoints: $currentBalance\n--------------------\n\nMessage:\n$supportMessage"
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:reelnearn@gmail.com?subject=" + Uri.encode(subject) + "&body=" + Uri.encode(body))
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {}
                            }
                        ) {
                            Text("✉️ Or open in Email App with Diagnostics", fontSize = 12.sp, color = NeonCyan)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (supportMessage.isBlank()) {
                            supportStatusMsg = "Please enter your message before submitting."
                            return@Button
                        }
                        isSubmittingSupport = true
                        supportStatusMsg = null

                        val ticketData = hashMapOf(
                            "userId" to userId,
                            "userEmail" to userEmail,
                            "userName" to (userName ?: "User"),
                            "category" to supportCategory,
                            "message" to supportMessage.trim(),
                            "deviceModel" to "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",
                            "androidVersion" to android.os.Build.VERSION.RELEASE,
                            "currentPoints" to currentBalance,
                            "status" to "Open",
                            "createdAt" to System.currentTimeMillis()
                        )

                        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("support_tickets")
                            .add(ticketData)
                            .addOnSuccessListener {
                                isSubmittingSupport = false
                                supportStatusMsg = "Ticket submitted successfully! We will get back to you soon."
                                supportMessage = ""
                            }
                            .addOnFailureListener { e ->
                                isSubmittingSupport = false
                                supportStatusMsg = "Failed to submit ticket: ${e.localizedMessage}"
                            }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isSubmittingSupport && supportMessage.isNotBlank()
                ) {
                    if (isSubmittingSupport) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Submit Ticket", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSupportDialog = false
                    supportStatusMsg = null
                }) {
                    Text("Close", color = TextGray)
                }
            }
        )
    }
}

@Composable
fun HeaderSection(name: String, email: String, gradient: Brush) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(gradient)
            .padding(24.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        // Overlay for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ReelBlack.copy(alpha = 0.3f))
        )
        Column {
            Text(
                text = name,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = TextWhite
            )
            Text(
                text = email,
                style = MaterialTheme.typography.bodyMedium,
                color = TextWhite.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun StatCard(title: String, value: String, icon: ImageVector, modifier: Modifier, color: Color) {
    Card(
        modifier = modifier
            .height(110.dp)
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = color)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = TextWhite)
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = TextGray)
        }
    }
}

@Composable
fun ActionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    trailingIcon: ImageVector = Icons.Default.KeyboardArrowRight
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = ReelSurface
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(NeonPink.copy(alpha = 0.12f), CircleShape)
                    .border(1.dp, NeonPink.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = NeonPink)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, color = TextWhite)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = TextGray)
            }
            Icon(trailingIcon, contentDescription = null, tint = TextGray)
        }
    }
}

@Composable
fun PrivacyPolicySection() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .border(1.dp, ReelDivider, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ReelSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            PrivacyItem(
                title = "Usage Tracking",
                content = "We track your screen time duration locally. Only points and redemptions are synced to our servers."
            )
            PrivacyItem(
                title = "Data Security",
                content = "We never share your data. Your name and email are used only for account identification and reward issuance."
            )
            PrivacyItem(
                title = "Permissions",
                content = "Usage Access is strictly for calculations. Notification Access is for threshold alerts."
            )
        }
    }
}

@Composable
fun PrivacyItem(title: String, content: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge, color = NeonCyan)
        Text(text = content, style = MaterialTheme.typography.bodySmall, color = TextGray)
    }
}
