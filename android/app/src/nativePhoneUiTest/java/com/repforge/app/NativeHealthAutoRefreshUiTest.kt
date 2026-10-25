package com.repforge.app

import android.content.Context
import com.repforge.app.ui.health.NativeHealthForegroundAdmission
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Real Compose destination and persisted store. No Samsung/HC SDK success is simulated. */
@RunWith(AndroidJUnit4::class)
class NativeHealthAutoRefreshUiTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context get() = instrumentation.targetContext
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before fun resetPreference() {
        assertTrue(context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0)
        runBlocking(Dispatchers.IO) {
            NativeDataReplacement.mutate(context) {
                HealthJournalStore.get(context).setPreferences(JSONObject()
                    .put("autoOnOpen", false).put("provider", HealthJournalStore.HC))
            }
        }
    }

    @After fun close() {
        scenario?.close()
        runBlocking(Dispatchers.IO) {
            NativeDataReplacement.mutate(context) {
                HealthJournalStore.get(context).setPreferences(JSONObject().put("autoOnOpen", false))
            }
        }
    }

    private fun openHealth() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        compose.waitUntil(20_000) {
            compose.onAllNodesWithTag("navigation_health").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("navigation_health").performClick()
        // LazyColumn creates off-screen children only when scrolled into view.
        // Waiting for the switch before scrolling deadlocks this instrumentation test.
        compose.waitUntil(20_000) {
            compose.onAllNodesWithTag("health_journal_list").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("health_journal_list")
            .performScrollToNode(hasTestTag("health_auto_refresh_switch"))
        compose.onNodeWithTag("health_auto_refresh_switch").assertExists()
    }

    @Test fun quietIoProviderAdmissionActuallyCrossesTheMainLooperAndRejectsBackupRestore() {
        // This exercises the production dispatcher split instead of the pure eligible()
        // helper. The prior bug always rejected even a quiet provider call from IO.
        instrumentation.runOnMainSync {
            assertTrue("Quiet main-looper workout execution should allow a provider read",
                NativeHealthForegroundAdmission.safeOnMain(context))
        }
        val forbiddenDirectIo = runBlocking(Dispatchers.IO) {
            runCatching { NativeHealthForegroundAdmission.safeOnMain(context) }.exceptionOrNull()
        }
        assertTrue("Direct IO access must retain the canonical main-looper guard",
            forbiddenDirectIo is IllegalStateException)
        var published = 0
        runBlocking(Dispatchers.IO) {
            NativeHealthForegroundAdmission.requireMainAdmission(context)
            assertTrue("Actual IO provider admission must be allowed when quiet",
                NativeHealthForegroundAdmission.safeOffMain(context))
            // This is the exact synchronous publication callback used by the real
            // HC and Samsung SDK adapters, without fabricating an SDK measurement.
            NativeHealthForegroundAdmission.publish(context, { }, { published++ })
        }
        assertTrue("A quiet IO read must reach the guarded journal writer", published == 1)
        NativeHealthBackupGate.begin()
        try {
            assertFalse(runBlocking(Dispatchers.IO) {
                NativeHealthForegroundAdmission.safeOffMain(context)
            })
            val error = runBlocking(Dispatchers.IO) {
                runCatching {
                    NativeHealthForegroundAdmission.publish(context, { }, { published++ })
                }.exceptionOrNull()
            }
            assertTrue("Backup must reject provider journal publication", error is IllegalStateException)
            assertTrue(published == 1)
        } finally {
            NativeHealthBackupGate.end()
        }
        val prefs = context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE)
        val before = prefs.getBoolean("blocked", false)
        try {
            assertTrue(prefs.edit().putBoolean("blocked", true).commit())
            assertFalse(runBlocking(Dispatchers.IO) {
                NativeHealthForegroundAdmission.safeOffMain(context)
            })
            val error = runBlocking(Dispatchers.IO) {
                runCatching {
                    NativeHealthForegroundAdmission.publish(context, { }, { published++ })
                }.exceptionOrNull()
            }
            assertTrue("Restore must reject the protected journal writer", error is IllegalStateException)
            assertTrue(published == 1)
        } finally {
            prefs.edit().putBoolean("blocked", before).commit()
        }
        assertTrue(NativeHealthBackupGate.available())
    }

    @Test fun ioProviderPublicationRejectsActiveOrPausedPhoneSnapshot() {
        val prefs = context.getSharedPreferences("trainpilot_wear_sync", Context.MODE_PRIVATE)
        val previous = prefs.getString("snapshot", null)
        try {
            for (frame in listOf(
                JSONObject().put("active", true),
                JSONObject().put("active", true).put("workoutPaused", true),
                JSONObject().put("active", false).put("finalizationPending", true)
            )) {
                assertTrue(prefs.edit().putString("snapshot", frame.toString()).commit())
                assertFalse("A live/paused/finalizing workout must deny IO publication",
                    runBlocking(Dispatchers.IO) {
                        NativeHealthForegroundAdmission.safeOffMain(context)
                    })
                val error = runBlocking(Dispatchers.IO) {
                    runCatching {
                        NativeHealthForegroundAdmission.publish(context, { }, {
                            throw AssertionError("Active workout must not publish")
                        })
                    }.exceptionOrNull()
                }
                assertTrue(error is IllegalStateException)
            }
        } finally {
            val editor = prefs.edit()
            if (previous == null) editor.remove("snapshot") else editor.putString("snapshot", previous)
            assertTrue(editor.commit())
        }
    }

    @Test fun realAutoSwitchPersistsInExistingHealthJournalAndSurvivesDestinationReentry() {
        openHealth()
        compose.onNodeWithTag("health_auto_refresh_switch").assertExists().assertIsOff()
        assertFalse(HealthJournalStore.get(context).preferences().optBoolean("autoOnOpen"))
        compose.onNodeWithTag("health_auto_refresh_switch").performClick()
        compose.waitUntil(15_000) {
            HealthJournalStore.get(context).preferences().optBoolean("autoOnOpen")
        }
        compose.onNodeWithTag("health_auto_refresh_switch").assertIsOn()

        compose.onNodeWithTag("navigation_home").performClick()
        compose.onNodeWithTag("navigation_health").performClick()
        compose.onNodeWithTag("health_journal_list")
            .performScrollToNode(hasTestTag("health_auto_refresh_switch"))
        compose.onNodeWithTag("health_auto_refresh_switch").assertIsOn()

        compose.onNodeWithTag("health_auto_refresh_switch").performClick()
        compose.waitUntil(15_000) {
            !HealthJournalStore.get(context).preferences().optBoolean("autoOnOpen")
        }
        compose.onNodeWithTag("health_auto_refresh_switch").assertIsOff()
    }
}
