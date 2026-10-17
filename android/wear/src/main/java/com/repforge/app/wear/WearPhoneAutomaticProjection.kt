package com.repforge.app.wear

import org.json.JSONObject

internal fun projectPhoneAutomatic(current: WearWorkout,value: JSONObject,a: WearAutomatic): WearWorkout {
    var workout=current.copy(automation=a,restEndAt=0)
    if(value.optBoolean("stopped"))return workout
    // A finished phone clock is idle, not paused. Older phone builds publish
    // paused=!running even for completion; never offer Resume for that clock.
    if(a.phase=="complete"&&a.remainingMs==0L&&!a.interrupted)
        workout=workout.copy(automation=a.copy(paused=false))
    val rows=value.optJSONArray("steps") ?: return workout
    val currentStep=value.optInt("stepIndex")
    var selected=current.currentExercise
    for(i in 0 until rows.length()) {
        val row=rows.optJSONObject(i) ?: continue
        val id=row.optString("setId");if(id.isBlank())continue
        val e=workout.exercises.indexOfFirst { it.id==row.optString("exerciseId")&&it.sets.any { set->set.setId==id } };if(e<0)continue
        val s=workout.exercises[e].sets.indexOfFirst { it.setId==id }
        if(i==currentStep)selected=e
        if(!value.optBoolean("autoAdvance")||value.optBoolean("restOnly")||i>currentStep)continue
        val completed=i<value.optInt("completed")
        val full=(row.optLong("work")*row.optLong("rounds")+row.optLong("rest")*(row.optLong("rounds")-1)+if(i<rows.length()-1)row.optLong("rest")else 0)*1000
        val ms=if(completed)full else value.optLong("stepElapsedMs")
        val old=workout.exercises[e].sets[s]
        if(row.has("bilateral192")){
            // Virtual elapsed includes skipped rest. Only the native measured ledger
            // may contribute side work or a completed bilateral set.
            val part=value.optJSONObject("ledger")?.optJSONArray("steps")?.optJSONObject(i) ?: continue
            val c=WearBilateralConfig192.parse(row.getJSONObject("bilateral192")) ?: continue
            val left=part.optLong("leftWorkMs");val right=part.optLong("rightWorkMs");val rounds=row.optInt("rounds",1)
            val l=(row.optLong("baseLeftSeconds")+left/1000).toInt();val r=(row.optLong("baseRightSeconds")+right/1000).toInt()
            val state=JSONObject().put("schema",1).put("key",a.key).put("skipped",part.optBoolean("skippedWork192"))
                .put("left",if(left>=row.optLong("targetLeftMs192",c.leftSeconds*1000L*rounds))"complete" else if(left>0)"partial" else "pending")
                .put("right",if(right>=row.optLong("targetRightMs192",c.rightSeconds*1000L*rounds))"complete" else if(right>0)"partial" else "pending")
                .put("timeLogKey",a.key)
            val update=old.copy(leftSeconds=l,rightSeconds=r,reps=minOf(l,r).toString(),sideState192=state.toString(),
                done=old.done||!part.optBoolean("skippedWork192")&&left>=row.optLong("targetLeftMs192",c.leftSeconds*1000L*rounds)&&right>=row.optLong("targetRightMs192",c.rightSeconds*1000L*rounds))
            workout=workout.copy(exercises=workout.exercises.mapIndexed { ei,ex->if(ei!=e)ex else ex.copy(sets=ex.sets.mapIndexed { si,set->if(si==s)update else set }) })
            continue
        }
        val updated=old.copy(reps=(row.optLong("baseSeconds")+ms/1000).coerceIn(0,86400).toString(),done=old.done||completed)
        workout=workout.copy(exercises=workout.exercises.mapIndexed { ei,ex->if(ei!=e)ex else ex.copy(sets=ex.sets.mapIndexed { si,set->if(si!=s)set else updated }) })
    }
    if(a.phase=="complete"&&value.optBoolean("autoAdvance")&&workout.exercises.getOrNull(selected)?.sets?.all { it.done }==true)
        selected=(selected+1).coerceAtMost(workout.exercises.lastIndex)
    val next=WearAutomaticEngine.next(workout.copy(currentExercise=selected))
    val normalized=workout.automation ?: a
    return workout.copy(currentExercise=selected,automation=if(a.phase=="complete"&&next!=null)normalized.copy(phase="ready")else normalized)
}
