package com.repforge.app

import android.content.Context
import com.repforge.app.data.store.TrainPilotSettingsStore
import com.repforge.app.ui.onboarding.NativeOnboardingPolicy
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class NativeOnboardingStateTest {
    private lateinit var context: Context
    private lateinit var store: TrainPilotSettingsStore

    @Before fun reset() = runBlocking {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("trainpilot_data_replacement", Context.MODE_PRIVATE)
            .edit().clear().commit()
        store = TrainPilotSettingsStore(context)
        store.resetAllSettingsForEraseLocal()
    }

    @Test fun distinguishesFreshLegacyAndExistingNativeProfiles() {
        val p = NativeOnboardingPolicy
        assertEquals(p.WELCOME, p.initialState(null, false, "{}"))
        assertEquals(p.COMPLETE, p.initialState(null, true, "{}"))
        assertEquals(p.COMPLETE, p.initialState(null, false, "{\"goal\":\"fitness\"}"))
        assertEquals(p.SKIPPED, p.initialState(p.SKIPPED, true, "{}"))
        assertEquals(p.IN_PROGRESS, p.initialState(p.IN_PROGRESS, true, "{}"))
        assertEquals(p.COMPLETE, p.initialState("unexpected", false, "{}"))
        assertEquals(NativeOnboardingPolicy.Destination.WELCOME, p.destination("pending"))
        assertEquals(NativeOnboardingPolicy.Destination.PROFILE, p.destination(p.IN_PROGRESS))
        assertEquals(NativeOnboardingPolicy.Destination.HOME, p.destination(p.SKIPPED))
    }

    @Test fun ignoresSyntheticEmptyLegacyRawButRespectsRealOrUnknownData() {
        val p = NativeOnboardingPolicy
        assertFalse(p.hasLegacyEvidence(mapOf("legacyStorageRaw" to "{}")))
        assertTrue(p.hasLegacyEvidence(mapOf("legacyStorageRaw" to "{\"repforge:settings\":\"{}\"}")))
        assertTrue(p.hasLegacyEvidence(mapOf("legacyStorageRaw" to "{}", "draft" to "{}")))
        // Corrupt/unknown storage cannot safely be treated as a clean install.
        assertTrue(p.hasLegacyEvidence(mapOf("legacyStorageRaw" to "not-json")))
    }

    @Test fun skipAndResumeSurviveStoreRecreationThenSaveTogether() = runBlocking {
        val p = NativeOnboardingPolicy
        assertEquals(p.WELCOME, store.current().onboarding128)
        assertTrue(store.transitionOnboarding(p.WELCOME, p.SKIPPED,
            NativeDataReplacement.continuationForCurrentData(context)))
        assertEquals(p.SKIPPED, TrainPilotSettingsStore(context).current().onboarding128)
        assertTrue(store.transitionOnboarding(p.SKIPPED, p.IN_PROGRESS,
            NativeDataReplacement.continuationForCurrentData(context)))
        assertEquals(p.IN_PROGRESS, TrainPilotSettingsStore(context).current().onboarding128)
        val oldProfile = store.current().profileJson
        assertTrue(store.completeOnboarding(oldProfile,
            mapOf("goal" to "fitness", "minutes" to "45", "gear" to "dumbbells, mat"),
            NativeDataReplacement.continuationForCurrentData(context)))
        val reopened = TrainPilotSettingsStore(context).current()
        assertEquals(p.COMPLETE, reopened.onboarding128)
        assertEquals(45, JSONObject(reopened.profileJson).getInt("minutes"))
        assertEquals(2, JSONObject(reopened.profileJson).getJSONArray("gear").length())
    }

    @Test fun invalidProfileDoesNotCompleteOrWritePartialData() = runBlocking {
        val p = NativeOnboardingPolicy
        assertTrue(store.transitionOnboarding(p.WELCOME, p.IN_PROGRESS,
            NativeDataReplacement.continuationForCurrentData(context)))
        val old = store.current().profileJson
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                store.completeOnboarding(old, mapOf("weight" to "-2"),
                    NativeDataReplacement.continuationForCurrentData(context))
            }
        }
        assertEquals(p.IN_PROGRESS, store.current().onboarding128)
        assertEquals(old, store.current().profileJson)
    }

    @Test fun staleProfileCannotOverwriteNewerSettingsAndNoProgramIsStarted() = runBlocking {
        val p = NativeOnboardingPolicy
        assertTrue(store.transitionOnboarding(p.WELCOME, p.IN_PROGRESS,
            NativeDataReplacement.continuationForCurrentData(context)))
        val opened = store.current().profileJson
        store.saveTrainingProfile(JSONObject().put("goal", "rehab").put("excluded",
            org.json.JSONArray().put("squat")))
        assertFalse(store.completeOnboarding(opened, mapOf("goal" to "strength"),
            NativeDataReplacement.continuationForCurrentData(context)))
        assertEquals(p.IN_PROGRESS, store.current().onboarding128)
        assertEquals("rehab", JSONObject(store.current().profileJson).getString("goal"))
        assertEquals("home-basic", store.current().activeProgramId)
    }

    @Test fun restoredLegacyOrImportedDraftDoesNotLookLikeFreshInstall() = runBlocking {
        val p = NativeOnboardingPolicy
        val settingsWithDraft = JSONObject().put("draft", JSONObject().put("started", "2026-10-26"))
            .put("settings", JSONObject().put("onboarding128", "pending")
                .put("profile", JSONObject().put("goal", "strength")))
        store.importPortableSettings(settingsWithDraft, replaceTrainingModesOnMissing = true)
        assertEquals(p.COMPLETE, store.current().onboarding128)
        assertEquals("strength", JSONObject(store.current().profileJson).getString("goal"))

        store.resetAllSettingsForEraseLocal()
        store.importPortableSettings(JSONObject().put("settings", JSONObject()
            .put("profile", JSONObject().put("gear", org.json.JSONArray().put("mat")))),
            replaceTrainingModesOnMissing = true)
        assertEquals(p.COMPLETE, store.current().onboarding128)
        assertEquals("mat", JSONObject(store.current().profileJson).getJSONArray("gear").getString(0))
    }

    @Test fun transitionsRejectImplicitCompleteAndDuplicatedSave() = runBlocking {
        val p = NativeOnboardingPolicy
        assertFalse(p.canTransition(p.COMPLETE, p.IN_PROGRESS))
        assertFalse(p.canTransition(p.WELCOME, p.COMPLETE))
        assertTrue(store.transitionOnboarding(p.WELCOME, p.IN_PROGRESS,
            NativeDataReplacement.continuationForCurrentData(context)))
        assertFalse(store.transitionOnboarding(p.WELCOME, p.SKIPPED,
            NativeDataReplacement.continuationForCurrentData(context)))
        val profile = store.current().profileJson
        assertTrue(store.completeOnboarding(profile, mapOf("goal" to "mobility"),
            NativeDataReplacement.continuationForCurrentData(context)))
        assertFalse(store.completeOnboarding(profile, mapOf("goal" to "running"),
            NativeDataReplacement.continuationForCurrentData(context)))
        assertEquals("mobility", JSONObject(store.current().profileJson).getString("goal"))
    }
}
