package com.repforge.app.wear

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class WearSaveSyncTest {
    private fun command(id:String="record",seq:Long=1,workout:String="workout")=JSONObject()
        .put("commandId",id).put("workoutId",workout).put("action","completeSet").put("sequence",seq)
        .put("reps","10").put("weight","5.5")
    private fun receipt(command:JSONObject,status:String="applied")=JSONObject()
        .put("schema",1).put("commandId",command.getString("commandId")).put("workoutId",command.getString("workoutId"))
        .put("action",command.getString("action")).put("sequence",command.getLong("sequence"))
        .put("status",status).put("receivedAt",9000)
    @Test fun transportSuccessNeverMeansPhoneSavedAndSurvivesReload() {
        val pending=JSONArray().put(command())
        assertEquals(WearSavePhase.LOCAL,WearSaveSyncModel.view(pending,JSONArray(),"workout",10000)!!.phase)
        pending.getJSONObject(0).put("_sent191",1000)
        val reloaded=JSONArray(pending.toString())
        assertEquals(WearSavePhase.WAITING,WearSaveSyncModel.view(reloaded,JSONArray(),"workout",10000)!!.phase)
        val expired=WearSaveSyncModel.view(reloaded,JSONArray(),"workout",61000)!!
        assertEquals(WearSavePhase.ERROR,expired.phase);assertTrue(expired.retryable);assertEquals("timeout",expired.reason)
        assertEquals("10",reloaded.getJSONObject(0).getString("reps"))
    }
    @Test fun receiptRequiresEveryIdentityFieldAndKnownStatus() {
        val c=command();val pending=JSONArray().put(c)
        for((field,value) in listOf("schema" to 2,"commandId" to "other","workoutId" to "other","action" to "discardWorkout","sequence" to 2,"status" to "unknown")) {
            assertNull(field,WearSaveSyncModel.accept(pending,JSONArray(),receipt(c).put(field,value)))
            assertEquals(1,pending.length())
        }
        val accepted=WearSaveSyncModel.accept(pending,JSONArray(),receipt(c))!!
        assertEquals(0,accepted.first.length())
        assertEquals(WearSavePhase.SYNCED,WearSaveSyncModel.view(accepted.first,accepted.second,"workout",10000)!!.phase)
        assertTrue(WearSaveSyncModel.sameReceipt(receipt(c),JSONObject(receipt(c).toString())))
    }
    @Test fun partialReceiptsForeignWorkoutsAndRejectedOperationsStayVisible() {
        val first=command("first");val second=command("second",2);val foreign=command("foreign",3,"another")
        val pending=JSONArray().put(first).put(second).put(foreign)
        val one=WearSaveSyncModel.accept(pending,JSONArray(),receipt(first,"error"))!!
        assertEquals(WearSavePhase.LOCAL,WearSaveSyncModel.view(one.first,one.second,"workout",10000)!!.phase)
        val two=WearSaveSyncModel.accept(one.first,one.second,receipt(second))!!
        val failed=WearSaveSyncModel.view(two.first,two.second,"workout",10000)!!
        assertEquals(WearSavePhase.ERROR,failed.phase);assertFalse(failed.retryable);assertEquals("error",failed.reason)
        assertEquals(1,two.first.length());assertEquals("foreign",two.first.getJSONObject(0).getString("commandId"))
        assertEquals(WearSavePhase.LOCAL,WearSaveSyncModel.view(two.first,two.second,"another",10000)!!.phase)
    }
    @Test fun lateRejectionDoesNotOverrideSavedTerminalSnapshot() {
        val old=command();val finish=command("finish",2).put("action","finishWorkout")
        val saved=WearSaveSyncModel.accept(JSONArray().put(old).put(finish),JSONArray(),receipt(finish,"saved"))!!
        val late=WearSaveSyncModel.accept(saved.first,saved.second,receipt(old,"superseded"))!!
        assertEquals(WearSavePhase.SYNCED,WearSaveSyncModel.view(late.first,late.second,"workout",10000)!!.phase)
        assertNull(WearSaveSyncModel.accept(late.first,late.second,receipt(old)))
    }
    @Test fun receiptCacheIsBoundedAndLegacyProcessedIsNotSuccess() {
        var results=JSONArray()
        for(i in 1L..300L) {val c=command("record-$i",i);results=WearSaveSyncModel.accept(JSONArray().put(c),results,receipt(c))!!.second}
        assertEquals(256,results.length())
        val c=command("legacy",301);val result=WearSaveSyncModel.accept(JSONArray().put(c),results,receipt(c,"processed"))!!
        assertEquals(WearSavePhase.ERROR,WearSaveSyncModel.view(result.first,result.second,"workout",10000)!!.phase)
    }
    @Test fun supportedLanguagesHaveDistinctLabelsForLocalAndConfirmedSaving() {
        for(language in listOf("hu","en","de","ro")) {
            assertNotEquals(wearSaveLabel(WearSavePhase.LOCAL,language),wearSaveLabel(WearSavePhase.SYNCED,language))
            assertNotEquals(wearSaveLabel(WearSavePhase.WAITING,language),wearSaveLabel(WearSavePhase.SYNCED,language))
        }
    }
}
