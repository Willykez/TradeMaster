package com.willykez.codeorganizer.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.willykez.codeorganizer.ui.screens.HomeScreen
import com.willykez.codeorganizer.ui.screens.SettingsScreen
import com.willykez.codeorganizer.viewmodel.MainViewModel

private object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
}

@Composable
fun CodeOrganizerNavGraph(viewModel: MainViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                viewModel = viewModel,
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                viewModel = viewModel,
                onBack = {
                    viewModel.refreshSettings()
                    navController.popBackStack()
                }
            )
        }
    }
}
