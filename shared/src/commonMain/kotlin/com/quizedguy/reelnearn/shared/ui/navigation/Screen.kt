package com.quizedguy.reelnearn.shared.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Login : Screen("login", "Login", Icons.Default.Lock)
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Earn : Screen("earn", "Earn", Icons.Default.CheckCircle)
    object ScreenTime : Screen("screen_time", "Usage", Icons.Default.List)
    object Rewards : Screen("rewards", "Rewards", Icons.Default.Star)
    object Points : Screen("points", "Points", Icons.Default.CheckCircle)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
    object Admin : Screen("admin", "Admin", Icons.Default.Settings)
    object Collection : Screen("collection", "History", Icons.Default.List)
    object Referrals : Screen("referrals", "Refer", Icons.Default.Share)
    object MyRewardsHistory : Screen("my_rewards_history", "My Rewards", Icons.Default.Star)
    object SponsoredTasks : Screen("sponsored_tasks", "Tasks", Icons.Default.CheckCircle)
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Earn,
    Screen.Rewards,
    Screen.Profile
)


