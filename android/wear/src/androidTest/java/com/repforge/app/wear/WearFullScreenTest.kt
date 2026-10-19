package com.repforge.app.wear

import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import java.time.Instant
import java.time.LocalDate
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Actual Compose layout, touch scrolling and screenshots on a round Wear OS emulator. */
@RunWith(Parameterized::class)
class WearFullScreenTest(private val diameter: Float, private val fontScale: Float) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}dp-font{1}")
        fun sizes() =
            listOf(
                arrayOf(160f, 1f),
                arrayOf(192f, 1f),
                arrayOf(225f, 1f),
                arrayOf(160f, 1.5f),
                arrayOf(225f, 2f),
            )
    }

    @get:Rule val compose = createComposeRule()
    @Test fun gpsConsentRetryStopAndPausedOwnerAreReadableInFourLanguages() {
        val owner=WorkoutSnapshotStore.parse("""{"workoutId":"gps-ui193","ownershipProtocol193":1,"timerOwner":"watch","exercises":[{"id":"running","measurementType":"distance","repUnit":"km","sets":[{"setId":"gps-one","done":false}]}]}""")!!
        val workout=mutableStateOf(owner);val status=mutableStateOf(JSONObject().put("state","permission"));val language=mutableStateOf("hu");val actions=mutableListOf<String>()
        compose.setContent { Watch { GpsRouteScreen193(workout.value,status.value,{actions.add("start")},{actions.add("stop")},{},language.value) } }
        for(lang in listOf("hu","en","de","ro")) {
            compose.runOnIdle { language.value=lang;workout.value=owner;status.value=JSONObject().put("state","permission") }
            val start=WearGpsText193.text("start",lang);val stop=WearGpsText193.text("stop",lang)
            lazyList().performScrollToNode(hasContentDescription(start));compose.onNodeWithContentDescription(start).assertIsEnabled().performClick()
            compose.runOnIdle { status.value=JSONObject().put("state","recording").put("armed",true).put("running",true).put("points",12) }
            lazyList().performScrollToNode(hasContentDescription(stop));compose.onNodeWithContentDescription(stop).assertIsEnabled().performClick()
            compose.onNodeWithContentDescription(start).assertDoesNotExist()
            compose.runOnIdle { workout.value=owner.copy(workoutPaused=true);status.value=JSONObject().put("state","paused") }
            lazyList().performScrollToNode(hasContentDescription(start));compose.onNodeWithContentDescription(start).assertIsNotEnabled()
        }
        assertEquals(listOf("start","stop","start","stop","start","stop","start","stop"),actions)
        lazyList().performScrollToNode(hasContentDescription(WearGpsText193.text("back","ro")))
        compose.onNodeWithContentDescription(WearGpsText193.text("back","ro")).assertIsDisplayed();screenshot("gps-route193-consent-paused")
    }
    @Test fun ownershipRequestPendingAndOfflineResumeAreDistinctActionsOnRoundScreens() {
        val phone=WorkoutSnapshotStore.parse("""{"workoutId":"ui193","ownershipProtocol193":1,"timerOwner":"phone","exercises":[{"id":"plank","sets":[{"setId":"one","done":false}]}]}""")!!
        val state=mutableStateOf(phone);val actions=mutableListOf<String>()
        compose.setContent { Watch { OwnershipScreen193(state.value,true,{actions.add("request")},{actions.add("retry")},{actions.add("resume")},{actions.add("phone")},{actions.add("back")}) } }
        assertFullViewport()
        lazyList().performScrollToNode(hasContentDescription(WearOwnershipText193.text("request")))
        compose.onNodeWithContentDescription(WearOwnershipText193.text("request")).assertIsDisplayed().performClick()
        assertEquals(listOf("request"),actions)
        compose.runOnIdle { state.value=phone.copy(workoutPaused=true,handoff193="""{"id":"ui193-transfer","state":"offered"}""") }
        lazyList().performScrollToNode(hasContentDescription(WearOwnershipText193.text("retry")))
        compose.onNodeWithContentDescription(WearOwnershipText193.text("retry")).assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription(WearOwnershipText193.text("resume")).assertDoesNotExist()
        assertEquals(listOf("request","retry"),actions)
        compose.runOnIdle { state.value=phone.copy(timerOwner="watch",ownerEpoch193=1,workoutPaused=true,handoff193="""{"id":"ui193-transfer","state":"committed"}""") }
        lazyList().performScrollToNode(hasContentDescription(WearOwnershipText193.text("resume")))
        compose.onNodeWithContentDescription(WearOwnershipText193.text("resume")).assertIsDisplayed().performClick()
        assertEquals(listOf("request","retry","resume"),actions)
        lazyList().performScrollToNode(hasContentDescription(WearOwnershipText193.text("back")))
        compose.onNodeWithContentDescription(WearOwnershipText193.text("back")).assertIsDisplayed()
        screenshot("ownership193-paused-target")
    }
    @Test fun disconnectedPhoneCannotOfferTakeoverButConfirmedWatchCanResumeOffline() {
        val phone=WorkoutSnapshotStore.parse("""{"workoutId":"offline193","ownershipProtocol193":1,"timerOwner":"phone","exercises":[{"id":"plank","sets":[{"setId":"one","done":false}]}]}""")!!
        val state=mutableStateOf(phone);var resumed=false
        compose.setContent { Watch { OwnershipScreen193(state.value,false,{fail("Disconnected takeover")},{},{resumed=true},{},{}) } }
        lazyList().performScrollToNode(hasContentDescription(WearOwnershipText193.text("request")))
        compose.onNodeWithContentDescription(WearOwnershipText193.text("request")).assertIsNotEnabled()
        compose.runOnIdle { state.value=phone.copy(timerOwner="watch",ownerEpoch193=1,workoutPaused=true,handoff193="""{"id":"offline193-transfer","state":"committed"}""") }
        lazyList().performScrollToNode(hasContentDescription(WearOwnershipText193.text("resume")))
        compose.onNodeWithContentDescription(WearOwnershipText193.text("resume")).assertIsEnabled().performClick()
        assertTrue(resumed)
    }
    @org.junit.Before fun clearTouchLock189() { InstrumentationRegistry.getInstrumentation().targetContext.getSharedPreferences("trainpilot_wear_touch_lock189",android.content.Context.MODE_PRIVATE).edit().clear().commit() }
    @org.junit.After fun cleanupTouchLock189() { clearTouchLock189() }

    @Test
    fun workoutFeedbackAllFiveChoicesAndSkipAreReachableOnRoundScreens() {
        val answers = mutableListOf<String?>()
        compose.setContent { Watch { WorkoutFeedbackScreen { answers.add(it) } } }
        assertFullViewport()
        WearWorkoutFeedback.choices.forEachIndexed { index, (key, label) ->
            val caption = "${index + 1} · $label"
            lazyList().performScrollToNode(hasContentDescription(caption))
            compose.onNodeWithContentDescription(caption).assertIsDisplayed().performClick()
            assertEquals(key, answers.last())
        }
        lazyList().performScrollToNode(hasContentDescription("Fájdalom / kellemetlenség"))
        compose.onNodeWithContentDescription("Fájdalom / kellemetlenség").assertIsDisplayed().performClick()
        assertEquals("pain", answers.last())
        lazyList().performScrollToNode(hasContentDescription("Most kihagyom"))
        compose.onNodeWithContentDescription("Most kihagyom").assertIsDisplayed().performClick()
        assertNull(answers.last())
        screenshot("workout-feedback-last-choice")
    }

    @Test
    fun watchAndPhoneCompletionFeedbackReturnDirectlyHome() {
        val selected = mutableStateOf("finish")
        val answers = mutableListOf<String?>()
        compose.setContent { Watch { key(selected.value) { Fixture(selected.value, feedbackAnswers = answers) } } }
        compose.onNodeWithContentDescription("Edzés befejezése és mentése").performClick()
        lazyList().performScrollToNode(hasContentDescription("3 · Pont jó"))
        compose.onNodeWithContentDescription("3 · Pont jó").assertIsDisplayed().performClick()
        assertEquals(listOf("good"), answers)
        compose.onNodeWithTag("wear-home-clock").assertIsDisplayed()
        compose.onNodeWithText("3 · Pont jó").assertDoesNotExist()
        compose.runOnIdle { selected.value = "phone-feedback" }
        lazyList().performScrollToNode(hasContentDescription("4 · Kicsit nehéz"))
        compose.onNodeWithContentDescription("4 · Kicsit nehéz").assertIsDisplayed().performClick()
        assertEquals(listOf("good", "challenging"), answers)
        compose.onNodeWithTag("wear-home-clock").assertIsDisplayed()
        screenshot("workout-feedback-return-home")
    }

    @Test
    fun closureQuestionAndAnswerSurviveReloadAndTerminalRetry() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val id = java.util.UUID.randomUUID().toString()
        val workout = WearWorkout(1, "p", "Program", "a", "", "2026-10-14T10:00:00Z", id, 0, 60, 0,
            listOf(WearExercise("press", "Nyomás", "weight", "ism.", "strength", listOf(WearSet(1, "10", "5", true, 0, 0, 0.0)))))
        val command = JSONObject().put("commandId", id).put("action", "finishWorkout").put("workoutId", id)
        WearClosureStore.close(context, workout, command)
        assertTrue(WearClosureStore.load(context)!!.needsFeedback)
        assertEquals(workout.started, WearClosureStore.load(context)!!.started)
        WearClosureStore.answer(context, id, WearWorkoutFeedback.answer("hard"))
        assertFalse(WearClosureStore.load(context)!!.needsFeedback)
        val retry = WearClosureStore.retry(context)!!
        assertEquals("hard", retry.getJSONObject("feedback").getString("effort"))
        WearClosureStore.close(context, workout, command)
        WearClosureStore.acceptResult(context, JSONObject().put("commandId", command.getString("commandId"))
            .put("status", "saved").put("feedbackAnswered", true))
        assertFalse("a phone answer closes the watch question", WearClosureStore.load(context)!!.needsFeedback)
        WearClosureStore.dismiss(context)
    }

    @Test
    fun saveSyncStatesAndRetryRemainReadableOnRoundDisplays() {
        val state=mutableStateOf(WearSaveSyncState("workout",WearSavePhase.LOCAL,2,true))
        var retries=0;var backs=0
        compose.setContent { Watch { SaveSyncScreen(state.value,{retries++},{backs++}) } }
        assertFullViewport()
        for(phase in WearSavePhase.entries) {
            compose.runOnIdle { state.value=WearSaveSyncState("workout",phase,2,phase!=WearSavePhase.SYNCED,
                if(phase==WearSavePhase.ERROR)"timeout" else "") }
            lazyList().performScrollToNode(hasText(wearSaveLabel(phase)))
            compose.onNodeWithText(wearSaveLabel(phase)).assertIsDisplayed()
            if(phase!=WearSavePhase.SYNCED) {
                lazyList().performScrollToNode(hasContentDescription(wearSaveRetry()))
                compose.onNodeWithContentDescription(wearSaveRetry()).assertIsDisplayed().performClick()
            } else compose.onNodeWithContentDescription(wearSaveRetry()).assertDoesNotExist()
        }
        assertEquals(4,retries)
        lazyList().performScrollToNode(hasContentDescription(wearSaveBack()))
        compose.onNodeWithContentDescription(wearSaveBack()).assertIsDisplayed().performClick()
        assertEquals(1,backs)
        screenshot("save-sync-retry")
    }

    @Test
    fun actualOutboxKeepsDataUntilMatchingPhoneReceiptAndRetryReusesIdentity() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val id=java.util.UUID.randomUUID().toString()
        val command=JSONObject().put("commandId",id).put("workoutId",id).put("action","healthSummary")
            .put("createdAt",System.currentTimeMillis()).put("healthSummary",JSONObject().put("steps",0))
        assertTrue(WearCommandOutbox.enqueue(context,command)>0)
        val sequence=command.getLong("sequence")
        val receipt=JSONObject().put("schema",1).put("commandId",id).put("workoutId",id)
            .put("action","healthSummary").put("sequence",sequence).put("status","applied").put("receivedAt",System.currentTimeMillis())
        assertNotEquals(WearSavePhase.SYNCED,WearCommandOutbox.state(context,id)!!.phase)
        WearCommandOutbox.retry(context,id)
        assertEquals(sequence,command.getLong("sequence"))
        val path=WearCommandOutbox.RECEIPT_PREFIX+id
        assertFalse(WearCommandOutbox.acceptReceipt(context,path,JSONObject(receipt.toString()).put("workoutId","foreign").toString()))
        assertNotEquals(WearSavePhase.SYNCED,WearCommandOutbox.state(context,id)!!.phase)
        assertTrue(WearCommandOutbox.acceptReceipt(context,path,receipt.toString()))
        assertEquals(WearSavePhase.SYNCED,WearCommandOutbox.state(context,id)!!.phase)
        assertTrue(WearCommandOutbox.acceptReceipt(context,path,receipt.toString()))
        assertFalse(WearCommandOutbox.acceptReceipt(context,path,JSONObject(receipt.toString()).put("status","error").toString()))
        assertEquals(WearSavePhase.SYNCED,WearCommandOutbox.state(context,id)!!.phase)
    }

    @Test
    fun failedLocalCommitCannotReportSavedOrConsumeReceipt() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val real=context.getSharedPreferences("trainpilot_wear_outbox",0)
        val failing=java.lang.reflect.Proxy.newProxyInstance(javaClass.classLoader,arrayOf(android.content.SharedPreferences::class.java)) { _,method,args ->
            if(method.name!="edit")method.invoke(real,*(args ?: emptyArray())) else {
                val editor=real.edit()
                java.lang.reflect.Proxy.newProxyInstance(javaClass.classLoader,arrayOf(android.content.SharedPreferences.Editor::class.java)) { proxy,m,a ->
                    val result=m.invoke(editor,*(a ?: emptyArray()))
                    if(m.name=="commit")false else if(result===editor)proxy else result
                }
            }
        } as android.content.SharedPreferences
        val wrapper=object:android.content.ContextWrapper(context) {
            override fun getSharedPreferences(name:String,mode:Int):android.content.SharedPreferences =
                if(name=="trainpilot_wear_outbox")failing else super.getSharedPreferences(name,mode)
        }
        val id=java.util.UUID.randomUUID().toString()
        val command=JSONObject().put("commandId",id).put("workoutId",id).put("action","healthSummary").put("createdAt",System.currentTimeMillis())
        assertEquals(0L,WearCommandOutbox.enqueue(wrapper,command))
        val failed=WearCommandOutbox.state(context,id)!!
        assertEquals(WearSavePhase.ERROR,failed.phase);assertEquals("local",failed.reason)
        WearCommandOutbox.retry(context,id)
        val pending=org.json.JSONArray(real.getString("pending","[]"))
        val stored=(0 until pending.length()).mapNotNull { pending.optJSONObject(it) }.single { it.optString("commandId")==id }
        val receipt=JSONObject().put("schema",1).put("commandId",id).put("workoutId",id).put("action","healthSummary")
            .put("sequence",stored.getLong("sequence")).put("status","applied").put("receivedAt",System.currentTimeMillis())
        val path=WearCommandOutbox.RECEIPT_PREFIX+id
        assertFalse(WearCommandOutbox.acceptReceipt(wrapper,path,receipt.toString()))
        assertNotEquals(WearSavePhase.SYNCED,WearCommandOutbox.state(context,id)!!.phase)
        assertTrue(WearCommandOutbox.acceptReceipt(context,path,receipt.toString()))
        assertEquals(WearSavePhase.SYNCED,WearCommandOutbox.state(context,id)!!.phase)
        val before=real.getString("pending","[]")
        val damagedId=java.util.UUID.randomUUID().toString()
        val damaged=JSONObject().put("commandId",damagedId).put("workoutId",damagedId).put("action","healthSummary")
        synchronized(WearCommandOutbox) {
        try {
            real.edit().putString("pending","unreadable").commit()
            assertEquals(0L,WearCommandOutbox.enqueue(context,damaged))
            WearCommandOutbox.flush(context)
            assertEquals("unreadable",real.getString("pending",""))
            assertFalse(WearCommandOutbox.acceptReceipt(context,path,receipt.toString()))
        } finally { real.edit().putString("pending",before).commit() }
        }
        WearCommandOutbox.retry(context,damagedId)
        val restored=org.json.JSONArray(real.getString("pending","[]"))
        val queued=(0 until restored.length()).mapNotNull { restored.optJSONObject(it) }.single { it.optString("commandId")==damagedId }
        val recovered=JSONObject(receipt.toString()).put("commandId",damagedId).put("workoutId",damagedId).put("sequence",queued.getLong("sequence"))
        assertTrue(WearCommandOutbox.acceptReceipt(context,WearCommandOutbox.RECEIPT_PREFIX+damagedId,recovered.toString()))
    }

    @Test
    fun explicitWorkoutPauseShowsOnlySafeContinuationControls() {
        val actions=mutableListOf<String>()
        compose.setContent { Watch { Fixture("workout-paused",automaticCommands=actions) } }
        screenshot("workout-paused-opening")
        assertTrue(compose.onAllNodesWithContentDescription("Edzés szünetel").fetchSemanticsNodes().isNotEmpty())
        // Curved text has multiple virtual accessibility nodes. Verify the actual
        // theme-colored glyphs in the fixed top arc, instead of assuming one rectangular Text node.
        val picture=compose.onRoot().captureToImage().asAndroidBitmap()
        val expected=android.graphics.Color.parseColor(WearHomeTheme.DEFAULT.accent)
        var accentPixels=0
        for(y in 0 until picture.height/4)for(x in 0 until picture.width){
            val pixel=picture.getPixel(x,y)
            if(kotlin.math.abs(android.graphics.Color.red(pixel)-android.graphics.Color.red(expected))<20&&
                kotlin.math.abs(android.graphics.Color.green(pixel)-android.graphics.Color.green(expected))<20&&
                kotlin.math.abs(android.graphics.Color.blue(pixel)-android.graphics.Color.blue(expected))<20)accentPixels++
        }
        assertTrue("The curved pause heading must actually be visible",accentPixels>20)
        compose.onNodeWithContentDescription("Sorozat rögzítése").assertDoesNotExist()
        assertFullViewport()
        lazyList().performScrollToNode(hasContentDescription("Folytatás telefonon"))
        compose.onNodeWithContentDescription("Folytatás telefonon").assertIsDisplayed().performClick()
        assertEquals(listOf("openPhone"),actions)
        lazyList().performScrollToNode(hasContentDescription("Kezdőlap"))
        compose.onNodeWithContentDescription("Kezdőlap").assertIsDisplayed()
        screenshot("workout-paused-continuation")
    }

    @Test
    fun skippedPhoneRestReturnsToAnEnabledRecordingButton() {
        val actions=mutableListOf<String>()
        compose.setContent { Watch { Fixture("phone-rest-finished",automaticCommands=actions) } }
        compose.onNodeWithContentDescription("Automata folytatása").assertDoesNotExist()
        compose.onNodeWithContentDescription("Sorozat rögzítése").assertIsDisplayed().assertIsEnabled().performClick()
        assertEquals(listOf("record"),actions)
        screenshot("phone-rest-finished-recording")
    }

    @Composable
    private fun Watch(content: @Composable () -> Unit) {
        BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
            val pixels = with(LocalDensity.current) { maxWidth.toPx() }
            CompositionLocalProvider(LocalDensity provides Density(pixels / diameter, fontScale)) {
                Box(Modifier.fillMaxSize().clip(CircleShape)) { content() }
            }
        }
    }

    private fun assertFullViewport(tag: String = "wear-round-list") {
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
        val list = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
        assertEquals(
            "Full height, including the space above/below the former strip",
            root.height,
            list.height,
            1f,
        )
        assertEquals("Full viewport width", root.width, list.width, 1f)
    }

    private fun lazyList() =
        compose.onNode(
            SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollToIndex) and
                hasAnyAncestor(hasTestTag("wear-round-list"))
        )

    private fun scrollToEnd(last: String? = null) {
        if (
            compose
                .onAllNodes(
                    SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollToIndex) and
                        hasAnyAncestor(hasTestTag("wear-round-list"))
                )
                .fetchSemanticsNodes()
                .isEmpty()
        )
            return
        if (last != null) lazyList().performScrollToNode(hasContentDescription(last))
        repeat(5) {
            compose.onNodeWithTag("wear-round-list").performTouchInput {
                swipeUp(durationMillis = 300)
            }
        }
        compose.waitForIdle()
    }

    private fun scrollToStart() {
        repeat(5) {
            compose.onNodeWithTag("wear-round-list").performTouchInput {
                swipeDown(durationMillis = 300)
            }
        }
        compose.waitForIdle()
    }

    private fun screenshot(name: String) {
        // MediaStore photos survive Gradle's automatic test-app uninstall; private app files do
        // not.
        val resolver = InstrumentationRegistry.getInstrumentation().targetContext.contentResolver
        val values =
            ContentValues().apply {
                put(
                    MediaStore.Images.Media.DISPLAY_NAME,
                    "$name-${diameter.toInt()}dp-font$fontScale.png",
                )
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/TrainPilot-Wear-layout",
                )
            }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
        resolver.openOutputStream(uri)!!.use {
            assertTrue(
                compose
                    .onRoot()
                    .captureToImage()
                    .asAndroidBitmap()
                    .compress(Bitmap.CompressFormat.PNG, 100, it)
            )
        }
    }

    @Test fun restRemainsVisibleAfterNextAndPreviousUntilExplicitSkip() {
        val initial=WorkoutSnapshotStore.parse("""{"workoutId":"rest216","restEndAt":9999999999999,"exercises":[{"id":"pushup","name":"Fekvőtámasz","loadType":"bodyweight","repUnit":"ism.","sets":[{"setId":"one","reps":"5","done":false}]},{"id":"squat","name":"Guggolás","loadType":"bodyweight","repUnit":"ism.","sets":[{"setId":"two","reps":"5","done":false}]}]}""")!!
        val workout=mutableStateOf<WearWorkout?>(initial);val rest=mutableStateOf(30)
        compose.setContent { Watch { Fixture("workout",externalWorkout=workout,rest216=rest) } }
        compose.onNodeWithTag("wear-rest-ring").assertIsDisplayed()
        compose.onNodeWithContentDescription("Következő gyakorlat").performClick()
        compose.runOnIdle {assertEquals(1,workout.value!!.currentExercise);assertEquals(initial.restEndAt,workout.value!!.restEndAt);rest.value=19}
        compose.onNode(hasText("Pihenő",substring=true)).assertIsDisplayed()
        compose.onNodeWithTag("wear-rest-ring").assertIsDisplayed()
        compose.onNodeWithContentDescription("Előző gyakorlat").performClick()
        compose.runOnIdle {assertEquals(0,workout.value!!.currentExercise);assertEquals(initial.restEndAt,workout.value!!.restEndAt);assertEquals(19,rest.value)}
        compose.onNodeWithContentDescription("Pihenő kihagyása").performClick()
        compose.runOnIdle {assertEquals(0L,workout.value!!.restEndAt);assertEquals(0,rest.value)}
        compose.onNodeWithTag("wear-rest-ring").assertDoesNotExist()
    }

    @Test fun distanceWorkoutExposesDirectGpsStatusAndManagement() {
        val workout=mutableStateOf<WearWorkout?>(WorkoutSnapshotStore.parse("""{"workoutId":"gps-shortcut216","ownershipProtocol193":1,"timerOwner":"watch","exercises":[{"id":"running","name":"Futás","measurementType":"distance","repUnit":"km","loadType":"bodyweight","sets":[{"setId":"run-one","done":false}]}]}""")!!)
        compose.setContent { Watch { Fixture("workout",externalWorkout=workout) } }
        compose.onNodeWithTag("wear-gps-shortcut216").assertIsDisplayed().performClick()
        lazyList().performScrollToNode(hasText(WearGpsText193.text("idle")))
        compose.onNodeWithText(WearGpsText193.text("idle")).assertIsDisplayed()
        lazyList().performScrollToNode(hasContentDescription(WearGpsText193.text("back")))
        compose.onNodeWithContentDescription(WearGpsText193.text("back")).performClick()
        compose.onNodeWithTag("wear-gps-shortcut216").assertIsDisplayed()
    }

    @Test fun gpsStartChoiceIsVisibleRememberedAndCancelNeverEnablesItInFourLanguages() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val language=mutableStateOf("hu");val revision=mutableStateOf(0)
        var starts=0;var cancels=0
        WearMenu216.choose(context,"running",false);WearMenu216.choose(context,"walking",false)
        compose.setContent { Watch { key(language.value,revision.value) {
            GpsStartScreen216("running",{starts++},{cancels++},language.value)
        } } }
        for(lang in listOf("hu","en","de","ro")) {
            compose.runOnIdle {language.value=lang;revision.value++}
            val off=WearMenu216.text("gps",lang)+": "+WearMenu216.text("off",lang)
            val on=WearMenu216.text("gps",lang)+": "+WearMenu216.text("on",lang)
            compose.onNodeWithContentDescription(off).assertIsDisplayed().performClick()
            compose.runOnIdle {assertFalse(WearMenu216.enabled(context,"running"));assertEquals(0,starts)}
            lazyList().performScrollToNode(hasContentDescription(WearMenu216.text("cancel",lang)))
            compose.onNodeWithContentDescription(WearMenu216.text("cancel",lang)).performClick()
            compose.runOnIdle {assertFalse(WearMenu216.enabled(context,"running"));revision.value++}
            compose.onNodeWithContentDescription(off).performClick()
            lazyList().performScrollToNode(hasContentDescription(WearMenu216.text("start",lang)))
            compose.onNodeWithContentDescription(WearMenu216.text("start",lang)).performClick()
            compose.runOnIdle {assertTrue(WearMenu216.enabled(context,"running"));assertFalse(WearMenu216.enabled(context,"walking"));revision.value++}
            compose.onNodeWithContentDescription(on).assertIsDisplayed().performClick()
            lazyList().performScrollToNode(hasContentDescription(WearMenu216.text("start",lang)))
            compose.onNodeWithContentDescription(WearMenu216.text("start",lang)).performClick()
            compose.runOnIdle {assertFalse(WearMenu216.enabled(context,"running"));starts=0}
        }
        compose.runOnIdle {assertEquals(4,cancels)}
        screenshot("gps-start-choice216")
    }

    @Test
    fun menuUsesFullViewportAndFirstLastButtonsAreReachableAndClickable() {
        val clicks = mutableListOf<String>()
        compose.setContent {
            Watch { MainMenuScreen(false, false, { clicks.add("phone") }, { clicks.add(it) }) }
        }
        compose.waitForIdle()
        assertFullViewport()
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
        val middle =
            compose.onNodeWithContentDescription(WearMenu216.text("quick")).fetchSemanticsNode().boundsInRoot
        assertTrue(
            "Centered menu button uses at least 80% of the diameter",
            middle.width >= root.width * .8f,
        )
        val first =
            compose.onNodeWithContentDescription(WearMenu216.text("quick")).fetchSemanticsNode().boundsInRoot
        assertTrue(
            "The first button must start below the curved title",
            first.top >= root.height * 32f / diameter,
        )
        screenshot("menu-initial")
        scrollToStart()
        compose.onNodeWithContentDescription(WearMenu216.text("quick")).assertIsDisplayed().performClick()
        scrollToEnd()
        compose
            .onNodeWithContentDescription(WearMenu216.text("phone"))
            .assertIsDisplayed()
            .performClick()
        assertEquals(listOf("quick", "phone"), clicks)
        screenshot("menu-last-control")
    }

    @Test
    fun everyListRouteHasFullViewportAndItsLastActionIsReachable() {
        val selected = mutableStateOf("home")
        compose.setContent { Watch { key(selected.value) { Fixture(selected.value) } } }
        val routes =
            listOf(
                "home" to "Főmenü",
                "menu" to WearMenu216.text("discard"),
                "journal" to "‹ Főmenü",
                "journal-detail" to "‹ Napló",
                "quick" to "‹ Vissza",
                "metrics" to "‹ Vissza",
                "settings" to "‹ Kezdőlap",
                "days" to "‹ Kezdőlap",
                "editor" to "Mégse",
                "timer" to "‹ Vissza",
                "discard" to "Edzés törlése",
                "finish" to "Edzés befejezése és mentése",
                "summary" to "Kész",
                "workout" to "Edzés befejezése",
                "timed-workout" to "Edzés befejezése",
            )
        for ((route, last) in routes) {
            compose.runOnIdle { selected.value = route }
            compose.waitForIdle()
            assertFullViewport()
            screenshot(route)
            if (route == "workout" || route == "timed-workout") {
                compose.onNodeWithText("93 bpm · 7 kcal").assertIsDisplayed()
                val clockLayouts = mutableListOf<TextLayoutResult>()
                compose.onNodeWithTag("wear-workout-clock").assertIsDisplayed()
                    .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(clockLayouts) }
                assertTrue(clockLayouts.single().layoutInput.text.text.matches(Regex("\\d{2}:\\d{2}")))
                assertFalse("Wall clock must fit alongside the set heading", clockLayouts.single().hasVisualOverflow)
                val caption =
                    compose
                        .onNodeWithText(
                            if (route == "workout") "Rögzítés" else "Mentés",
                            useUnmergedTree = true,
                        )
                        .fetchSemanticsNode()
                        .boundsInRoot
                val dots =
                    compose.onNodeWithTag("wear-page-indicator").fetchSemanticsNode().boundsInRoot
                if (caption.height > 0f)
                    assertFalse(
                        "Pager dots must not paint over the action caption",
                        caption.overlaps(dots),
                    )
            }
            if (route == "home")
                compose.onNodeWithContentDescription("Megnyitás telefonon").assertIsDisplayed()
            scrollToEnd(last)
            compose.onNodeWithContentDescription(last).assertIsDisplayed()
            screenshot("$route-last-control")
        }
        compose.runOnIdle { selected.value = "calendar" }
        compose.waitForIdle()
        assertFullViewport("wear-calendar-week")
        screenshot("calendar-week")
        val today =
            LocalDate.now()
                .format(
                    java.time.format.DateTimeFormatter.ofPattern(
                        "MMMM d., EEEE",
                        java.util.Locale.forLanguageTag("hu"),
                    )
                ) + ", ma"
        compose.onNodeWithContentDescription(today).performClick()
        assertFullViewport()
        scrollToEnd()
        compose.onNodeWithContentDescription("‹ Heti naptár").assertIsDisplayed()
        screenshot("calendar-day")
    }

    @Test
    fun journalDetailsAndAllWorkoutPagerPagesRemainScrollable() {
        val selected = mutableStateOf("journal")
        val liveHealth = mutableStateOf<JSONObject?>(null)
        compose.setContent { Watch { key(selected.value) { Fixture(selected.value, liveHealth) } } }
        lazyList()
            .performScrollToNode(hasContentDescription("Hosszú nevű próbaedzés", substring = true))
        compose
            .onNodeWithContentDescription("Hosszú nevű próbaedzés", substring = true)
            .performClick()
        assertFullViewport()
        scrollToEnd()
        compose.onNodeWithContentDescription("‹ Napló").assertIsDisplayed()
        screenshot("journal-detail-with-metrics")
        compose.runOnIdle { selected.value = "workout" }
        compose.waitForIdle()
        assertFullViewport()
        val actionsBefore =
            compose.onNodeWithTag("wear-fixed-actions").fetchSemanticsNode().boundsInRoot
        compose.runOnIdle {
            liveHealth.value =
                JSONObject()
                    .put("source", "wear_health_services")
                    .put("workoutId", "layout-test")
                    .put("state", "active")
                    .put("updatedAt", System.currentTimeMillis())
                    .put("heartRate", 93)
                    .put("totalCalories", 7.0)
        }
        compose.onNodeWithText("93 bpm · 7 kcal").assertIsDisplayed()
        assertEquals(
            "New measurements must not displace the controls",
            actionsBefore,
            compose.onNodeWithTag("wear-fixed-actions").fetchSemanticsNode().boundsInRoot,
        )
        compose.onNodeWithTag("wear-fixed-body").performTouchInput { swipeDown() }
        assertEquals(
            "Dragging content must not displace the controls",
            actionsBefore,
            compose.onNodeWithTag("wear-fixed-actions").fetchSemanticsNode().boundsInRoot,
        )
        screenshot("workout-strength")
        compose.onNodeWithTag("wear-round-list").performTouchInput { swipeLeft() }
        compose.waitForIdle()
        assertFullViewport()
        screenshot("workout-menu")
        compose.onNodeWithTag("wear-round-list").performTouchInput { swipeLeft() }
        compose.waitForIdle()
        assertFullViewport()
        scrollToEnd("‹ Vissza")
        compose.onNodeWithContentDescription("‹ Vissza").assertIsDisplayed()
        screenshot("workout-measurements")
    }

    @Test
    fun homeStaysCenteredAndOnlyBottomUpSwipeOpensTheMenu() {
        val selected = mutableStateOf("home")
        compose.setContent { Watch { key(selected.value) { Fixture(selected.value) } } }
        val actions = compose.onNodeWithTag("wear-fixed-actions").fetchSemanticsNode().boundsInRoot
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
        val program = compose.onNodeWithTag("wear-home-title").fetchSemanticsNode().boundsInRoot
        assertEquals("The home program title must be centered", root.center.x, program.center.x, 1f)
        compose.onNodeWithContentDescription("Főmenü").assertIsDisplayed()
        if (fontScale == 1f && diameter >= 192f) {
            val text = compose.onNodeWithTag("wear-home-title").fetchSemanticsNode().boundsInRoot
            repeat(2) {
                compose.onNodeWithTag("wear-fixed-body").performTouchInput { swipeUp() }
                compose.onNodeWithTag("wear-fixed-body").performTouchInput { swipeDown() }
            }
            assertEquals(
                "Short home content must not move after vertical swipes",
                text,
                compose.onNodeWithTag("wear-home-title").fetchSemanticsNode().boundsInRoot,
            )
        }
        assertEquals(
            actions,
            compose.onNodeWithTag("wear-fixed-actions").fetchSemanticsNode().boundsInRoot,
        )
        screenshot("home-fixed")
        compose.onNodeWithTag("wear-round-list").performTouchInput { swipeLeft() }
        compose.onNodeWithContentDescription("Edzés indítása").assertIsDisplayed()
        compose.onNodeWithTag("wear-round-list").performTouchInput { swipeRight() }
        compose.onNodeWithContentDescription("Edzés indítása").assertIsDisplayed()
        compose.onNodeWithTag("wear-round-list").performTouchInput { swipeDown() }
        compose.onNodeWithContentDescription("Edzés indítása").assertIsDisplayed()
        compose.onNodeWithTag("wear-round-list").performTouchInput { swipeUp() }
        // Fixture("home") clears the workout: Quick workout is the first menu action.
        compose.onNodeWithContentDescription(WearMenu216.text("quick")).assertIsDisplayed()
    }

    @Test
    fun measurementFailureShowsItsReasonBeforeEmptyMetricsAndKeepsRetryReachable() {
        val reason=WEAR_LOCATION_PERMISSION_MESSAGE
        val data=mutableStateOf<JSONObject?>(JSONObject().put("workoutId","layout-test")
            .put("source","wear_health_services").put("state","error")
            .put("updatedAt",System.currentTimeMillis()).put("message",reason).put("locationPermissionRequired",true))
        val retries=mutableListOf<String>()
        compose.setContent { Watch { Fixture("metrics",liveHealth=data,measurementActions=retries) } }
        // At normal font sizes the reason is in the initial viewport, above retry.
        if(fontScale==1f)compose.onNodeWithTag("wear-health-message").assertIsDisplayed()
        lazyList().performScrollToNode(hasTestTag("wear-health-message"))
        compose.onNodeWithText(reason).assertIsDisplayed()
        screenshot("measurement-failure-reason")
        lazyList().performScrollToNode(hasContentDescription("Aktuális edzés mérése"))
        compose.onNodeWithContentDescription("Aktuális edzés mérése").assertIsDisplayed().performClick()
        assertEquals(listOf("retry"),retries)
        lazyList().performScrollToNode(hasText("Pulzus · utolsó mérés"))
        compose.onNodeWithText("Pulzus · utolsó mérés").assertIsDisplayed()
    }

    @Test
    fun disabledSystemLocationShowsItsOwnSettingsActionAndPreservesMeasuredTotals() {
        val data=mutableStateOf<JSONObject?>(JSONObject().put("workoutId","layout-test")
            .put("source","wear_health_services").put("state","error")
            .put("updatedAt",System.currentTimeMillis()).put("message",WEAR_LOCATION_SERVICES_MESSAGE)
            .put("locationServicesRequired",true).put("locationPermissionRequired",false).put("steps",12))
        val actions=mutableListOf<String>()
        compose.setContent { Watch { Fixture("metrics",liveHealth=data,measurementActions=actions) } }
        lazyList().performScrollToNode(hasTestTag("wear-health-message"))
        compose.onNodeWithText(WEAR_LOCATION_SERVICES_MESSAGE).assertIsDisplayed()
        lazyList().performScrollToNode(hasContentDescription("Helyadatok bekapcsolása"))
        compose.onNodeWithContentDescription("Helyadatok bekapcsolása").assertIsDisplayed().performClick()
        assertEquals(listOf("retry"),actions)
        screenshot("measurement-system-location-off")
        compose.runOnIdle { data.value=wearRetryRecording(data.value!!) }
        compose.onNodeWithText(WEAR_LOCATION_SERVICES_MESSAGE).assertDoesNotExist()
        compose.onNodeWithContentDescription("Helyadatok bekapcsolása").assertDoesNotExist()
        lazyList().performScrollToNode(hasText("12 lépés"))
        compose.onNodeWithText("12 lépés").assertIsDisplayed()
    }

    @Test
    fun homeShortcutsReturnHomeAndDeleteStillRequiresConfirmation() {
        val selected = mutableStateOf("home")
        compose.setContent { Watch { key(selected.value) { Fixture(selected.value) } } }
        compose.onNodeWithContentDescription("Edzésnapló").assertIsDisplayed().performClick()
        scrollToEnd("‹ Kezdőlap")
        compose.onNodeWithContentDescription("‹ Kezdőlap").performClick()
        compose
            .onNodeWithContentDescription("Beállítások")
            .assertIsDisplayed()
            .performClick()
        compose.onNodeWithText("Edzésmérés bekapcsolva").assertIsDisplayed()
        lazyList().performScrollToNode(hasContentDescription("Órás mérések"))
        compose.onNodeWithContentDescription("Órás mérések").performClick()
        scrollToEnd("‹ Vissza")
        compose.onNodeWithContentDescription("‹ Vissza").performClick()
        scrollToEnd("‹ Kezdőlap")
        compose.onNodeWithContentDescription("‹ Kezdőlap").performClick()
        compose.onNodeWithContentDescription("Edzés indítása").assertIsDisplayed()
        compose.runOnIdle { selected.value = "active-home" }
        compose.onNodeWithContentDescription("Edzés folytatása").assertIsDisplayed()
        compose.onNodeWithContentDescription("Megkezdett edzés törlése").performClick()
        compose
            .onNodeWithContentDescription("Vissza, az edzés megőrzése")
            .assertIsDisplayed()
            .performClick()
        compose.onNodeWithContentDescription("Edzés folytatása").assertIsDisplayed()
    }

    @Test
    fun miniJournalKeepsLongNamesAndNotesOnMultipleLines() {
        val selected = mutableStateOf("journal")
        compose.setContent { Watch { key(selected.value) { Fixture(selected.value) } } }
        val note = "Mentett telefonos edzések · offline másolat"
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(note).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertFalse(layouts.single().hasVisualOverflow)
        assertTrue("Long journal note must wrap rather than truncate", layouts.single().lineCount > 1)
        lazyList().performScrollToNode(hasTestTag("wear-journal-entry"))
        val entry = compose.onNodeWithTag("wear-journal-entry")
        screenshot("journal-wrapped-entry")
        entry.performClick()
        val title = "Hosszú nevű próbaedzés a teljes edzésnap nevével és további részletekkel"
        layouts.clear()
        compose.onNodeWithText(title).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertFalse(layouts.single().hasVisualOverflow)
        assertTrue(layouts.single().lineCount > 1)
        screenshot("journal-wrapped-title")
        val exercise = "Földön fekve nyomás hosszú gyakorlatnévvel"
        lazyList().performScrollToNode(hasText(exercise))
        layouts.clear()
        compose.onNodeWithText(exercise).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertFalse(layouts.single().hasVisualOverflow)
        assertTrue(layouts.single().lineCount > 1)
        screenshot("journal-wrapped-exercise")
        scrollToEnd("‹ Napló")
        compose.onNodeWithContentDescription("‹ Napló").performClick()
        compose.onNodeWithText(note).assertExists()
    }

    @Test
    fun homeStatesKeepCircleControlsVisibleAndRespectPendingWorkouts() {
        val variant = mutableStateOf("recommended")
        val set = WearSet(1, "8", "5.5", false, 0, 0, 0.0)
        val exercise =
            WearExercise(
                "squat",
                "Goblet guggolás hosszú magyar gyakorlatnévvel",
                "weight",
                "ism.",
                "strength",
                listOf(set),
            )
        val work =
            WearWorkout(
                1,
                "p",
                "Otthoni A/B – Alap",
                "a",
                "",
                Instant.now().minusSeconds(512).toString(),
                "home-test",
                0,
                60,
                0,
                listOf(exercise),
                "Alap A",
            )
        val day =
            WatchHomeDay(
                "a",
                "Alap A – teljes test edzésnap",
                "p",
                "Otthoni A/B – Alap",
                listOf(
                    WatchHomeExercise(
                        "squat",
                        exercise.name,
                        "weight",
                        "ism.",
                        "strength",
                        "8",
                        listOf(WatchHomeSet(1, "5.5")),
                    )
                ),
            )
        val home =
            WatchHomeSnapshot(
                "p",
                "Otthoni A/B – Alap",
                60,
                false,
                WatchHomeRecommendation("s", "", day),
                listOf(day),
                publishedAt = System.currentTimeMillis(),
            )
        val health =
            JSONObject()
                .put("source", "wear_health_services")
                .put("workoutId", "home-test")
                .put("state", "active")
                .put("updatedAt", System.currentTimeMillis())
                .put("heartRate", 97)
                .put("totalCalories", 7)
                .put("steps", 65)
        compose.setContent {
            Watch {
                key(variant.value) {
                    WearHomeDashboard(
                        if (variant.value == "missing") null
                        else
                            home.copy(
                                hasDraft = variant.value == "draft",
                                theme =
                                    if (variant.value == "green")
                                        WearHomeTheme("green", "#62d08a", "#9cf5ba", "vivid")
                                    else WearHomeTheme.DEFAULT,
                            ),
                        if (variant.value in listOf("active", "rest"))
                            work.copy(
                                restEndAt =
                                    if (variant.value == "rest") System.currentTimeMillis() + 60000
                                    else 0
                            )
                        else null,
                        if (variant.value in listOf("pending", "error"))
                            WearClosure(
                                "c",
                                "home-test",
                                "finishWorkout",
                                variant.value,
                                "",
                                512,
                                1,
                                1,
                            )
                        else null,
                        health,
                        true,
                        variant.value != "offline",
                        67,
                        onStart = { _, _ -> },
                        onResume = {},
                        onDays = {},
                        onResult = {},
                        onJournal = {},
                        onDiscard = {},
                        onSettings = {},
                        onMenu = {},
                        onOpenPhone = {},
                    )
                }
            }
        }
        for (value in
            listOf(
                "recommended",
                "active",
                "rest",
                "pending",
                "error",
                "draft",
                "missing",
                "offline",
                "green",
            )) {
            compose.runOnIdle { variant.value = value }
            assertFullViewport()
            for (description in listOf(
                if (value in listOf("active", "rest")) "Megkezdett edzés törlése" else "Edzésnapló",
                "Beállítások",
            )) {
                val bounds =
                    compose
                        .onNodeWithContentDescription(description)
                        .assertIsDisplayed()
                        .fetchSemanticsNode()
                        .boundsInRoot
                val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
                assertEquals(
                    "Whole circular 48dp control",
                    root.width * 48f / diameter,
                    bounds.height,
                    1f,
                )
                assertEquals(bounds.width, bounds.height, 1f)
            }
            compose.onNodeWithContentDescription("Főmenü").assertIsDisplayed()
            compose.onNodeWithContentDescription("Megnyitás telefonon").assertIsDisplayed()
            if (value in listOf("pending", "error"))
                compose
                    .onNodeWithContentDescription("Edzés mentésének állapota")
                    .assertIsDisplayed()
            if (value !in listOf("active", "rest"))
                compose.onNodeWithText("97 bpm · 7 kcal").assertDoesNotExist()
            if (value in listOf("active", "rest")) {
                val titleLayouts = mutableListOf<TextLayoutResult>()
                val title =
                    if (value == "rest" && diameter < 210f) "Pihenő · ${exercise.name}"
                    else exercise.name
                compose.onNodeWithText(title).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(titleLayouts) }
                assertTrue("Exercise names use two lines", titleLayouts.single().lineCount > 1)
                assertFalse("The full exercise name fits", titleLayouts.single().hasVisualOverflow)
            }
            screenshot("home-revamp-$value")
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rotaryChangesOnlyTheOpenedFieldAndNeverSavesOrRetargetsImplicitly() {
        val initial=WorkoutSnapshotStore.parse("""{"workoutId":"rotary-ui212","exercises":[{"id":"press","repUnit":"ism.","loadType":"weight","sets":[{"setId":"one","reps":"5","weight":"5.5","done":false},{"setId":"two","reps":"8","done":false}]}]}""")!!
        val workout=mutableStateOf(initial)
        val field=mutableStateOf("weight")
        val revision=mutableStateOf(0)
        val changes=mutableListOf<Pair<String,Double>>()
        var backs=0
        compose.setContent { Watch { key(field.value,revision.value) {
            ValueEditor(workout.value,field.value,{name,delta->changes.add(name to delta)},{backs++})
        } } }
        val detent=android.view.ViewConfiguration.get(InstrumentationRegistry.getInstrumentation().targetContext).scaledVerticalScrollFactor
        fun rotate(pixels: Float) { compose.onNodeWithTag("wear-value-editor").performRotaryScrollInput { rotateToScrollVertically(pixels) } }
        rotate(detent)
        compose.onNodeWithTag("wear-edited-value").assertTextEquals("6")
        compose.runOnIdle { assertTrue(changes.isEmpty()) }
        rotate(-detent)
        compose.onNodeWithTag("wear-edited-value").assertTextEquals("5,5")
        compose.onNodeWithContentDescription("Súly növelése").performClick()
        compose.onNodeWithContentDescription("✓ Mentés").performClick()
        compose.runOnIdle { assertEquals(listOf("weight" to .5),changes) }
        compose.runOnIdle { field.value="reps" }
        rotate(detent)
        compose.onNodeWithTag("wear-edited-value").assertTextEquals("6")
        compose.onNodeWithContentDescription("Mégse").performClick()
        compose.runOnIdle { assertEquals(1,changes.size);assertEquals(2,backs);revision.value++ }
        compose.onNodeWithTag("wear-edited-value").assertTextEquals("5")
        rotate(-detent*20)
        compose.onNodeWithTag("wear-edited-value").assertTextEquals("0")
        // A later automatic/manual set change cannot redirect the open editor.
        compose.runOnIdle { workout.value=initial.copy(exercises=initial.exercises.map{it.copy(sets=it.sets.mapIndexed{i,v->v.copy(done=i==0)})}) }
        compose.onNodeWithContentDescription("✓ Mentés").assertIsNotEnabled()
        rotate(detent)
        compose.onNodeWithTag("wear-edited-value").assertTextEquals("0")
        compose.runOnIdle { assertEquals(1,changes.size) }
        screenshot("rotary-editor-stale212")
    }

    @Test
    fun editorsKeepMinusAndPlusBesideTheValueAndSaveCancelVisible() {
        val field = mutableStateOf("weight")
        val revision = mutableStateOf(0)
        val changes = mutableListOf<Pair<String, Double>>()
        var backs = 0
        val set = WearSet(1, "5", "5.5", false, 0, 0, 0.0)
        val workout =
            WearWorkout(
                1,
                "p",
                "Erősítés",
                "a",
                "",
                Instant.now().toString(),
                "editor-layout",
                0,
                60,
                0,
                listOf(
                    WearExercise(
                        "press",
                        "Földön fekve nyomás",
                        "weight",
                        "ism.",
                        "strength",
                        listOf(set),
                    )
                ),
                "A nap",
            )
        compose.setContent {
            Watch {
                key(field.value, revision.value) {
                    ValueEditor(
                        workout,
                        field.value,
                        { name, delta -> changes.add(name to delta) },
                        { backs++ },
                    )
                }
            }
        }
        for ((name, title, step) in
            listOf(Triple("weight", "Súly", .5), Triple("reps", "Ismétlés", 1.0))) {
            compose.runOnIdle { field.value = name }
            compose.onNodeWithContentDescription("✓ Mentés").assertIsDisplayed()
            compose.onNodeWithContentDescription("Mégse").assertIsDisplayed()
            val plus = compose.onNodeWithContentDescription("$title növelése")
            val minus = compose.onNodeWithContentDescription("$title csökkentése")
            val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
            for (button in listOf(plus, minus)) {
                val bounds = button.assertIsDisplayed().fetchSemanticsNode().boundsInRoot
                assertEquals(
                    "The whole 48dp editor target must be visible",
                    root.width * 48f / diameter,
                    bounds.height,
                    1f,
                )
                assertEquals(
                    "The editor target must not be clipped",
                    bounds.width,
                    bounds.height,
                    1f,
                )
            }
            if (fontScale == 1f) {
                val value =
                    compose.onNodeWithTag("wear-edited-value").fetchSemanticsNode().boundsInRoot
                val left = minus.fetchSemanticsNode().boundsInRoot
                val right = plus.fetchSemanticsNode().boundsInRoot
                assertTrue(left.right <= value.left && value.right <= right.left)
                assertTrue(kotlin.math.abs(left.center.y - right.center.y) < 1f)
                assertTrue(value.center.y in left.top..left.bottom)
            }
            plus.performClick()
            minus.performClick()
            plus.performClick()
            assertTrue("Editing alone must not save", changes.none { it.first == name })
            screenshot("editor-$name-fixed")
            compose.onNodeWithContentDescription("✓ Mentés").performClick()
            assertEquals(name to step, changes.last())
            val savedCount = changes.size
            compose.runOnIdle { revision.value++ }
            plus.performClick()
            compose.onNodeWithContentDescription("Mégse").performClick()
            assertEquals("Cancel must not save changes", savedCount, changes.size)
        }
        assertEquals(4, backs)
    }

    @Test
    fun automaticTimedWorkKeepsSmallModeClockAndBottomControlsVisible() {
        val commands=mutableListOf<String>()
        compose.setContent { Watch { Fixture("automatic-timed",automaticCommands=commands) } }
        screenshot("automatic-timed-ready")
        val toggle=compose.onNodeWithTag("wear-automatic-toggle").assertIsDisplayed()
        val width=toggle.fetchSemanticsNode().boundsInRoot.width
        val root=compose.onRoot().fetchSemanticsNode().boundsInRoot
        assertTrue("Mode is a compact secondary control",width<root.width*.16f)
        compose.onNodeWithTag("wear-workout-clock").assertIsDisplayed()
        compose.onNodeWithTag("wear-automatic-clock").assertIsDisplayed().performClick()
        assertEquals(listOf("start"),commands)
        compose.onNodeWithContentDescription("Edzés befejezése").assertIsDisplayed()
        compose.onNodeWithText("Munka · 1/3. kör").assertIsDisplayed()
        toggle.performClick();assertEquals("toggle",commands.last())
    }

    @Test
    fun automaticPausedRestKeepsResumeBetweenBottomButtons() {
        val commands=mutableListOf<String>()
        compose.setContent { Watch { Fixture("automatic-rest",automaticCommands=commands) } }
        val resume=compose.onNodeWithContentDescription("Automata folytatása").assertIsDisplayed()
        val progress=compose.onNodeWithTag("wear-rest-ring").fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current
        assertEquals("Two-minute rest is already shrinking above one minute",115f/120,progress,.00001f)
        val root=compose.onRoot().fetchSemanticsNode().boundsInRoot
        val button=resume.fetchSemanticsNode().boundsInRoot
        assertEquals(root.center.x,button.center.x,1f)
        assertTrue("Resume remains in lower controls",button.center.y>root.center.y)
        compose.onNodeWithTag("wear-workout-clock").assertIsDisplayed()
        screenshot("automatic-paused-rest")
        resume.performClick();assertEquals(listOf("resume"),commands)
    }

    @Test
    fun touchLockBlocksAccidentalActionsAndUnlocksWithOneThreeSecondHold() {
        val actions=mutableListOf<String>()
        compose.setContent { Watch { Fixture("touch-workout",touchActions=actions) } }
        compose.onNodeWithTag("wear-touch-lock").assertIsDisplayed().performClick()
        compose.onNodeWithTag("wear-touch-locked").assertIsDisplayed()
        compose.onNodeWithText("93 bpm · 7 kcal").assertIsDisplayed()
        for(label in listOf("Sorozat rögzítése","Edzés befejezése","Előző gyakorlat","Pihenő kihagyása"))
            compose.onNodeWithContentDescription(label).assertDoesNotExist()
        compose.onRoot().performTouchInput { click(center);swipeLeft();swipeUp();doubleClick(center) }
        val unlock=compose.onNodeWithTag("wear-touch-unlock").assertIsDisplayed()
        unlock.performTouchInput { down(center);advanceEventTime(800);up();advanceEventTime(100);down(center);advanceEventTime(800);up() }
        compose.onNodeWithTag("wear-touch-locked").assertIsDisplayed()
        unlock.performTouchInput { down(center);moveTo(center+androidx.compose.ui.geometry.Offset(35f,0f));advanceEventTime(3200);up() }
        compose.onNodeWithTag("wear-touch-locked").assertIsDisplayed()
        unlock.performTouchInput { down(0,center);down(1,center);advanceEventTime(3200);up(1);up(0) }
        compose.onNodeWithTag("wear-touch-locked").assertIsDisplayed()
        assertTrue(actions.isEmpty());screenshot("touch189-locked")
        unlock.performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(2700)
        compose.onNodeWithTag("wear-touch-locked").assertIsDisplayed()
        compose.mainClock.advanceTimeBy(500)
        // No UP has been sent: the hold alone must already have restored the workout.
        compose.onNodeWithTag("wear-touch-locked").assertDoesNotExist()
        compose.onRoot().performTouchInput { up() }
        assertTrue("Release cannot click through to the restored workout",actions.isEmpty())
        compose.onNodeWithContentDescription("Sorozat rögzítése").assertIsDisplayed().performClick()
        assertEquals(listOf("record"),actions)
    }

    @Test
    fun touchLockMenuReloadAndPhoneUpdatesStayBoundToOneWorkout() {
        val work=mutableStateOf<WearWorkout?>(touchWorkout189())
        val generation=mutableStateOf(0)
        compose.setContent { Watch { key(generation.value) { Fixture("touch-workout",externalWorkout=work) } } }
        compose.onNodeWithContentDescription("Edzésmenü",substring=true).performClick()
        val title=WearTouchCopy189.language(java.util.Locale.getDefault().language).lock
        lazyList().performScrollToNode(hasContentDescription(title))
        compose.onNodeWithContentDescription(title).performClick()
        compose.runOnIdle { generation.value++ }
        compose.onNodeWithTag("wear-touch-locked").assertIsDisplayed()
        compose.runOnIdle { work.value=work.value!!.copy(currentExercise=1) }
        compose.onNodeWithTag("wear-locked-clock").assertTextEquals("2/2 · 1/1")
        compose.onNodeWithTag("wear-touch-locked").assertIsDisplayed()
        compose.runOnIdle { work.value=work.value!!.copy(workoutId="touch-next",started=Instant.now().toString()) }
        compose.onNodeWithTag("wear-touch-locked").assertDoesNotExist()
        compose.onNodeWithTag("wear-touch-lock").assertIsDisplayed()
    }

    @Test
    fun touchLockKeepsTheExistingManualStopwatchVisibleAndRunning() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val initial=touchWorkout189().copy(exercises=touchWorkout189().exercises.map { it.copy(repUnit="mp") })
        val work=mutableStateOf<WearWorkout?>(initial)
        val key="${initial.workoutId}:${initial.exercise!!.id}:1:reps"
        val timer=WearStopwatch(key,8000L,System.currentTimeMillis()-4000L)
        WearStopwatchStore.save(context,timer)
        try {
            compose.setContent { Watch { Fixture("touch-workout",externalWorkout=work) } }
            compose.onNodeWithTag("wear-touch-lock").performClick()
            val before=compose.onNodeWithTag("wear-locked-clock").fetchSemanticsNode().config[SemanticsProperties.Text].single().text
            Thread.sleep(1200)
            compose.mainClock.advanceTimeBy(1500)
            compose.waitForIdle()
            val after=compose.onNodeWithTag("wear-locked-clock").fetchSemanticsNode().config[SemanticsProperties.Text].single().text
            fun seconds(value: String)=value.substringBefore(':').toInt()*60+value.substringAfter(':').toInt()
            assertTrue(seconds(before)>=12);assertTrue(seconds(after)>seconds(before))
            assertEquals(timer,WearStopwatchStore.load(context,key))
            compose.onNodeWithTag("wear-touch-locked").assertIsDisplayed()
            screenshot("touch189-manual-stopwatch")
        }finally{WearStopwatchStore.clear(context)}
    }

    @Test
    fun touchLockInstructionsFitFourLanguagesOnEveryRoundScreen() {
        val language=mutableStateOf("hu")
        val work=touchWorkout189()
        val measured=JSONObject().put("workoutId",work.workoutId).put("source","wear_health_services")
            .put("state","active").put("updatedAt",System.currentTimeMillis()).put("heartRate",93).put("totalCalories",7)
        compose.setContent { Watch { WearTouchLockedScreen189(work,57,measured,WearTouchCopy189.language(language.value),{}) } }
        for(lang in listOf("hu","en","de","ro")){
            compose.runOnIdle { language.value=lang }
            compose.onNodeWithTag("wear-touch-unlock").assertIsDisplayed()
            compose.onNodeWithTag("wear-locked-clock").assertIsDisplayed().assertTextEquals("0:57")
            compose.onNodeWithText("93 bpm · 7 kcal").assertIsDisplayed()
            val layouts=mutableListOf<TextLayoutResult>()
            compose.onNodeWithTag("wear-touch-hint").assertIsDisplayed().performSemanticsAction(SemanticsActions.GetTextLayoutResult){it(layouts)}
            assertFalse("Unlock instructions fit $lang",layouts.single().hasVisualOverflow)
            screenshot("touch189-$lang")
        }
    }

    private fun touchWorkout189(): WearWorkout {
        val exercise=WearExercise("press","Földön fekve nyomás","weight","ism.","strength",listOf(WearSet(1,"5","5.5",false,0,0,0.0,"touch-set")))
        return WearWorkout(1,"p","Program","a","", "2026-10-16T10:00:00Z","layout-test",0,60,0,
            listOf(exercise,exercise.copy(id="squat",name="Guggolás",sets=listOf(exercise.sets[0].copy(setId="touch-set2")))))
    }

    @Test
    fun quickUndoHasReachableControlAndNeverFinishesWorkout() {
        val actions=mutableListOf<String>()
        compose.setContent { Watch { Fixture("undo-quick",automaticCommands=actions) } }
        compose.onNodeWithContentDescription("Utolsó sorozat visszavonása").assertIsDisplayed().performClick()
        assertEquals(listOf("record188"),actions)
        compose.onNodeWithContentDescription("Sorozat rögzítése").assertIsDisplayed()
        assertFullViewport();screenshot("undo188-restored")
    }
    @Test
    fun expiredQuickUndoRemainsReachableInWorkoutMenuWithLargeText() {
        val actions=mutableListOf<String>()
        compose.setContent { Watch { Fixture("undo-menu",automaticCommands=actions) } }
        compose.onNodeWithContentDescription("Utolsó sorozat visszavonása").assertDoesNotExist()
        compose.onNodeWithContentDescription("Edzésmenü",substring=true).performClick()
        lazyList().performScrollToNode(hasContentDescription("Utolsó sorozat visszavonása"))
        compose.onNodeWithContentDescription("Utolsó sorozat visszavonása").assertIsDisplayed().performClick()
        assertEquals(listOf("record188"),actions);assertFullViewport();screenshot("undo188-menu")
    }
    @Test
    fun phoneStartedManualWorkoutHasUndoAndWaitsForItsReceipt() {
        val actions=mutableListOf<String>()
        compose.setContent { Watch { Fixture("undo-phone",automaticCommands=actions) } }
        compose.onNodeWithContentDescription("Utolsó sorozat visszavonása").assertIsDisplayed().performClick()
        assertEquals(listOf("record188"),actions)
        compose.onNodeWithContentDescription("Visszavonás egyeztetése").assertIsDisplayed()
        compose.onNodeWithContentDescription("Sorozat rögzítése").assertDoesNotExist()
        compose.onNodeWithText("93 bpm · 7 kcal").assertIsDisplayed()
        assertFullViewport();screenshot("undo188-phone-started")
    }

    @Composable
    private fun Fixture(route: String, liveHealth: State<JSONObject?>? = null, automaticCommands: MutableList<String>? = null, feedbackAnswers: MutableList<String?>? = null, measurementActions: MutableList<String>? = null, touchActions: MutableList<String>? = null, externalWorkout: androidx.compose.runtime.MutableState<WearWorkout?>? = null, rest216: androidx.compose.runtime.MutableState<Int>? = null) {
        val set = WearSet(1, "5", "5.5", feedbackAnswers != null, 30, 0, 0.0,if(route.startsWith("undo-"))"s188" else "")
        val exercise =
            WearExercise(
                "press",
                "Földön fekve nyomás hosszú gyakorlatnévvel",
                "weight",
                if (route == "automatic-timed") "mp" else if (route == "timed-workout") "mp/oldal" else "ism.",
                "strength",
                listOf(set),
            )
        val localWorkout = remember {
            mutableStateOf<WearWorkout?>(
                WearWorkout(
                    1,
                    "p",
                    "Erősítés",
                    "a",
                    "",
                    Instant.now().minusSeconds(60).toString(),
                    "layout-test",
                    0,
                    60,
                    0,
                    listOf(exercise),
                    "A nap",
                ).let { initial ->
                    if(route.startsWith("undo-"))WearUndo188.capture(initial,initial.copy(timerOwner=if(route=="undo-phone")"phone" else "watch",exercises=listOf(exercise.copy(sets=listOf(set.copy(done=true))))),0,0,"record188",System.currentTimeMillis()-if(route=="undo-menu")9000L else 0L)
                    else if (route=="workout-paused") initial.copy(workoutPaused=true,pauseRevision=1)
                    else if (route=="phone-rest-finished") projectPhoneAutomatic(initial,
                        JSONObject().put("restOnly",true).put("steps",org.json.JSONArray()),
                        WearAutomatic(enabled=true,owner="phone",phase="complete",paused=true,key="phone-rest",remainingMs=0))
                    else if (route.startsWith("automatic-")) initial.copy(timerOwner="watch",
                        automation=WearAutomatic(enabled=true,key="layout-auto",phase=if(route=="automatic-rest")"rest" else "ready",
                            paused=route=="automatic-rest",remainingMs=if(route=="automatic-rest")115000 else 0,
                            workSeconds=30,restSeconds=if(route=="automatic-rest")120 else 5,rounds=3,anchorMs=android.os.SystemClock.elapsedRealtime()))
                    else initial
                }
            )
        }
        val workout=externalWorkout ?: localWorkout
        val quickExercise =
            WatchHomeExercise(
                "press",
                exercise.name,
                "weight",
                "ism.",
                "strength",
                "5",
                listOf(WatchHomeSet(1, "5.5")),
            )
        val day = WatchHomeDay("a", "A nap — teljes test", "p", "Erősítés", listOf(quickExercise))
        val journal =
            MiniJournalWorkout(
                "journal-layout",
                "2026-10-12T06:00:00Z",
                "2026-10-12T06:10:00Z",
                "Hosszú nevű próbaedzés a teljes edzésnap nevével és további részletekkel",
                false,
                600,
                listOf(
                    MiniJournalExercise(
                        exercise.name,
                        listOf(MiniJournalSet(1, "5", 5.5, null, null, null)),
                    )
                ),
                MiniJournalMetric(
                    "wear_health_services",
                    averageHeartRate = 93.0,
                    maxHeartRate = 110.0,
                    totalCalories = 12.0,
                    steps = 65.0,
                    activeDurationSeconds = 600.0,
                ),
                null,
            )
        val home = remember {
            mutableStateOf<WatchHomeSnapshot?>(
                WatchHomeSnapshot(
                    "p",
                    "Erősítés",
                    60,
                    false,
                    WatchHomeRecommendation("", "", day),
                    listOf(day),
                    listOf(WatchCalendarEntry(LocalDate.now().toString(), "", "", "planned", day)),
                    listOf(quickExercise),
                    listOf(journal),
                    "1.3.4",
                    true,
                    pendingFeedback = if (route == "phone-feedback") WatchWorkoutFeedback("layout-test", workout.value!!.started) else null,
                )
            )
        }
        val closure = remember {
            mutableStateOf<WearClosure?>(
                WearClosure("c", "layout-test", "finishWorkout", "saved", "", 60, 1, 1)
            )
        }
        val request = remember {
            mutableStateOf<String?>(
                if (route == "timed-workout" || route.startsWith("automatic-") || route in listOf("phone-rest-finished","workout-paused") || route.startsWith("undo-") || route=="touch-workout") "workout"
                else if (route == "active-home") "home" else route
            )
        }
        val health =
            JSONObject()
                .put("workoutId", "layout-test")
                .put("source", "wear_health_services")
                .put("state", "active")
                .put("updatedAt", System.currentTimeMillis())
                .put("heartRate", 93)
                .put("totalCalories", 7.0)
                .put("averageHeartRate", 90.0)
                .put("maxHeartRate", 110.0)
                .put("steps", 65)
                .put("activeDurationSeconds", 60.0)
        if (route == "quick" || route == "home" || route == "phone-feedback") workout.value = null
        TrainPilotWearApp(
            touchClock189 = { compose.mainClock.currentTime },
            workout = workout,
            home = home,
            closure = closure,
            restRemaining = rest216 ?: mutableStateOf(if(route=="automatic-rest")115 else 0),
            onStart = { _, _ -> },
            onStartQuick = {},
            onAddQuick = {},
            onChange = { _, _ -> },
            onUndo = if(!route.startsWith("undo-"))null else ({ id ->
                automaticCommands?.add(id)
                workout.value=WearUndo188.inverse(workout.value!!,"ui-undo",android.os.SystemClock.elapsedRealtime(),-1)
            }),
            onComplete = { touchActions?.add("record");if(route=="phone-rest-finished")automaticCommands?.add("record") },
            onPrevious = {if(rest216!=null)workout.value=workout.value?.selectExercise((workout.value?.currentExercise ?: 0)-1)},
            onNext = {if(rest216!=null)workout.value=workout.value?.selectExercise((workout.value?.currentExercise ?: 0)+1)},
            onSelectExercise = {},
            onSkipRest = {if(rest216!=null){rest216.value=0;workout.value=workout.value?.endRest()}},
            onFinish = {
                if (feedbackAnswers != null) {
                    closure.value = closure.value?.copy(status = "pending", started = workout.value!!.started, needsFeedback = true)
                    workout.value = null
                }
            },
            onWorkoutFeedback = if (feedbackAnswers == null) null else ({ _, answer ->
                feedbackAnswers.add(answer)
                home.value = home.value?.copy(pendingFeedback = null)
                closure.value = closure.value?.copy(needsFeedback = false)
            }),
            onDiscard = {},
            onResumeWorkout = {},
            onRetry = {},
            onDismissClosure = {},
            onOpenPhone = { if(route=="workout-paused")automaticCommands?.add("openPhone") },
            onRefreshJournal = {},
            health = liveHealth ?: mutableStateOf(health),
            measuring = mutableStateOf(true),
            gpsEnabled = mutableStateOf(false),
            requestedRoute = request,
            onRouteConsumed = { request.value = null },
            onEnableHealth = { measurementActions?.add("retry") },
            onDisableHealth = {},
            onToggleGps = {},
            onToggleAutomatic = if(automaticCommands==null)null else ({automaticCommands.add("toggle")}),
            onAutomaticCommand = if(automaticCommands==null)null else ({automaticCommands.add(it)}),
        )
    }
}
