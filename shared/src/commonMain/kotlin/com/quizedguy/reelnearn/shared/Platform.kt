package com.quizedguy.reelnearn.shared

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform


