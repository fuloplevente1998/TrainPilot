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
        val updated=old.copy(reps=(row.optLong("baseSeconds")+ms/1000).coerceIn(0,86400).toString(),done=old.done||completed)
        workout=workout.copy(exercises=workout.exercises.mapIndexed { ei,ex->if(ei!=e)ex else ex.copy(sets=ex.sets.mapIndexed { si,set->if(si!=s)set else updated }) })
    }
    if(a.phase=="complete"&&value.optBoolean("autoAdvance")&&workout.exercises.getOrNull(selected)?.sets?.all { it.done }==true)
        selected=(selected+1).coerceAtMost(workout.exercises.lastIndex)
    val next=WearAutomaticEngine.next(workout.copy(currentExercise=selected))
    val normalized=workout.automation ?: a
    return workout.copy(currentExercise=selected,automation=if(a.phase=="complete"&&next!=null)normalized.copy(phase="ready")else normalized)
}
