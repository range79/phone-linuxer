package com.range.phoneLinuxer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.*
import com.range.phoneLinuxer.ui.screen.*
import com.range.phoneLinuxer.viewModel.LinuxViewModel

object Screen {
    const val Welcome = "welcome"
    const val Main = "main"
    const val Settings = "settings"
    const val Logs = "logs"
}

@Composable
fun AppNavigation(vm: LinuxViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Welcome
    ) {
        composable(Screen.Welcome) {
            WelcomeScreen(
                onDownloadDistro = { navController.navigate(Screen.Main) },
                onStartDistro = { },
                onNavigateToSettings = { navController.navigate(Screen.Settings) },
                onNavigateToLogs = { navController.navigate(Screen.Logs) }
            )
        }
        composable(Screen.Main) {
            MainScreen(
                vm = vm,
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings)
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Settings) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToLogs = { navController.navigate(Screen.Logs) }
            )
        }

        composable(Screen.Logs) {
        }
    }
}