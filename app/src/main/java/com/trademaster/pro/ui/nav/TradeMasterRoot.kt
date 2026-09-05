package com.trademaster.pro.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.trademaster.pro.BuildConfig
import com.trademaster.pro.auth.requestGoogleIdToken
import com.trademaster.pro.data.repo.AdminAuthRepository
import com.trademaster.pro.data.repo.AuthSummary
import com.trademaster.pro.data.repo.TradeRepository
import com.trademaster.pro.ui.components.AdminLoginDialog
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
    val context = LocalContext.current
    var showLoginDialog by remember { mutableStateOf(false) }
    var loginLoading by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }

    // Single reactive source for "who's signed in right now" -- updates live
    // across Google sign-in, admin sign-in, and sign-out, from anywhere in
    // the app (e.g. Settings' sign-out button), unlike a plain function call
    // which Compose would never know to re-read.
    val authSummary by adminAuth.observeAuthSummary().collectAsState(
        initial = AuthSummary(isAnonymous = true, isGoogleUser = false, isAdminAccount = false, email = null, displayName = null, uid = null)
    )

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
                    if (mode == AppMode.CLIENT && !authSummary.isGoogleUser) {
                        IconButton(onClick = {
                            scope.launch {
                                val token = requestGoogleIdToken(context, BuildConfig.GOOGLE_WEB_CLIENT_ID)
                                if (token == null) {
                                    snackbarHostState.showSnackbar(
                                        if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank())
                                            "Google Sign-In isn't configured yet."
                                        else
                                            "Sign-in cancelled."
                                    )
                                } else {
                                    val result = adminAuth.signInWithGoogleIdToken(token)
                                    val message = if (result.isSuccess) {
                                        "Signed in as ${adminAuth.currentEmail() ?: "your Google account"}"
                                    } else {
                                        "Sign-in failed: ${result.exceptionOrNull()?.localizedMessage}"
                                    }
                                    snackbarHostState.showSnackbar(message)
                                }
                            }
                        }) {
                            Icon(Icons.Filled.Login, contentDescription = "Sign in with Google", tint = TextDim)
                        }
                    }
                    IconButton(onClick = {
                        if (mode == AppMode.ADMIN) {
                            onModeChange(AppMode.CLIENT)
                        } else if (isAdminGranted) {
                            onModeChange(AppMode.ADMIN)
                        } else if (authSummary.isAdminAccount) {
                            // Already logged into the admin account on this
                            // device, just not (yet) on the allowlist --
                            // no point re-showing a login form for that.
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    "Signed in as ${authSummary.email ?: "admin"}, but this account isn't on the admin list yet. UID: ${authSummary.uid}"
                                )
                            }
                        } else {
                            loginError = null
                            showLoginDialog = true
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
                    SettingsScreen(adminAuth = adminAuth, onModeChange = onModeChange)
                }
            }
        }
    }

    if (showLoginDialog) {
        AdminLoginDialog(
            uidForDisplay = adminAuth.currentUid(),
            loading = loginLoading,
            error = loginError,
            onDismiss = { showLoginDialog = false; loginError = null },
            onSubmit = { email, password ->
                scope.launch {
                    loginLoading = true
                    loginError = null
                    val result = adminAuth.signInAdmin(email, password)
                    if (result.isFailure) {
                        loginLoading = false
                        loginError = result.exceptionOrNull()?.localizedMessage ?: "Sign-in failed"
                    } else {
                        val granted = adminAuth.checkIsAdminOnce()
                        loginLoading = false
                        if (granted) {
                            showLoginDialog = false
                            onModeChange(AppMode.ADMIN)
                        } else {
                            loginError = "Signed in, but this account isn't on the admin list yet.\n" +
                                "UID: ${adminAuth.currentUid()}\n" +
                                "Ask the project owner to add it under admins/ in Firebase Console -> Firestore."
                        }
                    }
                }
            }
        )
    }
}
