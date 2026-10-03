package com.quizedguy.reelnearn.shared.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ── Always-dark TikTok/Reels color scheme ────────────────────────────
private val ReelColorScheme = darkColorScheme(
    primary            = NeonPink,
    onPrimary          = Color.White,
    secondary          = NeonCyan,
    onSecondary        = Color.Black,
    tertiary           = NeonPurple,
    onTertiary         = Color.White,
    background         = ReelBlack,
    onBackground       = TextWhite,
    surface            = ReelSurface,
    onSurface          = TextWhite,
    surfaceVariant     = ReelSurfaceHigh,
    onSurfaceVariant   = TextGray,
    error              = ErrorRed,
    onError            = Color.White,
    outline            = ReelDivider,
    secondaryContainer = ReelSurfaceHigh,
    onSecondaryContainer = TextWhite,
    primaryContainer   = Color(0xFF330020),  // dark pink tint for containers
    onPrimaryContainer = NeonPink,
)

@Composable
fun ReelNEarnTheme(
    darkTheme: Boolean = true,   // always dark
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor     = ReelBlack.toArgb()
            window.navigationBarColor = ReelBlack.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars     = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = ReelColorScheme,
        typography  = Typography,
        content     = content
    )
}

/** Legacy alias — keeps GenGhealth references compiling */
@Composable
fun Geng_healthTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = ReelNEarnTheme(darkTheme, dynamicColor, content)
