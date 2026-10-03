package com.quizedguy.reelnearn.shared.ui.screens

import android.app.usage.UsageStats
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quizedguy.reelnearn.shared.data.UsageStatsHelper
import java.util.concurrent.TimeUnit

@Composable
fun ScreenTimeScreen() {
    val context = LocalContext.current
    var totalTime by remember { mutableStateOf(0L) }
    
    LaunchedEffect(Unit) {
        totalTime = UsageStatsHelper.getTodayTotalScreenTime(context)
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text(text = "Usage Details", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Total Screen Time Today", style = MaterialTheme.typography.titleLarge)
                    Text(text = formatDuration(totalTime), fontSize = 32.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Text(text = "Tips to Reduce Screen Time", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            TipItem("📵 Put phone face down during work")
            TipItem("🌳 Go for a 15-min walk without your phone")
            TipItem("💤 No screens 1 hour before bedtime")
        }
    }
}

@Composable
fun TipItem(tip: String) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(text = tip, modifier = Modifier.padding(12.dp))
    }
}

private fun formatDuration(millis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    return "${hours}h ${minutes}m"
}


