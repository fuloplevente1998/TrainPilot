package com.repforge.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.migration.LegacyWebViewMigrator
import com.repforge.app.data.store.TrainPilotSettingsStore
import com.repforge.app.ui.navigation.TrainPilotAppScaffold
import com.repforge.app.ui.theme.TrainPilotTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var startupScreen: StartupScreen
    private lateinit var settingsStore: TrainPilotSettingsStore
    private lateinit var database: TrainPilotDatabase
    private lateinit var migrator: LegacyWebViewMigrator

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        val splashScreen = installSplashScreen()
        startupScreen = StartupScreen.install(this)
        super.onCreate(savedInstanceState)

        settingsStore = TrainPilotSettingsStore(this)
        database = TrainPilotDatabase.getInstance(this)
        migrator = LegacyWebViewMigrator(this, database, settingsStore)

        CoroutineScope(Dispatchers.IO).launch {
            migrator.migrateIfNeeded()
            HealthBackgroundService.resume(this@MainActivity)
            try { HealthBackgroundJob.schedule(this@MainActivity) } catch (_: Exception) {}
            WearSyncRepository.publishWatchHomeSnapshot(this@MainActivity)
        }

        setContent {
            val settings by settingsStore.settingsFlow.collectAsStateWithLifecycle(
                initialValue = com.repforge.app.data.store.TrainPilotUserSettings()
            )
            TrainPilotTheme(settings = settings) {
                TrainPilotAppScaffold(
                    settings = settings,
                    onFirstFrameReady = {
                        startupScreen.markNativeReady()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        HealthBackgroundService.stop(this)
        CoroutineScope(Dispatchers.IO).launch {
            try { HealthJournalStore.get(this@MainActivity).retireWatchUi() } catch (_: Exception) {}
            try { HealthBackgroundJob.schedule(this@MainActivity) } catch (_: Exception) {}
            WearSyncRepository.publishWatchHomeSnapshot(this@MainActivity)
        }
    }

    override fun onDestroy() {
        startupScreen.destroy()
        super.onDestroy()
    }
}
