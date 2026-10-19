package com.repforge.app.wear

import com.repforge.shared.IntervalTimeline193
import org.json.JSONArray
import org.json.JSONObject

/** Runs a frozen phone plan on the existing watch timer owner, using local monotonic time. */
internal object WearTransferredTimer193 {
    private data class Plan(val checkpoint: JSONObject,val timer: JSONObject,val rows: JSONArray,val timeline: IntervalTimeline193)
    private fun read(workout: WearWorkout): Plan {
        val raw=workout.timerCheckpoint193 ?: error("Missing transferred timer")
        require(raw.toByteArray(Charsets.UTF_8).size<=96000)
        val c=JSONObject(raw);require(c.getInt("schema")==1)
        val t=c.getJSONObject("timer");require(t.getString("workoutId")==workout.workoutId)
        val rows=t.getJSONArray("steps");require(rows.length() in 1..100)
        val ids=mutableSetOf<String>()
        val steps=(0 until rows.length()).map { i ->
            val row=rows.getJSONObject(i);val e=workout.exercises.indexOfFirst { it.id==row.getString("exerciseId") }
            require(e>=0&&workout.exercises[e].sets.any { it.setId==row.getString("setId") }&&ids.add(row.getString("setId")))
            require(row.optString("gpsKey").isEmpty())
            IntervalTimeline193.Step(row.getString("exerciseId"),row.getString("setId"),row.optString("name"),row.getInt("work"),row.getInt("rest"),row.getInt("rounds"),"")
        }
        val timeline=IntervalTimeline193(steps,t.getBoolean("autoAdvance"),t.optInt("initialRest"),t.getBoolean("restOnly"))
        val position=t.getLong("elapsedMs");val p=timeline.at(position)
        require(position==p.elapsedMs&&t.getString("phase")==p.phase&&t.getLong("remainingMs")==p.remainingMs&&t.getInt("round")==p.round&&t.getInt("stepIndex")==p.step)
        val ledger=t.getJSONObject("ledger");val parts=ledger.getJSONArray("steps")
        require(parts.length()==rows.length()&&ledger.getLong("positionMs")==position)
        var work=0L;var rest=0L;var paused=0L
        for(i in 0 until rows.length()) {
            val row=rows.getJSONObject(i);val part=parts.getJSONObject(i)
            require(row.getString("setId")==part.getString("setId")&&row.getString("exerciseId")==part.getString("exerciseId"))
            val w=part.getLong("workMs");val r=part.getLong("restMs");val pause=part.getLong("pausedMs")
            require(w in 0..IntervalTimeline193.MAX_MS&&r in 0..IntervalTimeline193.MAX_MS&&pause in 0..604800000L)
            work+=w;rest+=r;paused+=pause
            val base=row.optDouble("baseSeconds",0.0);require(base.isFinite()&&base in 0.0..86400.0)
        }
        require(work==ledger.getLong("workMs")&&rest==ledger.getLong("restMs")&&paused==ledger.getLong("pausedMs")&&work+rest<=position)
        return Plan(c,t,rows,timeline)
    }
    fun install(workout: WearWorkout,now: Long,boot: Int,key: String): WearWorkout {
        val plan=read(workout);val t=plan.timer;val p=plan.timeline.at(t.getLong("elapsedMs"));val row=plan.rows.getJSONObject(p.step)
        val e=workout.exercises.indexOfFirst { it.id==row.getString("exerciseId") }
        val s=workout.exercises[e].sets.indexOfFirst { it.setId==row.getString("setId") }
        val logs=plan.checkpoint.optJSONObject("timeLogs193") ?: JSONObject()
        if(!logs.has(t.getString("key")))logs.put(t.getString("key"),JSONObject(t.getJSONObject("ledger").toString()).put("key",t.getString("key")))
        plan.checkpoint.put("timeLogs193",logs).put("continueAutomatic193",workout.automation?.enabled==true).put("started193",false)
        val automatic=WearAutomatic(enabled=true,owner="watch",key=key,phase=p.phase,paused=true,
            workSeconds=row.getInt("work"),restSeconds=row.getInt("rest"),rounds=row.getInt("rounds"),round=p.round,
            vibration=t.optBoolean("vibration",true),targetExercise=e,targetSet=s,targetId=row.getString("setId"),remainingMs=p.remainingMs,
            anchorMs=now,boot=boot,phaseNumber=t.optLong("phaseNumber")+1)
        return workout.copy(automation=automatic,timerCheckpoint193=plan.checkpoint.toString(),restEndAt=0)
    }
    private fun project(plan: Plan,position: Long) {
        val t=plan.timer;val p=plan.timeline.at(position)
        t.put("elapsedMs",p.elapsedMs).put("phase",p.phase).put("stepIndex",p.step).put("round",p.round)
            .put("completed",p.completed).put("stepElapsedMs",p.stepElapsedMs).put("remainingMs",p.remainingMs).put("totalMs",p.totalMs)
        t.getJSONObject("ledger").put("positionMs",p.elapsedMs).put("owner","timeline193")
    }
    fun tick(workout: WearWorkout,now: Long,boot: Int): WearAutomaticTransition {
        val a=workout.automation ?: return WearAutomaticTransition(workout)
        if(workout.workoutPaused)return WearAutomaticTransition(workout)
        if(a.boot!=boot||now<a.anchorMs)return WearAutomaticTransition(WearAutomaticEngine.interrupt(workout,a.anchorMs))
        return try { advance(workout,read(workout),now) }catch(_:Exception) {
            WearAutomaticTransition(WearAutomaticEngine.interrupt(workout,a.anchorMs))
        }
    }
    private fun advance(workout: WearWorkout,plan: Plan,now: Long): WearAutomaticTransition {
        val a=workout.automation!!;val t=plan.timer;val ledger=t.getJSONObject("ledger");val parts=ledger.getJSONArray("steps")
        val delta=now-a.anchorMs;var position=t.getLong("elapsedMs");val before=plan.timeline.at(position)
        var work=0L;var rest=0L;var paused=0L
        if(a.paused) {
            if(!a.interrupted&&plan.checkpoint.optBoolean("started193")) {
                paused=delta;val part=parts.getJSONObject(before.step);part.put("pausedMs",part.getLong("pausedMs")+paused)
            }
        } else {
            plan.checkpoint.put("started193",true)
            val end=(position+delta).coerceAtMost(before.totalMs)
            while(position<end) {
                val phase=plan.timeline.at(position);val ms=minOf(phase.remainingMs,end-position)
                check(ms>0)
                val part=parts.getJSONObject(phase.step);val field=if(phase.phase=="work")"workMs" else "restMs"
                part.put(field,part.getLong(field)+ms)
                if(field=="workMs")work+=ms else rest+=ms
                position+=ms
            }
        }
        ledger.put("workMs",ledger.getLong("workMs")+work).put("restMs",ledger.getLong("restMs")+rest).put("pausedMs",ledger.getLong("pausedMs")+paused)
        project(plan,position);val p=plan.timeline.at(position);val row=plan.rows.getJSONObject(p.step)
        val e=workout.exercises.indexOfFirst { it.id==row.getString("exerciseId") };val s=workout.exercises[e].sets.indexOfFirst { it.setId==row.getString("setId") }
        var automatic=a.copy(phase=p.phase,remainingMs=p.remainingMs,anchorMs=now,round=p.round,workSeconds=row.getInt("work"),
            restSeconds=row.getInt("rest"),rounds=row.getInt("rounds"),targetExercise=e,targetSet=s,targetId=row.getString("setId"),
            phaseNumber=a.phaseNumber+if(p.token!=before.token)1 else 0,workMs=a.workMs+work,restMs=a.restMs+rest,pausedMs=a.pausedMs+paused)
        val logs=plan.checkpoint.getJSONObject("timeLogs193");logs.put(a.key,automatic.json().getJSONObject("timeLog"))
        var current=workout.copy(automation=automatic,timerCheckpoint193=plan.checkpoint.toString(),currentExercise=e)
        var completedExercise: Int?=null;var completedSet: Int?=null
        if(t.getBoolean("autoAdvance")&&!t.getBoolean("restOnly")) {
            // Follow the source plan's measured work+rest recording semantics and original baseline.
            for(i in 0 until minOf(p.completed+1,plan.rows.length())) {
                val owner=plan.rows.getJSONObject(i);val ei=current.exercises.indexOfFirst { it.id==owner.getString("exerciseId") }
                val si=current.exercises[ei].sets.indexOfFirst { it.setId==owner.getString("setId") };val old=current.exercises[ei].sets[si]
                if(old.done)continue
                val part=parts.getJSONObject(i)
                val seconds=kotlin.math.floor(owner.optDouble("baseSeconds",0.0)+(part.getLong("workMs")+part.getLong("restMs"))/1000.0).toLong()
                val done=i<p.completed
                current=current.copy(exercises=current.exercises.mapIndexed { ex,exercise -> if(ex!=ei)exercise else exercise.copy(sets=exercise.sets.mapIndexed { set,value -> if(set!=si)value else value.copy(reps=seconds.toString(),done=done) }) })
                if(done){completedExercise=ei;completedSet=si}
            }
        }
        if(p.phase=="complete") {
            val enabled=plan.checkpoint.optBoolean("continueAutomatic193")
            automatic=automatic.copy(phase=if(t.getBoolean("restOnly"))"rest" else "work",paused=false)
            current=current.copy(timerCheckpoint193=null,automation=automatic)
            current=if(!enabled)current.copy(automation=automatic.copy(enabled=false,phase="complete",remainingMs=0))
                else if(t.getBoolean("restOnly"))WearAutomaticEngine.ready(current,now,a.boot)
                else WearAutomaticEngine.afterRecord(current,now,a.boot)
        }
        return WearAutomaticTransition(current,completedExercise,completedSet)
    }
    fun skipRest(workout: WearWorkout,now: Long,boot: Int): WearWorkout {
        var current=tick(workout,now,boot).workout
        val a=current.automation ?: return current
        if(a.phase!="rest"||a.interrupted||current.timerCheckpoint193==null)return current
        return try {
            val plan=read(current);val p=plan.timeline.at(plan.timer.getLong("elapsedMs"))
            project(plan,p.elapsedMs+p.remainingMs)
            current=current.copy(timerCheckpoint193=plan.checkpoint.toString(),automation=a.copy(paused=false,anchorMs=now,phaseNumber=a.phaseNumber+1))
            // Advance at the same monotonic instant: skipped rest adds no measured milliseconds.
            advance(current,plan,now).workout
        }catch(_:Exception){WearAutomaticEngine.interrupt(current,a.anchorMs)}
    }
}
