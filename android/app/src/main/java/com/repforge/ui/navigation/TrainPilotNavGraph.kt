package com.repforge.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.repforge.*
import com.repforge.ui.screens.*

sealed class NavItem(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Home : NavItem("home", "Főoldal", Icons.Default.Home)
    object ActiveWorkout : NavItem("active_workout", "Edzés", Icons.Default.FitnessCenter)
    object History : NavItem("history", "Napló", Icons.Default.History)
    object Settings : NavItem("settings", "Beállítások", Icons.Default.Settings)
}

@Composable
fun TrainPilotApp(
    homeViewModel: HomeViewModel,
    activeWorkoutViewModel: ActiveWorkoutViewModel,
    historyViewModel: HistoryViewModel,
    settingsViewModel: SettingsViewModel
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                val items = listOf(NavItem.Home, NavItem.ActiveWorkout, NavItem.History, NavItem.Settings)
                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
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
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(NavItem.Home.route) {
                HomeScreen(
                    homeViewModel = homeViewModel,
                    activeWorkoutViewModel = activeWorkoutViewModel,
                    onNavigateToActiveWorkout = { navController.navigate(NavItem.ActiveWorkout.route) }
                )
            }
            composable(NavItem.ActiveWorkout.route) {
                ActiveWorkoutScreen(
                    viewModel = activeWorkoutViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(NavItem.History.route) {
                HistoryScreen(viewModel = historyViewModel)
            }
            composable(NavItem.Settings.route) {
                SettingsScreen(viewModel = settingsViewModel)
            }
        }
    }
}
