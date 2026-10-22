package com.repforge.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.migration.LegacyWebViewMigrator
import com.repforge.app.data.store.TrainPilotSettingsStore
import com.repforge.app.ui.navigation.TrainPilotAppScaffold
import com.repforge.app.ui.theme.TrainPilotTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var startupScreen: StartupScreen
    private lateinit var settingsStore: TrainPilotSettingsStore
    private lateinit var database: TrainPilotDatabase
    private lateinit var migrator: LegacyWebViewMigrator
    private var migrationReady by mutableStateOf(false)
    private var migrationFailure by mutableStateOf(false)
    private var migrationJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        startupScreen = StartupScreen.install(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        settingsStore = TrainPilotSettingsStore(this)
        database = TrainPilotDatabase.getInstance(this)
        migrator = LegacyWebViewMigrator(this, database, settingsStore)

        startMigration()

        setContent {
            val settings by settingsStore.settingsFlow.collectAsStateWithLifecycle(
                initialValue = com.repforge.app.data.store.TrainPilotUserSettings()
            )
            TrainPilotTheme(settings = settings) {
                if (migrationReady) {
                    TrainPilotAppScaffold(settings = settings, onFirstFrameReady = { startupScreen.markNativeReady() })
                } else {
                    LaunchedEffect(Unit) { startupScreen.markNativeReady() }
                    Column(Modifier.padding(24.dp)) {
                        if (migrationFailure) {
                            Text("Az adatok átvétele nem sikerült. Az eredeti tárhely megmaradt; az átállás nincs késznek jelölve.")
                            Button(onClick = { startMigration() }) { Text("Újrapróbálás") }
                        } else {
                            CircularProgressIndicator()
                            Text("A meglévő TrainPilot-adatok átvétele…")
                        }
                    }
                }
            }
        }
    }

    private fun startMigration() {
        if (migrationJob?.isActive == true || migrationReady) return
        migrationFailure = false
        migrationJob = lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) { migrator.migrateIfNeeded() }
                migrationReady = true
                withContext(Dispatchers.IO) {
                    HealthBackgroundService.resume(this@MainActivity)
                    try { HealthBackgroundJob.schedule(this@MainActivity) } catch (_: Exception) {}
                    WearSyncRepository.publishWatchHomeSnapshot(this@MainActivity)
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { migrationFailure = true }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!migrationReady) return
        HealthBackgroundService.stop(this)
        lifecycleScope.launch(Dispatchers.IO) {
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
