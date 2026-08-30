package com.trademaster.pro.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.trademaster.pro.AppMode
import com.trademaster.pro.data.repo.AdminAuthRepository
import com.trademaster.pro.data.repo.TradeRepository
import com.trademaster.pro.ui.screens.community.CommunityScreen
import com.trademaster.pro.ui.screens.community.CommunityViewModel
import com.trademaster.pro.ui.screens.dashboard.DashboardScreen
import com.trademaster.pro.ui.screens.dashboard.DashboardViewModel
import com.trademaster.pro.ui.screens.learn.LearnScreen
import com.trademaster.pro.ui.screens.learn.LearnViewModel
import com.trademaster.pro.ui.screens.settings.SettingsScreen
import com.trademaster.pro.ui.screens.setups.SetupsScreen
import com.trademaster.pro.ui.screens.signals.SignalsScreen
import com.trademaster.pro.ui.screens.signals.SignalsViewModel
import com.trademaster.pro.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun TradeMasterRoot(
    repository: TradeRepository,
    adminAuth: AdminAuthRepository,
    mode: AppMode,
    onModeChange: (AppMode) -> Unit
) {
    val navController = rememberNavController()
    val factory = RepoViewModelFactory(repository)
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Server-granted, not client-decided: true only once Firestore confirms
    // this device's UID has a document under admins/. The bottom-nav mode
    // toggle is cosmetic on its own -- this is what actually gates whether
    // Admin screens are reachable and whether writes will be accepted.
    val isAdminGranted by adminAuth.observeIsAdmin().collectAsState(initial = false)

    // If server-side admin access is ever revoked while the app is open in
    // Admin mode, drop back to Client immediately rather than leaving a
    // UI that promises capabilities Firestore will now reject.
    LaunchedEffect(isAdminGranted) {
        if (!isAdminGranted && mode == AppMode.ADMIN) onModeChange(AppMode.CLIENT)
    }

    Scaffold(
        containerColor = Bg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("TradeMaster Pro", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 17.sp)
                        Text(
                            if (mode == AppMode.ADMIN) "Admin Control" else "Premium Signals",
                            color = Gold, fontSize = 10.sp, fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (mode == AppMode.ADMIN) {
                            onModeChange(AppMode.CLIENT)
                        } else if (isAdminGranted) {
                            onModeChange(AppMode.ADMIN)
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    "This device isn't on the admin list yet. Ask the project owner to add your UID in the Firebase console."
                                )
                            }
                        }
                    }) {
                        Icon(
                            if (mode == AppMode.ADMIN) Icons.Filled.Person else Icons.Filled.AdminPanelSettings,
                            contentDescription = "Switch mode",
                            tint = when {
                                mode == AppMode.ADMIN -> Purple
                                isAdminGranted -> Gold
                                else -> TextMute
                            }
                        )
                    }
                    if (mode == AppMode.ADMIN) {
                        IconButton(onClick = { navController.navigate("settings") }) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = TextDim)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Bg2, titleContentColor = TextPrimary)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Bg2, contentColor = TextDim) {
                Destination.bottomNavItems.forEach { dest ->
                    val selected = currentRoute?.hierarchy?.any { it.route == dest.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(if (selected) dest.filledIcon else dest.outlinedIcon, contentDescription = dest.label) },
                        label = { Text(dest.label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Gold, selectedTextColor = Gold,
                            unselectedIconColor = TextMute, unselectedTextColor = TextMute,
                            indicatorColor = Gold.copy(alpha = 0.12f)
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).background(Bg).fillMaxSize()) {
            NavHost(navController = navController, startDestination = Destination.Dashboard.route) {
                composable(Destination.Dashboard.route) {
                    val vm: DashboardViewModel = viewModel(factory = factory)
                    DashboardScreen(vm)
                }
                composable(Destination.Signals.route) {
                    val vm: SignalsViewModel = viewModel(factory = factory)
                    SignalsScreen(vm, mode)
                }
                composable(Destination.Setups.route) {
                    SetupsScreen()
                }
                composable(Destination.Community.route) {
                    val vm: CommunityViewModel = viewModel(factory = factory)
                    CommunityScreen(vm, mode)
                }
                composable(Destination.Learn.route) {
                    val vm: LearnViewModel = viewModel(factory = factory)
                    LearnScreen(vm, mode)
                }
                composable("settings") {
                    SettingsScreen()
                }
            }
        }
    }
}
