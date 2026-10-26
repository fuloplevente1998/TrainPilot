package com.repforge.app

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.repforge.app.data.db.WorkoutHistoryEntity
import com.repforge.app.data.db.WorkoutHistoryExerciseEntity
import com.repforge.app.data.db.WorkoutHistorySetEntity
import com.repforge.app.data.db.WorkoutPhotoEntity
import java.io.FileOutputStream
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Only synthetic photo data on a reset, disposable API35 emulator; no public exports. */
@RunWith(AndroidJUnit4::class)
class NativeProgressPhotoRangeUiTest {
    @get:Rule val compose = createEmptyComposeRule()
    @get:Rule val testIdentity = org.junit.rules.TestName()
    private val fixture = NativeLocalizationEmulatorFixture()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device get() = UiDevice.getInstance(instrumentation)
    private var scenario: ActivityScenario<MainActivity>? = null
    private val repository by lazy { WorkoutPhotoRepository(fixture.context) }
    private val photoId = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
    private val missingId = "bbbbbbbb-cccc-dddd-eeee-ffffffffffff"
    private val deletedId = "cccccccc-dddd-eeee-ffff-aaaaaaaaaaaa"
    private val orphanId = "dddddddd-eeee-ffff-aaaa-bbbbbbbbbbbb"
    private var shot = 0

    @Before fun setup() = fixture.reset()

    @After fun finish() {
        scenario?.close()
        for (id in listOf(photoId, missingId, deletedId, orphanId)) repository.photoFile(id).delete()
        fixture.stopServices()
    }

    private fun exists(tag: String) =
        compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    private fun enterProgress() {
        scenario = fixture.launch()
        compose.waitUntil(30_000) { exists("navigation_progress") }
        compose.onNodeWithTag("navigation_progress").performClick()
        compose.waitUntil(30_000) { exists("progress_list") }
    }

    private suspend fun seedWorkout(index: Int, ageDays: Int, photoIds: List<Pair<String, String?>> = emptyList()) {
        val dao = fixture.database.workoutHistoryDao()
        val now = System.currentTimeMillis()
        val epoch = now - ageDays * 86_400_000L
        val id = "g10-workout-${index}"
        dao.insertWorkout(WorkoutHistoryEntity(
            id = id, workoutKey = "g10-key-${index}", syncId = "g10-sync-${index}",
            started = Instant.ofEpochMilli(epoch).toString(),
            finished = Instant.ofEpochMilli(epoch + 60_000).toString(),
            startedEpochMs = epoch, finishedEpochMs = epoch + 60_000,
            durationSeconds = 60, programId = "g10-program", programName = "G10 Program",
            dayId = "g10-day", dayName = "Photo workout", scheduleId = "",
            quickWorkout = false, originDevice = "phone",
            ownershipProtocol193 = 0, ownerEpoch193 = 0L,
            effortFeedback = null, feedbackSkipped = false, feedbackUpdatedAt = 0L,
            totalVolumeKg = 20.0, completedSetsCount = 1, totalSetsCount = 1,
            totalRepsCount = 8, hasPr226 = false, prRecordsJson226 = "[]",
            wearMetricsJson = null, health240Json = null, timeLogs162Json = null,
            activeIntervalsJson = null, editedAt = null, rawJson = "{}"
        ))
        val exerciseRow = dao.insertExercise(WorkoutHistoryExerciseEntity(
            workoutHistoryId = id, exerciseId = "goblet-squat",
            nameHu = "Guggolás", nameEn = "Squat", loadType = "single_dumbbell",
            repUnit = "ism.", measurementType = "reps", orderIndex = 0,
            actualRir = null, setRir229 = false
        ))
        dao.insertSets(listOf(WorkoutHistorySetEntity(
            setId = "g10-set-${index}", workoutHistoryId = id,
            historyExerciseRowId = exerciseRow, exerciseId = "goblet-squat",
            setNumber = 1, weight = 30.0, reps = "8",
            leftSeconds = null, rightSeconds = null, distanceMeters = null,
            done = true, actualRir = null, gpsRouteKey163 = null, gpsRouteKey193 = null
        )))
        if (photoIds.isNotEmpty()) {
            dao.insertPhotos(photoIds.map { (photo, deletedAt) ->
                WorkoutPhotoEntity(photo, id, "before", Instant.ofEpochMilli(epoch).toString(),
                    now, 64, 64, 256, null, deletedAt)
            })
        }
    }

    private fun seedJpeg(id: String) {
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        try {
            FileOutputStream(repository.photoFile(id)).use {
                assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it))
            }
        } finally { bitmap.recycle() }
    }

    private fun screenshot(screen: String, language: String = "hu", theme: String = "classicBlue") {
        compose.waitForIdle()
        val name = "g10_${testIdentity.methodName.hashCode().toUInt().toString(16)}_${screen}_${++shot}"
        require(Regex("[a-z0-9_]{1,120}").matches(name))
        val request = JSONObject().put("name", name).put("screen", screen)
            .put("language", language).put("theme", theme)
            .put("data", "synthetic_disposable_g10_photo_fixture").toString()
        val pipes = instrumentation.uiAutomation.executeShellCommandRw("tee /data/local/tmp/trainpilot-shot-request")
        android.os.ParcelFileDescriptor.AutoCloseOutputStream(pipes[1]).use {
            it.write(request.toByteArray(Charsets.UTF_8))
        }
        android.os.ParcelFileDescriptor.AutoCloseInputStream(pipes[0]).use { it.readBytes() }
        val deadline = System.nanoTime() + 15_000_000_000L
        while (System.nanoTime() < deadline) {
            if (device.executeShellCommand("cat /data/local/tmp/trainpilot-shot-ack").trim() == name) return
            Thread.sleep(100)
        }
        fail("Raw screenshot missing: ${name}")
    }

    @Test fun emptyGalleryAndStrengthStatesAreDistinct() {
        enterProgress()
        compose.onNodeWithTag("progress_list").performScrollToNode(hasTestTag("progress_photo_gallery"))
        compose.waitUntil(30_000) { exists("progress_photo_empty") }
        screenshot("empty")
        compose.onNodeWithTag("progress_list").performScrollToNode(hasTestTag("progress_strength_ranges"))
        compose.waitUntil(30_000) { exists("progress_strength_empty") }
        compose.onNodeWithTag("progress_strength_days_90").assertIsSelected()
    }

    @Test fun galleryShowsOnlyOwnedLivePhotosWithExplicitCloseAndRealDecode() {
        runBlocking(Dispatchers.IO) {
            seedWorkout(1, 1, listOf(photoId to null, missingId to null, deletedId to "2026-10-01"))
            seedJpeg(photoId)
            repository.photoFile(orphanId).writeText("orphan")
        }
        enterProgress()
        compose.onNodeWithTag("progress_list").performScrollToNode(hasTestTag("progress_photo_gallery"))
        compose.waitUntil(30_000) { exists("progress_photo_choices") }
        compose.onNodeWithTag("progress_photo_${photoId}").assertExists()
        compose.onNodeWithTag("progress_photo_${deletedId}").assertDoesNotExist()
        compose.onNodeWithTag("progress_photo_${orphanId}").assertDoesNotExist()
        screenshot("gallery")
        compose.onNodeWithTag("progress_photo_${photoId}").performClick()
        compose.waitUntil(30_000) { exists("progress_photo_detail") }
        screenshot("detail")
        compose.onNodeWithTag("progress_photo_close").performClick()
        compose.onNodeWithTag("progress_photo_detail").assertDoesNotExist()
        compose.onNodeWithTag("progress_photo_choices")
            .performScrollToNode(hasTestTag("progress_photo_${missingId}"))
        // The clickable thumbnail merges its child semantics. Inspect the real
        // error node in the unmerged tree rather than treating it as absent.
        compose.waitUntil(30_000) {
            compose.onAllNodesWithTag("progress_photo_error", useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test fun strengthRangeRemainsIndependentAcrossLanguageAndTheme() {
        runBlocking(Dispatchers.IO) { seedWorkout(2, 120) }
        enterProgress()
        compose.onNodeWithTag("progress_list").performScrollToNode(hasTestTag("progress_strength_ranges"))
        compose.onNodeWithTag("progress_strength_all").performClick()
        compose.waitUntil(30_000) { !exists("progress_strength_loading") }
        compose.onNodeWithTag("progress_strength_days_30").performClick()
        compose.onNodeWithTag("progress_strength_all").performClick()
        compose.onNodeWithTag("progress_strength_days_30").performClick()
        compose.waitUntil(30_000) { exists("progress_strength_empty") }
        compose.onNodeWithTag("progress_strength_days_30").assertIsSelected()
        screenshot("range")
        compose.onNodeWithTag("navigation_settings").performClick()
        fixture.selectLanguageThroughUi(compose, "en")
        compose.onNodeWithTag("settings_list").performScrollToNode(hasTestTag("theme_yellow"))
        compose.onNodeWithTag("theme_yellow").performScrollTo().performClick()
        compose.onNodeWithTag("navigation_back").performClick()
        compose.waitUntil(30_000) { exists("progress_list") }
        compose.onNodeWithTag("progress_list").performScrollToNode(hasTestTag("progress_strength_ranges"))
        compose.onNodeWithTag("progress_strength_days_30").assertIsSelected()
        compose.waitUntil(30_000) { exists("progress_strength_empty") }
        screenshot("range_en_yellow", "en", "yellow")
    }
}
