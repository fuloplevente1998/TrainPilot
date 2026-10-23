package com.repforge.app.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.repforge.app.ActiveWorkoutStore
import com.repforge.app.NativeWorkoutSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import com.repforge.app.data.store.TrainPilotUserSettings
import com.repforge.app.ui.home.HomeScreen
import com.repforge.app.ui.workout.ActiveWorkoutScreen
import com.repforge.app.ui.settings.SettingsScreen
import com.repforge.app.ui.progress.ProgressScreen
import com.repforge.app.ui.journal.JournalScreen
import com.repforge.app.ui.health.HealthDashboardScreen
import com.repforge.app.ui.programs.TrainingProgramsScreen
import com.repforge.app.ui.theme.LocalTrainPilotColors

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Home : Screen("home", "Kezdőlap", Icons.Default.Home)
    data object Training : Screen("training", "Edzés", Icons.Default.FitnessCenter)
    data object Journal : Screen("journal", "Napló", Icons.Default.History)
    data object Progress : Screen("progress", "Fejlődés", Icons.Default.ShowChart)
    data object Health : Screen("health", "Egészség", Icons.Default.DateRange)
}

val BottomNavItems = listOf(
    Screen.Home,
    Screen.Training,
    Screen.Journal,
    Screen.Progress,
    Screen.Health
)

@Composable
fun TrainPilotAppScaffold(
    settings: TrainPilotUserSettings,
    onFirstFrameReady: () -> Unit
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val colors = LocalTrainPilotColors.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var starting by remember { mutableStateOf(false) }
    val startWorkout: (String?, String) -> Unit = { programId, dayId ->
        if (!starting) {
            starting = true
            scope.launch {
                try {
                    NativeWorkoutSession.startWorkout(context, programId, dayId)
                    navController.navigate("active_workout") { launchSingleTop = true }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    snackbar.showSnackbar(error.message ?: "Az edzést nem sikerült elindítani. Az adatok megmaradtak.")
                } finally {
                    starting = false
                }
            }
        }
    }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route

    val activeSnapshot by ActiveWorkoutStore.snapshotFlow.collectAsStateWithLifecycle()
    val hasActiveWorkout = activeSnapshot?.optBoolean("active", false) == true
    val activeWorkoutId = activeSnapshot?.optString("workoutId").orEmpty()
    val isWorkoutPaused = activeSnapshot?.optBoolean("workoutPaused", false) == true
    val timerOwner = activeSnapshot?.optString("timerOwner", "phone").orEmpty()

    LaunchedEffect(Unit) {
        ActiveWorkoutStore.current(context)
        onFirstFrameReady()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colors.bg,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            if (currentRoute !in listOf("active_workout", "route_map")) {
                TopNavigationBar(
                    onCoachClick = { navController.navigate("training") },
                    onSettingsClick = { navController.navigate("settings") }
                )
            }
        },
        bottomBar = {
            if (currentRoute !in listOf("active_workout", "route_map")) {
                BottomNavigationBar(navController = navController, currentRoute = currentRoute)
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        navController = navController,
                        onStartWorkout = { dayId ->
                            startWorkout(null, dayId)
                        }
                    )
                }
                composable(Screen.Training.route) {
                    TrainingProgramsScreen(
                        navController = navController,
                        onStartProgramDay = { programId, dayId ->
                            startWorkout(programId, dayId)
                        }
                    )
                }
                composable(Screen.Journal.route) {
                    JournalScreen(navController = navController)
                }
                composable(Screen.Progress.route) {
                    ProgressScreen(navController = navController)
                }
                composable(Screen.Health.route) {
                    HealthDashboardScreen(navController = navController)
                }
                composable("active_workout") {
                    ActiveWorkoutScreen(navController = navController)
                }
                composable("settings") {
                    SettingsScreen(navController = navController)
                }
            }

            // Floating Active Workout Banner (ha fut edzés, de nem az aktív edzés nézetben vagyunk)
            AnimatedVisibility(
                visible = hasActiveWorkout && currentRoute != "active_workout",
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                FloatingActiveWorkoutBanner(
                    title = activeSnapshot?.optString("dayName", "Folyamatban lévő edzés") ?: "Edzés",
                    isPaused = isWorkoutPaused,
                    timerOwner = timerOwner,
                    onClick = { navController.navigate("active_workout") }
                )
            }
        }
    }
}

@Composable
fun TopNavigationBar(
    onCoachClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val colors = LocalTrainPilotColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .background(colors.bg)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "TRAINPILOT",
            color = colors.accent,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onCoachClick) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Coach",
                    tint = colors.accent2
                )
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Beállítások",
                    tint = colors.muted
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(
    navController: NavHostController,
    currentRoute: String
) {
    val colors = LocalTrainPilotColors.current
    NavigationBar(
        modifier = Modifier.navigationBarsPadding(),
        containerColor = colors.card,
        contentColor = colors.text
    ) {
        BottomNavItems.forEach { screen ->
            val selected = currentRoute == screen.route
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = screen.title,
                        tint = if (selected) colors.accent else colors.muted
                    )
                },
                label = {
                    Text(
                        text = screen.title,
                        color = if (selected) colors.accent else colors.muted,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                selected = selected,
                onClick = {
                    if (currentRoute != screen.route) {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = colors.card2
                )
            )
        }
    }
}

@Composable
fun FloatingActiveWorkoutBanner(
    title: String,
    isPaused: Boolean,
    timerOwner: String,
    onClick: () -> Unit
) {
    val colors = LocalTrainPilotColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.card2)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isPaused) colors.accent else Color(0xFF3CA87D))
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    color = colors.text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (isPaused) "Szünetel" else if (timerOwner == "watch") "Óra vezérli" else "Telefon vezérli",
                    color = colors.muted,
                    fontSize = 11.sp
                )
            }
        }
        Text(
            text = "Folytatás ›",
            color = colors.accent,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
