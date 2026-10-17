package com.repforge.app.wear

import android.content.Context
import android.os.SystemClock
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** A single inverse operation, never a second workout history or a whole-draft rollback. */
object WearUndo188 {
    const val QUICK_MILLIS = 8000L
    fun values(set: WearSet) = JSONObject().put("done",set.done).put("reps",set.reps).put("weight",set.weight)
        .put("leftSeconds",set.leftSeconds).put("rightSeconds",set.rightSeconds).put("sideState192",set.sideState192?.let { JSONObject(it) } ?: JSONObject.NULL).put("distanceMeters",set.distanceMeters)
    fun guard(workout: WearWorkout): JSONArray = JSONArray().also { rows ->
        workout.exercises.forEach { e -> e.sets.forEach { s -> rows.put(JSONArray().put(e.id).put(s.setId)
            .put(s.done).put(number(s.reps)).put(number(s.weight)).put(s.leftSeconds.toDouble())
            .put(s.rightSeconds.toDouble()).put(s.distanceMeters)) } }
    }
    private fun number(value: String) = value.replace(',','.').toDoubleOrNull() ?: 0.0
    fun same(left: JSONArray?, right: JSONArray?): Boolean {
        if(left==null||right==null||left.length()!=right.length())return false
        return (0 until left.length()).all { i ->
            val a=left.optJSONArray(i);val b=right.optJSONArray(i)
            a!=null&&b!=null&&a.length()==8&&b.length()==8&&(0..2).all { a.opt(it)==b.opt(it) }&&
                (3..7).all { a.optDouble(it,Double.NaN)==b.optDouble(it,Double.NaN) }
        }
    }
    fun capture(before: WearWorkout,after: WearWorkout,e: Int,s: Int,recordId: String,now: Long): WearWorkout {
        val exercise=before.exercises.getOrNull(e) ?: return after
        val set=exercise.sets.getOrNull(s) ?: return after
        if(set.done||recordId.isBlank()||set.setId.isBlank())return after.copy(lastRecord188=null)
        val record=JSONObject().put("recordId",recordId).put("workoutId",before.workoutId)
            .put("native188",nativePhoneExecution(before,"completeSet",exercise,null))
            .put("exerciseId",exercise.id).put("setId",set.setId).put("before",values(set))
            .put("expected",guard(after)).put("recordedAt",now)
        return after.copy(lastRecord188=record.toString())
    }
    fun record(workout: WearWorkout): JSONObject? = runCatching { JSONObject(workout.lastRecord188 ?: return null) }.getOrNull()
    fun marker(workout: WearWorkout): JSONObject? = runCatching { JSONObject(workout.undo188 ?: return null) }.getOrNull()
    fun blocked(workout: WearWorkout) = marker(workout)?.optString("status")=="conflict" ||
        workout.timerOwner=="phone"&&marker(workout)?.optString("status")=="pending"
    fun available(workout: WearWorkout): Boolean {
        if(workout.workoutPaused||blocked(workout))return false
        val r=record(workout) ?: return false
        if(r.optString("workoutId")!=workout.workoutId||!same(r.optJSONArray("expected"),guard(workout)))return false
        return workout.exercises.any { e -> e.id==r.optString("exerciseId")&&e.sets.any { it.setId==r.optString("setId")&&it.done } }
    }
    fun quick(workout: WearWorkout,now: Long) = available(workout)&&now-(record(workout)?.optLong("recordedAt") ?: 0) in 0..QUICK_MILLIS
    fun inverse(current: WearWorkout,id: String,now: Long,boot: Int): WearWorkout? {
        if(!available(current))return null
        val r=record(current)!!;val before=r.getJSONObject("before")
        val e=current.exercises.indexOfFirst { it.id==r.getString("exerciseId") }
        val s=current.exercises[e].sets.indexOfFirst { it.setId==r.getString("setId") }
        // Account real elapsed time before pausing; a newly changed/completed next set invalidates the inverse.
        val timed=if(current.automation?.enabled==true&&current.automation.owner=="watch")
            WearAutomaticEngine.tick(current,now,boot).workout else current
        if(!same(r.getJSONArray("expected"),guard(timed)))return null
        val old=timed.exercises[e].sets[s]
        val restored=old.copy(done=false,reps=before.optString("reps"),weight=before.optString("weight"),
            leftSeconds=before.optInt("leftSeconds"),rightSeconds=before.optInt("rightSeconds"),distanceMeters=before.optDouble("distanceMeters",0.0),sideState192=before.optJSONObject("sideState192")?.toString())
        val next=timed.copy(currentExercise=e,restEndAt=0,lastRecord188=null,
            exercises=timed.exercises.mapIndexed { i,ex -> if(i!=e)ex else ex.copy(sets=ex.sets.mapIndexed { j,set -> if(j==s)restored else set }) },
            automation=timed.automation?.let { a -> a.copy(phase="ready",paused=true,interrupted=false,remainingMs=0,
                deadlineAt=0,anchorMs=now,boot=boot,phaseNumber=a.phaseNumber+1,targetExercise=e,targetSet=s,targetId=old.setId,
                key=if(a.owner=="watch")a.key else "undo188:$id",baseSeconds=number(restored.reps).toInt(),awaitingPhone=current.timerOwner=="phone") })
        return next.copy(undo188=JSONObject().put("commandId",id).put("recordId",r.getString("recordId"))
            .put("status","pending").put("after",guard(next)).toString())
    }
    fun undo(context: Context,expectedRecordId: String): WearWorkout? {
        synchronized(WearAutomaticController) { synchronized(WorkoutSnapshotStore) {
            val current=WorkoutSnapshotStore.load(context) ?: return null
            val r=record(current) ?: return current
            if(r.optString("recordId")!=expectedRecordId)return current
            val id=UUID.randomUUID().toString()
            val next=inverse(current,id,SystemClock.elapsedRealtime(),WearAutomaticService.boot(context)) ?: return current
            val command=JSONObject(r.toString()).put("schema",1).put("commandId",id).put("action","undoSet")
                .put("createdAt",System.currentTimeMillis()).put("watchSequence",current.localSequence)
                .put("generation",current.generation).put("executionRevision",current.executionRevision)
                .put("automation",next.automation?.json()).put("pauseRevision",current.pauseRevision)
            marker(current)?.let { command.put("undoDependency188",it.optString("commandId")) }
            if(r.optBoolean("native188")&&current.nativeExecutionProtocol==2&&current.timerOwner=="phone")command.put("nativeExecutionProtocol",2)
            // Persist inverse and its marker before transport; the outbox remains the only delivery queue.
            val durable=next.copy(localSequence=current.localSequence+1,undo188=JSONObject(next.undo188!!).put("command",command).toString())
            WorkoutSnapshotStore.saveWorkout(context,durable)
            val seq=WearCommandOutbox.enqueue(context,command)
            WorkoutSnapshotStore.saveWorkout(context,durable.copy(localSequence=maxOf(durable.localSequence,seq)))
            WearStopwatchStore.clear(context)
            context.getSystemService(android.os.Vibrator::class.java)?.cancel()
            if(current.timerOwner=="watch")WearAutomaticController.publish(context,WorkoutSnapshotStore.load(context)!!)
            WearAutomaticService.nudge();WearSurfaces.refresh(context,true)
            return WorkoutSnapshotStore.load(context)
        } }
    }
    fun recover(context: Context) {
        val current=WorkoutSnapshotStore.load(context) ?: return
        val m=marker(current) ?: return
        if(m.optString("status")!="pending")return
        val prefs=context.getSharedPreferences("trainpilot_wear_outbox",Context.MODE_PRIVATE)
        val results=runCatching { JSONArray(prefs.getString("results191","[]")) }.getOrNull() ?: return
        for(i in 0 until results.length())results.optJSONObject(i)?.takeIf { it.optString("commandId")==m.optString("commandId") }?.let { receipt(context,it);return }
        m.optJSONObject("command")?.let { WearCommandOutbox.enqueue(context,it) }
    }
    /** Receipt correction is identity matched and cannot erase edits made after an offline undo. */
    fun receipt(context: Context,receipt: JSONObject) {
        if(receipt.optString("action")!="undoSet")return
        synchronized(WearAutomaticController) { synchronized(WorkoutSnapshotStore) {
            val current=WorkoutSnapshotStore.load(context) ?: return
            val m=marker(current) ?: return
            if(current.workoutId!=receipt.optString("workoutId")||m.optString("commandId")!=receipt.optString("commandId")||m.optString("status")!="pending")return
            val accepted=receipt.optString("status")=="applied"
            if(accepted)WorkoutSnapshotStore.saveWorkout(context,current.copy(undo188=m.put("status","applied").toString(),
                automation=current.automation?.copy(awaitingPhone=false)))
            else {
                val authoritative=receipt.optJSONObject("undoFrame")?.let { WorkoutSnapshotStore.parse(it.toString()) }
                if(authoritative!=null&&authoritative.workoutId==current.workoutId&&same(m.optJSONArray("after"),guard(current)))
                    WorkoutSnapshotStore.saveWorkout(context,authoritative.copy(localSequence=maxOf(current.localSequence,receipt.optLong("sequence")),lastRecord188=null,
                        undo188=null,automation=authoritative.automation?.copy(paused=true,deadlineAt=0)))
                else WorkoutSnapshotStore.saveWorkout(context,current.copy(lastRecord188=null,undo188=m.put("status","conflict").toString(),
                    automation=current.automation?.copy(paused=true,deadlineAt=0,awaitingPhone=false)))
            }
        } }
    }
}
