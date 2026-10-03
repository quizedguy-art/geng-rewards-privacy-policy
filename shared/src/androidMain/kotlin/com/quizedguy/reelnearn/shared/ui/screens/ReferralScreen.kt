package com.quizedguy.reelnearn.shared.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.quizedguy.reelnearn.shared.ui.theme.*
import com.quizedguy.reelnearn.shared.ui.viewmodel.AuthViewModel
import com.quizedguy.reelnearn.shared.ui.viewmodel.ReferralViewModel
import java.text.SimpleDateFormat
import java.util.*

import com.quizedguy.reelnearn.shared.ui.components.BannerAdView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferralScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    referralViewModel: ReferralViewModel = viewModel()
) {
    val referralCode by authViewModel.userReferralCode.collectAsState()
    val referredBy by authViewModel.referredBy.collectAsState()
    val isAuthLoading by authViewModel.isLoading.collectAsState()
    val referrals by referralViewModel.referrals.collectAsState()
    val isLoading by referralViewModel.isLoading.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var inputCode by remember { mutableStateOf("") }
    var claimMessage by remember { mutableStateOf<String?>(null) }
    var isSuccessMessage by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = ReelBlack,
        topBar = {
            TopAppBar(
                title = { Text("My Referrals", fontWeight = FontWeight.Bold, color = TextWhite, modifier = Modifier.offset(y = (-6).dp)) },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.offset(y = (-6).dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ReelBlack
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ReelBlack)
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // Claim Referral Code Card (Only shown if user hasn't claimed a referral bonus yet)
                if (referredBy == null) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = ReelSurface)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Have a Referral Code? 🎁",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Enter a friend's code to get 250 bonus points instantly!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextGray,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = inputCode,
                                        onValueChange = { inputCode = it.uppercase() },
                                        placeholder = { Text("ENTER CODE", color = TextDimmed) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true,
                                        enabled = !isAuthLoading,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = NeonPink,
                                            unfocusedBorderColor = ReelDivider,
                                            focusedTextColor = TextWhite,
                                            unfocusedTextColor = TextWhite
                                        )
                                    )
                                    Button(
                                        onClick = {
                                            authViewModel.claimReferralCode(context, inputCode) { success, msg ->
                                                claimMessage = msg
                                                isSuccessMessage = success
                                                if (success) {
                                                    inputCode = ""
                                                    referralViewModel.loadReferrals()
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonPink),
                                        enabled = inputCode.isNotBlank() && !isAuthLoading
                                    ) {
                                        if (isAuthLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                color = Color.White
                                            )
                                        } else {
                                            Text("Claim", color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                claimMessage?.let { msg ->
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = msg,
                                        color = if (isSuccessMessage) SuccessGreen else ErrorRed,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                // Header Card with Code
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .border(1.dp, NeonPink.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = ReelSurface)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Refer Friends & Earn 🎁",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextWhite,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Both get 250 points when they sign up using your code.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextGray,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Surface(
                                color = ReelSurfaceHigh,
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NeonPink.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = referralCode ?: "......",
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 4.sp,
                                    color = NeonPink
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            val currentRefCode = referralCode ?: ""
                            val currentRefLink = if (currentRefCode.isNotBlank()) "https://geng-money.web.app/?ref=$currentRefCode" else "https://geng-money.web.app"

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        if (currentRefCode.isNotBlank()) {
                                            clipboardManager.setText(AnnotatedString(currentRefCode))
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonPink)
                                ) {
                                    Text("Copy Code", color = Color.White, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(currentRefLink))
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan)
                                ) {
                                    Text("Copy Link", fontWeight = FontWeight.Bold)
                                }
                                
                                IconButton(
                                    onClick = {
                                        if (currentRefCode.isNotBlank()) {
                                            val sendIntent: Intent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                val shareMessage = """
                                                    🎁 Join me on Reel n Earn & get 250 Free Points!
                                                    
                                                    Watch reels, track screen time & earn instant UPI cash & gift cards.
                                                    
                                                    👉 Download & claim your bonus:
                                                    $currentRefLink
                                                    
                                                    Invite Code: $currentRefCode
                                                """.trimIndent()
                                                putExtra(Intent.EXTRA_TEXT, shareMessage)
                                                type = "text/plain"
                                            }
                                            context.startActivity(Intent.createChooser(sendIntent, "Share Reel n Earn Invite"))
                                        }
                                    },
                                    modifier = Modifier
                                        .background(NeonCyan.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                        .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Share", tint = NeonCyan)
                                }
                            }
                        }
                    }
                }

                // Stats row
                item {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f).border(1.dp, ReelDivider, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = ReelSurface)
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Text("Total Refers", style = MaterialTheme.typography.labelSmall, color = TextGray)
                                Spacer(Modifier.height(4.dp))
                                Text("${referrals.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = TextWhite)
                            }
                        }
                        Card(
                            modifier = Modifier.weight(1f).border(1.dp, NeonGold.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = ReelSurface)
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Text("Points Earned", style = MaterialTheme.typography.labelSmall, color = TextGray)
                                Spacer(Modifier.height(4.dp))
                                Text("${referrals.sumOf { it.pointsAwarded }}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = NeonGold)
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Referral History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                if (isLoading) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = NeonPink)
                        }
                    }
                } else if (referrals.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No referrals yet. Start sharing!", color = TextGray)
                        }
                    }
                } else {
                    items(referrals) { record ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            ReferralItem(record)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReferralItem(record: com.quizedguy.reelnearn.shared.ui.viewmodel.ReferralRecord) {
    val date = remember(record.timestamp) {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        sdf.format(Date(record.timestamp))
    }
    
    val maskedEmail = remember(record.referredEmail) {
        val parts = record.referredEmail.split("@")
        if (parts.size == 2) {
            val name = parts[0]
            val domain = parts[1]
            if (name.length > 2) {
                name.take(2) + "***@" + domain
            } else {
                "***@" + domain
            }
        } else {
            record.referredEmail
        }
    }

    Surface(
        color = ReelSurface,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ReelDivider)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = record.referredName, fontWeight = FontWeight.Bold, color = TextWhite)
                Text(text = maskedEmail, style = MaterialTheme.typography.bodySmall, color = TextGray)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "+${record.pointsAwarded} Pts", color = NeonPink, fontWeight = FontWeight.Bold)
                Text(text = date, style = MaterialTheme.typography.labelSmall, color = TextGray)
            }
        }
    }
}


