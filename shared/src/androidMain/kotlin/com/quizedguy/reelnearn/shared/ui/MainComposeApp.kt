package com.quizedguy.reelnearn.shared.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.quizedguy.reelnearn.shared.ui.components.BannerAdView
import com.quizedguy.reelnearn.shared.ui.navigation.Screen
import com.quizedguy.reelnearn.shared.ui.navigation.bottomNavItems
import com.quizedguy.reelnearn.shared.ui.screens.*
import com.quizedguy.reelnearn.shared.ui.viewmodel.AuthViewModel
import com.quizedguy.reelnearn.shared.util.CompatibilityUtils
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.text.font.FontWeight
import com.quizedguy.reelnearn.shared.util.RewardedAdManager
import com.quizedguy.reelnearn.shared.util.RewardedInterstitialAdManager
import com.quizedguy.reelnearn.shared.util.AgeSignalsHelper
import android.app.Activity
import android.Manifest
import android.content.Intent
import android.widget.Toast
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.quizedguy.reelnearn.shared.ui.viewmodel.DashboardViewModel
import com.quizedguy.reelnearn.shared.ui.viewmodel.PointsViewModel
import com.quizedguy.reelnearn.shared.util.SurveyManager
import com.quizedguy.reelnearn.shared.ui.theme.*
import androidx.compose.ui.graphics.Color

@Composable
fun MainComposeApp(
    deepLinkRoute: String? = null,
    onDeepLinkConsumed: (() -> Unit)? = null
) {
    val authViewModel: AuthViewModel = viewModel()
    val currentUser by authViewModel.currentUser.collectAsState()
    val isEmailVerified by authViewModel.isEmailVerified.collectAsState()
    val isAdmin by authViewModel.isAdmin.collectAsState()
    val context = LocalContext.current
    
    val isAdLoaded by RewardedAdManager.isAdLoaded.collectAsState()
    val isMinor by AgeSignalsHelper.isMinor.collectAsState()
    
    var showCompatibilityAlert by remember { 
        mutableStateOf(!CompatibilityUtils.isGooglePlayServicesAvailable(context)) 
    }
    var showSplashScreen by remember { mutableStateOf(true) }

    val dashboardViewModel: DashboardViewModel = viewModel()
    val hasUsagePermission by dashboardViewModel.hasPermission.collectAsState()
    val userReferralCode by authViewModel.userReferralCode.collectAsState()
    
    var showReferralPopup by remember { mutableStateOf(false) }
    
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (!isGranted) {
            try {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                } catch (_: Exception) {}
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                dashboardViewModel.checkPermission()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    hasNotificationPermission = ContextCompat.checkSelfPermission(
                        context, 
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                } else {
                    hasNotificationPermission = androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
                }
            } else if (event == Lifecycle.Event.ON_PAUSE) {
                dashboardViewModel.stopTracking()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            authViewModel.checkUserRole(context)
            dashboardViewModel.checkPermission()
            com.quizedguy.reelnearn.shared.util.NotificationHelper.createNotificationChannels(context)
            com.quizedguy.reelnearn.shared.worker.WorkScheduler.schedulePeriodicWorkers(context)
            // Initialize BitLabs survey SDK for the logged-in user
            SurveyManager.initialize()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            // User logged out — reset survey manager
            SurveyManager.reset()
        }
    }
    
    if (showCompatibilityAlert) {
        AlertDialog(
            onDismissRequest = { showCompatibilityAlert = false },
            title = { Text("Device Compatibility") },
            text = { 
                Text("Reel n Earn noticed that Google Play Services is missing. Features like Real-time Sync and Ads may be limited on this device.") 
            },
            confirmButton = {
                Button(onClick = { showCompatibilityAlert = false }) {
                    Text("I Understand")
                }
            }
        )
    }



    if (showSplashScreen) {
        SplashScreen(
            onTimeout = {
                val app = context.applicationContext
                val activity = context as? Activity
                if (app != null && activity != null && !isMinor) {
                    val manager = try {
                        val method = app.javaClass.getMethod("getAppOpenAdManager")
                        method.invoke(app) as? com.quizedguy.reelnearn.shared.util.AppOpenAdManager
                    } catch (e: Exception) { null }

                    if (manager != null) {
                        manager.showAdIfAvailable(activity) {
                            showSplashScreen = false
                        }
                    } else {
                        showSplashScreen = false
                    }
                } else {
                    showSplashScreen = false
                }
            }
        )
    } else if (currentUser == null || !isEmailVerified) {
        LoginScreen(
            authViewModel = authViewModel,
            onLoginSuccess = { 
                // AuthViewModel.currentUser and isEmailVerified will update and swap the UI
            }
        )
    } else if (!hasUsagePermission || !hasNotificationPermission) {
        PermissionRequiredScreen(
            hasUsage = hasUsagePermission,
            hasNotification = hasNotificationPermission,
            onRequestUsage = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
            onRequestNotification = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    try {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        try {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = android.net.Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    }
                }
            }
        )
    } else {
        val navController = rememberNavController()
        val pointsViewModel: PointsViewModel = viewModel()
        
        // Handle Deep Linking from Notifications
        LaunchedEffect(deepLinkRoute) {
            if (!deepLinkRoute.isNullOrBlank()) {
                try {
                    navController.navigate(deepLinkRoute) {
                        launchSingleTop = true
                    }
                    onDeepLinkConsumed?.invoke()
                } catch (e: Exception) {
                    android.util.Log.e("MainComposeApp", "Failed to navigate to deep link: $deepLinkRoute", e)
                }
            }
        }

        // Show Rewarded Interstitial Ad when transitioning to the Rewards or Earn Screen
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        // Professional, non-annoying Refer & Earn Popup on App Open (Frequency Capped: 1x/24h)
        LaunchedEffect(currentUser, showSplashScreen) {
            if (currentUser != null && !showSplashScreen && deepLinkRoute.isNullOrBlank()) {
                kotlinx.coroutines.delay(1500)
                if (com.quizedguy.reelnearn.shared.ui.components.ReferralPopupManager.shouldShow(context)) {
                    showReferralPopup = true
                    com.quizedguy.reelnearn.shared.ui.components.ReferralPopupManager.recordShown(context)
                }
            }
        }

        if (showReferralPopup) {
            com.quizedguy.reelnearn.shared.ui.components.ReferAndEarnPopupDialog(
                referralCode = userReferralCode ?: "",
                onDismiss = { showReferralPopup = false },
                onNavigateToReferrals = {
                    navController.navigate(Screen.Referrals.route) {
                        launchSingleTop = true
                    }
                }
            )
        }

        LaunchedEffect(currentRoute) {
            if (context is Activity && currentUser != null && (currentRoute == Screen.Rewards.route || currentRoute == Screen.Earn.route || currentRoute == Screen.SponsoredTasks.route)) {
                RewardedInterstitialAdManager.showAd(context)
            }
        }

        Scaffold(
            containerColor = ReelBlack,
            bottomBar = {
                val currentBottomItems = remember(isAdmin) {
                    if (isAdmin) bottomNavItems + Screen.Admin else bottomNavItems
                }
                
                Column {
                    if (!isMinor) {
                        key(currentRoute) {
                            BannerAdView()
                        }
                    }
                    NavigationBar(
                        containerColor = ReelSurface,
                        contentColor = TextWhite
                    ) {
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentDestination = navBackStackEntry?.destination
                        
                        currentBottomItems.forEach { screen ->
                            NavigationBarItem(
                                icon = { Icon(screen.icon, contentDescription = screen.title) },
                                label = { Text(screen.title) },
                                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = NeonPink,
                                    selectedTextColor = NeonPink,
                                    indicatorColor = NeonPink.copy(alpha = 0.25f),
                                    unselectedIconColor = TextGray,
                                    unselectedTextColor = TextGray
                                ),
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Home.route) { 
                    DashboardScreen(
                        navController = navController,
                        viewModel = dashboardViewModel,
                        pointsViewModel = pointsViewModel
                    ) 
                }
                composable(Screen.ScreenTime.route) { ScreenTimeScreen() }
                composable(Screen.Rewards.route) { 
                    RewardsScreen(
                        navController = navController,
                        pointsViewModel = pointsViewModel
                    ) 
                }
                composable(Screen.Points.route) { 
                    PointsScreen(
                        viewModel = pointsViewModel
                    ) 
                }
                composable(Screen.Collection.route) { 
                    PointCollectionScreen(
                        navController = navController,
                        pointsViewModel = pointsViewModel
                    ) 
                }
                composable(Screen.Profile.route) { 
                    ProfileScreen(
                        navController = navController,
                        authViewModel = authViewModel,
                        pointsViewModel = pointsViewModel
                    ) 
                }
                composable(Screen.Admin.route) { 
                    AdminDashboardScreen(
                        navController = navController,
                        authViewModel = authViewModel
                    ) 
                }
                composable(Screen.Referrals.route) {
                    ReferralScreen(
                        navController = navController,
                        authViewModel = authViewModel
                    )
                }
                composable(Screen.Earn.route) {
                    SponsoredTasksScreen(
                        navController = navController,
                        pointsViewModel = pointsViewModel
                    )
                }
                composable(Screen.SponsoredTasks.route) {
                    SponsoredTasksScreen(
                        navController = navController,
                        pointsViewModel = pointsViewModel
                    )
                }
                composable(Screen.MyRewardsHistory.route) { MyRewardsHistoryScreen(navController = navController) }
            }
        }
    }
}


