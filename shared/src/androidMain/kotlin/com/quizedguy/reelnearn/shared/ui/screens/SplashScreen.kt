package com.quizedguy.reelnearn.shared.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quizedguy.reelnearn.shared.R
import com.quizedguy.reelnearn.shared.ui.theme.NeonCyan
import com.quizedguy.reelnearn.shared.ui.theme.NeonPink
import com.quizedguy.reelnearn.shared.ui.theme.NeonPurple
import com.quizedguy.reelnearn.shared.ui.theme.ReelBlack
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
    onTimeout: () -> Unit
) {
    // Logo pulse glow animation
    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val pulse by pulseAnim.animateFloat(
        initialValue = 0.9f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "pulseScale"
    )
    val glowAlpha by pulseAnim.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "glowAlpha"
    )

    // Fade-in on entry
    val alpha = remember { Animatable(0f) }
    val translateY = remember { Animatable(40f) }
    LaunchedEffect(Unit) {
        launch { alpha.animateTo(1f, tween(700)) }
        launch { translateY.animateTo(0f, tween(700, easing = FastOutSlowInEasing)) }
        delay(2800)
        onTimeout()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ReelBlack),
        contentAlignment = Alignment.Center
    ) {
        // Background glow blob
        Box(
            modifier = Modifier
                .size(280.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            NeonPink.copy(alpha = 0.18f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .graphicsLayer(
                    alpha = alpha.value,
                    translationY = translateY.value
                )
        ) {
            // Glowing logo ring
            Box(contentAlignment = Alignment.Center) {
                // Outer glow ring
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(pulse)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    NeonPink.copy(alpha = glowAlpha * 0.5f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "Reel n Earn Logo",
                    modifier = Modifier.size(120.dp).scale(pulse)
                )
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text = "Reel n Earn",
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Watch. Earn. Repeat.",
                fontSize = 15.sp,
                color = NeonCyan,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(56.dp))

            // Neon loading dots
            NeonLoadingDots()
        }
    }
}

@Composable
private fun NeonLoadingDots() {
    val colors = listOf(NeonPink, NeonPurple, NeonCyan)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        colors.forEachIndexed { index, color ->
            val inf = rememberInfiniteTransition(label = "dot$index")
            val dotScale by inf.animateFloat(
                initialValue = 0.7f, targetValue = 1.3f,
                animationSpec = infiniteRepeatable(
                    animation = tween(500, delayMillis = index * 160, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ), label = "dot${index}scale"
            )
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .scale(dotScale)
                    .background(color, CircleShape)
            )
        }
    }
}


