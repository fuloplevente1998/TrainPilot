package com.repforge.app.wear

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class PerSetRir229Test {
    private fun sample() = WearWorkout(1L,"p","P","A","","2026-10-20T08:00:00Z","w",0,60,0,
        listOf(WearExercise("press","Press","total","ism.","",listOf(
            WearSet(1,"10","10",true,0,0,0.0,"s1",0),
            WearSet(2,"10","10",true,0,0,0.0,"s2",6),
            WearSet(3,"","10",false,0,0,0.0,"s3")),actualRir=8,setRir229=true)),
        timerOwner="watch")

    @Test fun roundTripKeepsDistinctObservationsAndLegacyWithoutInventingMissingValue() {
        val original=sample()
        val encoded=WorkoutSnapshotStore.encode(original)
        val sets=encoded.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets")
        assertEquals(0,sets.getJSONObject(0).getInt("actualRir"))
        assertEquals(6,sets.getJSONObject(1).getInt("actualRir"))
        assertFalse(sets.getJSONObject(2).has("actualRir"))
        assertEquals(original,WorkoutSnapshotStore.parse(encoded.toString()))
    }

    @Test fun invalidAndMissingAreNeverCoercedToZero() {
        for(value in listOf<Any>(JSONObject.NULL,-1,11,2.5,"bad")) {
            val json=WorkoutSnapshotStore.encode(sample())
            json.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(0).put("actualRir",value)
            assertNull(WorkoutSnapshotStore.parse(json.toString())!!.exercises[0].sets[0].actualRir)
        }
        val legacy=WorkoutSnapshotStore.encode(sample())
        val exercise=legacy.getJSONArray("exercises").getJSONObject(0)
        exercise.remove("setRir229")
        for(i in 0 until exercise.getJSONArray("sets").length())exercise.getJSONArray("sets").getJSONObject(i).remove("actualRir")
        val parsed=WorkoutSnapshotStore.parse(legacy.toString())!!
        assertEquals(8,parsed.exercises[0].actualRir)
        assertFalse(parsed.exercises[0].setRir229)
        assertTrue(parsed.exercises[0].sets.all { it.actualRir==null })
    }

    @Test fun undoClearsOnlyObservationOfUndoneCompletion() {
        val after=sample()
        val exercise=after.exercises[0]
        val before=after.copy(exercises=listOf(exercise.copy(sets=exercise.sets.mapIndexed { i,s -> if(i==1)s.copy(done=false,actualRir=null)else s })))
        val captured=WearUndo188.capture(before,after,0,1,"record",1000)
        assertTrue(WearUndo188.available(captured))
        val undone=WearUndo188.inverse(captured,"undo",2000,1)!!
        assertFalse(undone.exercises[0].sets[1].done)
        assertNull(undone.exercises[0].sets[1].actualRir)
        assertEquals(0,undone.exercises[0].sets[0].actualRir)
        assertEquals(8,undone.exercises[0].actualRir)
    }
}
