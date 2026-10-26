package com.repforge.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.core.content.ContextCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Disposable emulator only: real MainActivity, Android permissions/settings, real Compose dialog. */
@RunWith(AndroidJUnit4::class)
class NativeGpsPermissionRecoveryUiTest {
    @get:Rule val compose = createEmptyComposeRule()
    @get:Rule val testIdentity = org.junit.rules.TestName()
    private var shot = 0
    private val fixture = NativeLocalizationEmulatorFixture()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context get() = fixture.context
    private var scenario: ActivityScenario<MainActivity>? = null
    private var originallyGpsEnabled = true

    private fun permission(p: String) =
        ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED

    @Before fun prepare() {
        fixture.reset() // Refuses physical devices and non-debuggable apps.
        originallyGpsEnabled = gpsEnabled()
    }

    @After fun cleanup() {
        fixture.close(scenario)
        // Permission reset is host-owned, between independent instrumentation processes.
        setGpsEnabled(originallyGpsEnabled)
    }

    private fun shell(command: String) {
        val fd = instrumentation.uiAutomation.executeShellCommand(command)
        ParcelFileDescriptor.AutoCloseInputStream(fd).use { it.readBytes() }
        instrumentation.waitForIdleSync()
    }

    private fun gpsEnabled() =
        context.getSystemService(LocationManager::class.java)
            ?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true

    private fun setGpsEnabled(on: Boolean) {
        shell("cmd location set-location-enabled ${if (on) "true" else "false"}")
        // Android broadcasts the provider state asynchronously even after cmd exits.
        repeat(30) {
            if (gpsEnabled() == on) return
            Thread.sleep(100)
        }
        assertEquals("emulator location switch", on, gpsEnabled())
    }

    private fun requirePermissions(precise: Boolean, approximate: Boolean) {
        assertEquals("Host must prepare precise permission before instrumentation", precise,
            permission(Manifest.permission.ACCESS_FINE_LOCATION))
        assertEquals("Host must prepare coarse permission before instrumentation", approximate,
            permission(Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    /** Device pixels only: host must ACK an unmodified screencap of this foreground app. */
    private fun screenshot(screen: String) {
        compose.waitForIdle()
        val name = "${javaClass.simpleName.lowercase()}_${testIdentity.methodName.hashCode().toUInt().toString(16)}_${screen}_hu_classicblue_${++shot}"
        require(Regex("[a-z0-9_]{1,120}").matches(name))
        val request = JSONObject().put("name", name).put("screen", screen)
            .put("language", "hu").put("theme", "classicBlue")
            .put("data", "synthetic-gps-emulator-fixture").toString()
        val pipes = instrumentation.uiAutomation.executeShellCommandRw("tee /data/local/tmp/trainpilot-shot-request")
        ParcelFileDescriptor.AutoCloseOutputStream(pipes[1]).use {
            it.write(request.toByteArray(Charsets.UTF_8))
        }
        ParcelFileDescriptor.AutoCloseInputStream(pipes[0]).use { it.readBytes() }
        val deadline = System.nanoTime() + 15_000_000_000L
        val device = UiDevice.getInstance(instrumentation)
        while (System.nanoTime() < deadline) {
            if (device.executeShellCommand("cat /data/local/tmp/trainpilot-shot-ack").trim() == name) return
            Thread.sleep(100)
        }
        fail("Host did not capture unmodified GPS screenshot: $name")
    }

    private fun startDistanceWorkout() {
        val exerciseId = runBlocking(Dispatchers.IO) {
            fixture.database.exerciseLibraryDao().getAllExercises().first {
                it.measurementType == "distance" || it.repUnit.trim().lowercase() in setOf("m", "km")
            }.id
        }
        fixture.startQuick(exerciseId)
        scenario = fixture.launch()
        compose.waitUntil(30_000) { compose.onAllNodesWithTag("navigation_training").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("navigation_training").performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithText("GPS-mérés indítása", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun startGpsFromUi() {
        compose.onNodeWithText("GPS-mérés indítása", useUnmergedTree = true).performScrollTo().performClick()
    }

    private fun snapshot(): JSONObject? = fixture.currentWorkout()
    private fun assertNotStarted() {
        val frame = snapshot()
        assertTrue(frame?.optJSONObject(NativeGpsMeasurement.FIELD)?.optString("state").orEmpty() !in
            listOf("prepared", "running", "explicitStopped", "recorded"))
        assertFalse(frame?.getJSONArray("exercises")?.getJSONObject(0)?.getJSONArray("sets")
            ?.getJSONObject(0)?.optBoolean("done") == true)
    }

    private fun waitDialog() {
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("gps_recovery_dialog").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun approximateGrantOpensAppPermissionRouteAndBackDoesNotStartMeasurement() {
        setGpsEnabled(true)
        requirePermissions(precise = false, approximate = true)
        startDistanceWorkout()
        startGpsFromUi()
        waitDialog()
        compose.onNodeWithTag("gps_recovery_settings").assertExists().performClick()
        shell("input keyevent 4") // Back from Android App Info.
        waitDialog()
        assertNotStarted()
        compose.onNodeWithTag("gps_recovery_cancel").performClick()
        assertNotStarted()
    }

    @Test fun systemLocationOffHasOwnSettingsAndRequiresExplicitRetryAfterReturn() {
        requirePermissions(precise = true, approximate = true)
        setGpsEnabled(false)
        startDistanceWorkout()
        startGpsFromUi()
        waitDialog()
        assertNotStarted()
        compose.onNodeWithTag("gps_recovery_settings").assertExists().performClick()
        setGpsEnabled(true) // Emulates user turning the system location switch on.
        shell("input keyevent 4") // Returns to the running app.
        waitDialog()
        assertNotStarted() // ON_RESUME must NOT start GPS by itself.
        val old = requireNotNull(snapshot())
        compose.onNodeWithTag("gps_recovery_retry").performClick()
        compose.waitUntil(15_000) {
            val after = snapshot()?.optJSONObject(NativeGpsMeasurement.FIELD)
            after != null && after.optString("id").isNotBlank()
        }
        val newFrame = requireNotNull(snapshot())
        assertEquals(old.optString("workoutId"), newFrame.optString("workoutId"))
        assertEquals(old.optLong("generation"), newFrame.optLong("generation"))
        assertFalse(newFrame.getJSONArray("exercises").getJSONObject(0)
            .getJSONArray("sets").getJSONObject(0).optBoolean("done"))
        // The real native workout screen after explicit permission/settings retry.
        // Emulator GPS and any heartbeat are synthetic fixtures, not device measurements.
        screenshot("active_gps_retry")
    }

    @Test fun runtimePermissionDismissedByBackNeverRecordsDistance() {
        setGpsEnabled(true)
        requirePermissions(precise = false, approximate = false)
        startDistanceWorkout()
        startGpsFromUi() // Actual RequestMultiplePermissions launcher.
        // Back must dismiss the Android permission sheet, not the app behind it.
        // Observe the platform foreground dialog before sending Back.
        val device = UiDevice.getInstance(instrumentation)
        val deadline = System.nanoTime() + 15_000_000_000L
        var permissionSheetShown = false
        while (System.nanoTime() < deadline) {
            val focus = device.executeShellCommand("dumpsys window").lineSequence()
                .filter { it.contains("mCurrentFocus=") }.joinToString("\n")
            if (focus.contains("permissioncontroller", ignoreCase = true)) {
                permissionSheetShown = true
                break
            }
            Thread.sleep(100)
        }
        assertTrue("Android runtime permission sheet never reached foreground", permissionSheetShown)
        shell("input keyevent 4") // Cancel the observed permission sheet.
        waitDialog()
        assertNotStarted()
        compose.onNodeWithTag("gps_recovery_cancel").performClick()
        assertNotStarted()
    }

    @Test fun staleOriginalWorkoutIsRefusedAfterSettingsOrPermissionBlock() {
        requirePermissions(precise = true, approximate = true)
        setGpsEnabled(false)
        startDistanceWorkout()
        startGpsFromUi()
        waitDialog()
        instrumentation.runOnMainSync { ActiveWorkoutStore.clear(context) } // Isolated fixture only.
        compose.onNodeWithTag("gps_recovery_retry").performClick()
        compose.waitUntil(15_000) {
            compose.onAllNodesWithTag("gps_recovery_dialog").fetchSemanticsNodes().isEmpty()
        }
        // clear() intentionally retains a non-active tombstone to block stale restore;
        // null is not the persistence contract. Neither a current session nor a
        // previously prepared GPS measurement is allowed to be resurrected.
        val cleared = snapshot()
        assertFalse(cleared?.optBoolean("active") == true)
        assertTrue(cleared?.optJSONObject(NativeGpsMeasurement.FIELD)?.optString("state").orEmpty() !in
            listOf("prepared", "running", "explicitStopped", "recorded"))
    }
}
