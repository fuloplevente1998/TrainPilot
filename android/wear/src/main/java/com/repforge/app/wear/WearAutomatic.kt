package com.repforge.app.wear

import org.json.JSONObject
import org.json.JSONArray

/** Durable monotonic state. Only the owner is allowed to advance an automatic workout. */
data class WearAutomatic(
    val enabled: Boolean = false,
    val owner: String = "watch",
    val phase: String = "ready",
    val paused: Boolean = false,
    val workSeconds: Int = 30,
    val restSeconds: Int = 90,
    val vibration: Boolean = true,
    val key: String = "",
    val targetExercise: Int = 0,
    val targetSet: Int = 0,
    val targetId: String = "",
    val remainingMs: Long = 0,
    val anchorMs: Long = 0,
    val boot: Int = -1,
    val phaseNumber: Long = 0,
    val baseSeconds: Int = 0,
    val interrupted: Boolean = false,
    val deadlineAt: Long = 0,
    val rounds: Int = 1,
    val round: Int = 1,
    val handledCommands: List<String> = emptyList()
) {
    val counting get() = enabled && phase in listOf("work", "rest")
    fun remaining(now: Long): Long = if(owner=="phone"&&counting&&!paused&&deadlineAt>0)(deadlineAt-System.currentTimeMillis()).coerceAtLeast(0)
        else (remainingMs - if (counting && !paused) (now-anchorMs).coerceAtLeast(0) else 0).coerceAtLeast(0)
    fun json() = JSONObject().put("enabled",enabled).put("owner",owner).put("phase",phase)
        .put("paused",paused).put("workSeconds",workSeconds).put("restSeconds",restSeconds)
        .put("vibration",vibration).put("key",key).put("targetExercise",targetExercise)
        .put("targetSet",targetSet).put("targetId",targetId).put("remainingMs",remainingMs)
        .put("anchorMs",anchorMs).put("boot",boot).put("phaseNumber",phaseNumber)
        .put("baseSeconds",baseSeconds).put("interrupted",interrupted).put("deadlineAt",deadlineAt).put("rounds",rounds).put("round",round).put("handledCommands",JSONArray(handledCommands))
    companion object {
        fun parse(root: JSONObject?): WearAutomatic? {
            root ?: return null
            if(root.optString("owner","watch") !in listOf("watch","phone"))return null
            if(root.optString("phase","ready") !in listOf("ready","work","rest","complete"))return null
            return WearAutomatic(root.optBoolean("enabled"),root.optString("owner","watch"),root.optString("phase","ready"),
                root.optBoolean("paused"),root.optInt("workSeconds",30).coerceIn(1,3600),
                root.optInt("restSeconds",90).coerceIn(0,3600),root.optBoolean("vibration",true),root.optString("key"),
                root.optInt("targetExercise"),root.optInt("targetSet"),root.optString("targetId"),
                root.optLong("remainingMs").coerceIn(0,3600000),root.optLong("anchorMs"),root.optInt("boot",-1),
                root.optLong("phaseNumber").coerceAtLeast(0),root.optInt("baseSeconds").coerceAtLeast(0),root.optBoolean("interrupted"),root.optLong("deadlineAt"),root.optInt("rounds",1).coerceIn(1,100),root.optInt("round",1).coerceIn(1,100),
                root.optJSONArray("handledCommands")?.let { array -> (0 until array.length()).takeLast(128).map { array.optString(it) } } ?: emptyList())
        }
    }
}

data class WearAutomaticTransition(val workout: WearWorkout, val completedExercise: Int? = null, val completedSet: Int? = null)

object WearAutomaticEngine {
    fun timed(exercise: WearExercise?) = exercise?.repUnit?.trim()?.lowercase() == "mp"
    fun targetId(workout: WearWorkout, exercise: Int, set: Int): String = workout.exercises.getOrNull(exercise)?.sets?.getOrNull(set)?.let {
        it.setId.ifBlank { "${workout.workoutId}:$exercise:${it.number}" }
    }.orEmpty()
    fun next(workout: WearWorkout, from: Int = workout.currentExercise): Pair<Int,Int>? {
        for(e in from.coerceAtLeast(0)..workout.exercises.lastIndex) {
            val s=workout.exercises[e].sets.indexOfFirst { !it.done }
            if(s>=0)return e to s
        }
        return null
    }
    private fun validTarget(workout: WearWorkout, a: WearAutomatic) =
        targetId(workout,a.targetExercise,a.targetSet).let { it.isNotBlank() && it==a.targetId } &&
            workout.exercises.getOrNull(a.targetExercise)?.sets?.getOrNull(a.targetSet)?.done==false
    private fun set(workout: WearWorkout, e: Int, s: Int, value: WearSet): WearWorkout = workout.copy(
        exercises=workout.exercises.mapIndexed { ei,ex -> if(ei!=e)ex else ex.copy(sets=ex.sets.mapIndexed { si,old -> if(si==s)value else old }) })
    private fun phase(workout: WearWorkout, a: WearAutomatic, target: Pair<Int,Int>, phase: String, seconds: Int, now: Long, boot: Int): WearWorkout {
        val current=workout.exercises[target.first].sets[target.second]
        return workout.copy(restEndAt=0,automation=a.copy(phase=phase,paused=false,interrupted=false,
            targetExercise=target.first,targetSet=target.second,targetId=targetId(workout,target.first,target.second),
            remainingMs=seconds*1000L,anchorMs=now,boot=boot,phaseNumber=a.phaseNumber+1,
            baseSeconds=if(phase=="work")current.reps.toIntOrNull()?.coerceAtLeast(0) ?: 0 else 0))
    }
    fun ready(workout: WearWorkout, now: Long, boot: Int): WearWorkout {
        val a=workout.automation ?: return workout
        val target=next(workout) ?: return workout.copy(restEndAt=0,automation=a.copy(phase="complete",remainingMs=0,paused=false,phaseNumber=a.phaseNumber+1))
        val selected=workout.copy(currentExercise=target.first)
        return if(timed(selected.exercise))phase(selected,a,target,"work",a.workSeconds,now,boot)
        else phase(selected,a,target,"ready",0,now,boot)
    }
    fun enable(workout: WearWorkout, key: String, work: Int, rest: Int, vibration: Boolean, now: Long, boot: Int): WearWorkout {
        require(key.isNotBlank() && work in 1..3600 && rest in 0..3600)
        require(workout.exercises.sumOf { it.sets.count { set -> !set.done } }.toLong()*(work+rest)<=86400)
        // Enabling does not start an unrequested timed set. Its Start action is explicit.
        val target=next(workout)
        val a=WearAutomatic(enabled=true,key=key,workSeconds=work,restSeconds=rest,vibration=vibration,
            targetExercise=target?.first ?: workout.currentExercise,targetSet=target?.second ?: 0,
            targetId=target?.let { targetId(workout,it.first,it.second) }.orEmpty(),anchorMs=now,boot=boot,
            phase=if(target==null)"complete" else "ready")
        return workout.copy(restEndAt=0,automation=a)
    }
    fun afterRecord(workout: WearWorkout, now: Long, boot: Int): WearWorkout {
        val a=workout.automation ?: return workout
        if(!a.enabled||a.owner!="watch")return workout
        val target=next(workout) ?: return workout.copy(restEndAt=0,automation=a.copy(phase="complete",remainingMs=0,paused=false,phaseNumber=a.phaseNumber+1))
        if(a.restSeconds==0)return ready(workout.copy(automation=a.copy(round=1)),now,boot)
        return phase(workout,a.copy(round=1),target,"rest",a.restSeconds,now,boot)
    }
    fun interrupt(workout: WearWorkout, now: Long): WearWorkout {
        val a=workout.automation ?: return workout
        return workout.copy(restEndAt=0,automation=a.copy(remainingMs=a.remaining(now),anchorMs=now,paused=true,interrupted=true))
    }
    fun tick(workout: WearWorkout, now: Long, boot: Int): WearAutomaticTransition {
        val a=workout.automation ?: return WearAutomaticTransition(workout)
        if(!a.enabled||a.owner!="watch"||!a.counting||a.paused)return WearAutomaticTransition(workout)
        if(a.boot!=boot||now<a.anchorMs||!validTarget(workout,a))return WearAutomaticTransition(interrupt(workout,now))
        val remaining=a.remaining(now)
        var current=workout
        if(a.phase=="work") {
            val seconds=a.baseSeconds+((a.workSeconds*1000L-remaining).coerceIn(0,a.workSeconds*1000L)/1000).toInt()
            val previous=current.exercises[a.targetExercise].sets[a.targetSet]
            current=set(current,a.targetExercise,a.targetSet,previous.copy(reps=seconds.toString()))
        }
        if(remaining>0)return WearAutomaticTransition(current.copy(automation=a.copy(remainingMs=remaining,anchorMs=now)))
        // Advance once, rather than inventing multiple sets after a delayed/restarted callback.
        if(a.phase=="rest")return WearAutomaticTransition(ready(current,now,boot))
        if(a.round<a.rounds){
            val next=a.copy(round=a.round+1)
            return WearAutomaticTransition(if(a.restSeconds==0)phase(current,next,a.targetExercise to a.targetSet,"work",a.workSeconds,now,boot)
                else phase(current,next,a.targetExercise to a.targetSet,"rest",a.restSeconds,now,boot))
        }
        val previous=current.exercises[a.targetExercise].sets[a.targetSet]
        current=set(current,a.targetExercise,a.targetSet,previous.copy(done=true))
        return WearAutomaticTransition(afterRecord(current,now,boot),a.targetExercise,a.targetSet)
    }
    fun pause(workout: WearWorkout, now: Long, boot: Int): WearWorkout {
        val current=tick(workout,now,boot).workout;val a=current.automation ?: return current
        return current.copy(automation=a.copy(paused=true,anchorMs=now))
    }
    fun resume(workout: WearWorkout, now: Long, boot: Int): WearWorkout {
        val a=workout.automation ?: return workout
        if(!a.enabled||a.owner!="watch")return workout
        return workout.copy(automation=a.copy(paused=false,interrupted=false,anchorMs=now,boot=boot))
    }
    fun skipRest(workout: WearWorkout, now: Long, boot: Int): WearWorkout =
        if(workout.automation?.phase=="rest" && workout.automation.owner=="watch")ready(workout,now,boot)else workout
    fun disable(workout: WearWorkout, now: Long, boot: Int): WearWorkout {
        val current=tick(workout,now,boot).workout
        return current.copy(restEndAt=0,automation=current.automation?.copy(enabled=false,paused=false,phase="ready",remainingMs=0,phaseNumber=(current.automation?.phaseNumber ?: 0)+1))
    }
}

/** Last three seconds and one continuous second at a boundary, with no replay on resume. */
class WearAutomaticHaptics {
    private var token=""
    private var second=-1L
    private var quietUntil=0L
    fun next(a: WearAutomatic, now: Long): Long {
        if(!a.enabled||a.owner!="watch"||a.paused||a.interrupted)return 0
        val current="${a.key}:${a.phaseNumber}:${a.phase}"
        val remaining=(a.remaining(now)+999)/1000
        if(current!=token) {
            val changed=token.isNotEmpty();token=current;second=remaining
            val duration=if(changed)1000L else if(a.counting&&remaining in 1..3)80L else 0L
            quietUntil=now+duration
            return duration
        }
        if(a.counting && remaining in 1..3 && remaining!=second) {
            second=remaining
            if(now>=quietUntil){quietUntil=now+80;return 80}
        }
        return 0
    }
}
