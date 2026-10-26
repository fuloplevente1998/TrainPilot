package com.repforge.app

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.repforge.app.data.store.TrainPilotUserSettings
import com.repforge.app.ui.settings.*
import com.repforge.app.ui.theme.TrainPilotTheme
import org.junit.*
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith

/** Real Compose confirmation controls, isolated callbacks; never contacts Google or deletes data. */
@RunWith(AndroidJUnit4::class)
class NativeGoogleMaintenanceUiTest {
    @get:Rule val compose = createEmptyComposeRule()
    private var scenario: ActivityScenario<MainActivity>? = null
    @After fun close() { scenario?.close() }

    @Test fun allThreeActionsRequireConfirmationAndCancelDoesNotExecute() {
        val preview = mutableStateOf<NativeGoogleMaintenance.Request?>(null)
        var confirmed = 0
        scenario = ActivityScenario.launch(MainActivity::class.java).also { activity ->
            activity.onActivity { screen -> screen.setContent {
                TrainPilotTheme(TrainPilotUserSettings()) {
                    Column {
                    NativeGoogleMaintenanceControls("en", false) { kind ->
                        preview.value = NativeGoogleMaintenance.Request(kind, "isolated-test-sub", "isolated@example.invalid",
                            NativeDataReplacement.continuationForCurrentData(screen))
                    }
                    NativeGoogleMaintenanceConfirmation(preview.value, "en", false,
                        { preview.value = null }, { confirmed++; preview.value = null })
                    }
                }
            } }
        }
        for (kind in NativeGoogleMaintenance.Kind.values()) {
            compose.onNodeWithTag("google_maintenance_${kind.key}").performClick()
            compose.onNodeWithText("Affected account: isolated@example.invalid").assertExists()
            compose.onNodeWithTag("google_maintenance_cancel").performClick()
            compose.runOnIdle { assertEquals(0, confirmed) }
        }
        compose.onNodeWithTag("google_maintenance_drive-delete").performClick()
        compose.onNodeWithTag("google_maintenance_confirm").performClick()
        compose.runOnIdle { assertEquals(1, confirmed) }
    }

    @Test fun partialFailureIsNotRenderedAsSuccessfulCompletion() {
        scenario = ActivityScenario.launch(MainActivity::class.java).also { activity ->
            activity.onActivity { screen -> screen.setContent {
                TrainPilotTheme(TrainPilotUserSettings()) { NativeGoogleMaintenanceFeedback("en", "partial", 2) }
            } }
        }
        compose.onNodeWithTag("google_maintenance_result").assertTextContains("At least 2 deletions confirmed", substring = true)
        compose.onNodeWithTag("google_maintenance_result").assertTextContains("incomplete", substring = true)
    }
    @Test fun actualSettingsConfirmationFailsBeforeOauthWhenBackupStartsAfterPreview() {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        org.junit.Assert.assertTrue(context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0)
        val prefs = context.getSharedPreferences("repforge-google", android.content.Context.MODE_PRIVATE)
        val old = prefs.all.toMap()
        var backup = false
        try {
            prefs.edit().clear().putString("sub", "isolated-test-sub").putString("email", "isolated@example.invalid")
                .putString("profile", "{\"sub\":\"isolated-test-sub\",\"email\":\"isolated@example.invalid\"}").commit()
            scenario = ActivityScenario.launch(MainActivity::class.java).also { activity ->
                activity.onActivity { screen -> screen.setContent {
                    TrainPilotTheme(TrainPilotUserSettings()) {
                        SettingsScreen(androidx.navigation.compose.rememberNavController())
                    }
                } }
            }
            compose.onNodeWithTag("settings_list").performScrollToNode(hasTestTag("google_maintenance_drive-delete"))
            compose.onNodeWithTag("google_maintenance_drive-delete").performClick()
            compose.waitUntil(20_000) { compose.onAllNodesWithTag("google_maintenance_confirm").fetchSemanticsNodes().isNotEmpty() }
            NativeHealthBackupGate.begin(); backup = true
            compose.onNodeWithTag("google_maintenance_confirm").performClick()
            compose.waitUntil(20_000) { compose.onAllNodesWithTag("google_maintenance_result").fetchSemanticsNodes().isNotEmpty() }
            // Native requireMaintenance admission runs before Identity.authorize: no OAuth request/transport is faked.
            val language = kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
                com.repforge.app.data.store.TrainPilotSettingsStore(context).current().language
            }
            compose.onNodeWithTag("google_maintenance_result").assertTextEquals(
                com.repforge.app.ui.text.nativeResources(context, language).getString(R.string.google_maintenance_failed))
            org.junit.Assert.assertFalse(prefs.getBoolean("maintenancePaused", false))
            org.junit.Assert.assertEquals("isolated-test-sub", prefs.getString("sub", ""))
        } finally {
            if (backup) NativeHealthBackupGate.end()
            scenario?.close(); scenario = null
            val edit = prefs.edit().clear()
            for ((key, value) in old) when (value) {
                is String -> edit.putString(key, value)
                is Boolean -> edit.putBoolean(key, value)
                is Int -> edit.putInt(key, value)
                is Long -> edit.putLong(key, value)
                is Float -> edit.putFloat(key, value)
            }
            edit.commit()
        }
    }
}
