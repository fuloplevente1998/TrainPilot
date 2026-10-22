package com.repforge

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.repforge.ui.navigation.TrainPilotApp
import com.repforge.ui.theme.TrainPilotTheme

class MainActivity : ComponentActivity() {

    private lateinit var healthConnectManager: HealthConnectManager
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var activeWorkoutViewModel: ActiveWorkoutViewModel
    private lateinit var historyViewModel: HistoryViewModel
    private lateinit var settingsViewModel: SettingsViewModel

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* Engedélyek elfogadva háttérkövetéshez és pulzusméréshez */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Natív motorok és ViewModel-ek példányosítása
        healthConnectManager = HealthConnectManager(applicationContext)
        homeViewModel = HomeViewModel(healthConnectManager)
        activeWorkoutViewModel = ActiveWorkoutViewModel(applicationContext)
        historyViewModel = HistoryViewModel()
        settingsViewModel = SettingsViewModel()

        requestRequiredPermissions()

        setContent {
            val settingsState by settingsViewModel.uiState.collectAsState()

            TrainPilotTheme(selectedTheme = settingsState.selectedTheme) {
                TrainPilotApp(
                    homeViewModel = homeViewModel,
                    activeWorkoutViewModel = activeWorkoutViewModel,
                    historyViewModel = historyViewModel,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    }

    private fun requestRequiredPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BODY_SENSORS) {
            permissions.add(Manifest.permission.BODY_SENSORS)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }
}
