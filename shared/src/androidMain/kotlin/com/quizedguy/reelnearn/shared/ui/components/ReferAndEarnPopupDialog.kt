package com.quizedguy.reelnearn.shared.ui.components

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.quizedguy.reelnearn.shared.ui.theme.*

object ReferralPopupManager {
    private const val PREFS_NAME = "referral_popup_prefs"
    private const val KEY_LAST_SHOWN = "last_shown_timestamp"
    private const val KEY_SESSIONS_COUNT = "app_sessions_count"
    private const val COOLDOWN_HOURS_MILLIS = 24 * 60 * 60 * 1000L // 24 hours

    fun shouldShow(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastShown = prefs.getLong(KEY_LAST_SHOWN, 0L)
        val now = System.currentTimeMillis()

        // Only show at most once every 24 hours
        return (now - lastShown) >= COOLDOWN_HOURS_MILLIS
    }

    fun recordShown(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_SHOWN, System.currentTimeMillis()).apply()
    }
}

@Composable
fun ReferAndEarnPopupDialog(
    referralCode: String,
    onDismiss: () -> Unit,
    onNavigateToReferrals: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val codeToShare = if (referralCode.isNotBlank()) referralCode else "EARN500"
    val referralLink = "https://geng-money.web.app/?ref=$codeToShare"

    fun shareReferral() {
        val shareText = """
            🎁 Join me on Reel n Earn & get 250 Free Points!
            
            Earn real cash, UPI rewards & gift cards simply by watching Reels & videos 🎬
            
            👉 Download app: $referralLink
            🔑 Use my referral code: $codeToShare
        """.trimIndent()

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Reel n Earn Referral Code")
        context.startActivity(shareIntent)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF1E142B),
                            Color(0xFF121422),
                            Color(0xFF090A10)
                        )
                    )
                )
                .border(
                    1.2.dp,
                    Brush.horizontalGradient(listOf(NeonPink, NeonGold, NeonCyan)),
                    RoundedCornerShape(26.dp)
                )
                .padding(22.dp)
        ) {
            // Close 'X' Button in Top Right
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = TextGray,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    NeonPink.copy(alpha = 0.2f),
                                    NeonGold.copy(alpha = 0.2f)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            NeonGold.copy(alpha = 0.5f),
                            RoundedCornerShape(50.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "🎁 REFER & EARN BONUS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NeonGold,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Title & Subtitle
                Text(
                    text = "Invite Friends,\nEarn Unlimited Cash! 💸",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = TextWhite,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Share Reel n Earn with your friends and earn rewards every time they watch reels.",
                    fontSize = 13.sp,
                    color = TextGray,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Perks List Box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PerkRow(icon = "⚡", title = "Instant +500 PTS", desc = "When your friend signs up & joins")
                    PerkRow(icon = "💎", title = "Friend Gets +250 PTS", desc = "Free welcome bonus for them")
                    PerkRow(icon = "🔄", title = "10% Lifetime Payouts", desc = "Earn points on all their watch milestones")
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Referral Code Box with Copy Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black.copy(alpha = 0.4f))
                        .border(1.dp, NeonPink.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .clickable {
                            clipboardManager.setText(AnnotatedString(codeToShare))
                            Toast.makeText(context, "Referral code copied: $codeToShare", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "YOUR REFERRAL CODE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextGray,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = codeToShare,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = NeonPink,
                            letterSpacing = 2.sp
                        )
                    }

                    Surface(
                        color = NeonPink.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonPink.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "COPY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonPink,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Primary Share Button
                Button(
                    onClick = {
                        shareReferral()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPink)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Share with Friends 🚀",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Secondary Action: View Referrals
                TextButton(
                    onClick = {
                        onDismiss()
                        onNavigateToReferrals()
                    }
                ) {
                    Text(
                        text = "View My Referrals History ➔",
                        fontSize = 12.sp,
                        color = NeonCyan,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun PerkRow(icon: String, title: String, desc: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = icon, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
            Text(
                text = desc,
                fontSize = 11.sp,
                color = TextGray
            )
        }
    }
}
