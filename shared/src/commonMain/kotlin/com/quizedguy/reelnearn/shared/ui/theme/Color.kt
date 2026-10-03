package com.quizedguy.reelnearn.shared.ui.theme

import androidx.compose.ui.graphics.Color

// ── TikTok / Reels Dark Palette ──────────────────────────────────────
// Backgrounds
val ReelBlack        = Color(0xFF000000) // Pure black background
val ReelSurface      = Color(0xFF111111) // Card surface
val ReelSurfaceHigh  = Color(0xFF1A1A1A) // Elevated card surface
val ReelDivider      = Color(0xFF2A2A2A) // Divider lines

// Neon Accents
val NeonPink         = Color(0xFFFF0080) // Primary neon pink (TikTok pink)
val NeonCyan         = Color(0xFF00F5FF) // Secondary neon cyan
val NeonPurple       = Color(0xFFBF00FF) // Tertiary / gradient end
val NeonGold         = Color(0xFFFFD700) // Points / coins

// Glow versions (semi-transparent for borders/glow effects)
val NeonPinkGlow     = Color(0x66FF0080) // 40% alpha neon pink
val NeonCyanGlow     = Color(0x4400F5FF) // 27% alpha neon cyan

// Text
val TextWhite        = Color(0xFFFFFFFF) // Primary text
val TextGray         = Color(0xFF888888) // Secondary / hint text
val TextDimmed       = Color(0xFF555555) // Disabled / placeholder

// Status
val SuccessGreen     = Color(0xFF00E676) // Success green
val AlertOrange      = Color(0xFFFF9800) // Warning orange
val ErrorRed         = Color(0xFFFF1744) // Error red

// Legacy aliases (keeps old references compiling)
val PrimaryIndigo    = NeonPink
val PrimaryBlue      = NeonCyan
val PrimaryTeal      = NeonCyan
val PrimaryRose      = NeonPink
val SecondaryViolet  = NeonPurple
val SecondarySky     = NeonCyan
val SecondaryEmerald = SuccessGreen
val SoftBackground   = ReelBlack
val DarkBackground   = ReelBlack
val CardSurface      = ReelSurface
val TextPrimary      = TextWhite
val TextSecondary    = TextGray
