package com.repforge.wear.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.repforge.wear.WearWorkoutManager

class WearMainActivity : ComponentActivity() {

    private lateinit var workoutManager: WearWorkoutManager

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Pulzusmérés engedélyezve */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        workoutManager = WearWorkoutManager.getInstance(applicationContext)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.BODY_SENSORS)
        }

        setContent {
            WearActiveWorkoutScreen(workoutManager = workoutManager)
        }
    }
}
