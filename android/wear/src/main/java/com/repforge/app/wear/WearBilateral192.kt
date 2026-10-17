package com.repforge.app.wear

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class WearBilateralConfig192(val first: String="left",val leftSeconds: Int=30,val rightSeconds: Int=30,val sideRestSeconds: Int=0,val setRestSeconds: Int=90) {
    init { require(first in listOf("left","right")&&leftSeconds in 1..3600&&rightSeconds in 1..3600&&sideRestSeconds in 0..3600&&setRestSeconds in 0..3600) }
    fun second()=if(first=="left")"right" else "left"
    fun work(side: String)=if(side=="left")leftSeconds else rightSeconds
    fun json()=JSONObject().put("schema",1).put("first",first).put("leftSeconds",leftSeconds).put("rightSeconds",rightSeconds).put("sideRestSeconds",sideRestSeconds).put("setRestSeconds",setRestSeconds)
    companion object {
        fun parse(value: JSONObject?): WearBilateralConfig192? {
            value ?: return null
            val schema=value.get("schema");require((schema is Int||schema is Long)&&(schema as Number).toLong()==1L)
            fun seconds(field: String,min: Int=0,default: Int?=null): Int {
                if(!value.has(field)&&default!=null)return default
                val n=value.get(field);require(n is Int||n is Long);return (n as Number).toLong().also { require(it in min.toLong()..3600L) }.toInt()
            }
            return WearBilateralConfig192(value.getString("first"),seconds("leftSeconds",1),seconds("rightSeconds",1),seconds("sideRestSeconds"),seconds("setRestSeconds",default=90))
        }
    }
}

internal object WearBilateralSettings192 {
    fun load(context: Context,id: String): WearBilateralConfig192?=runCatching { context.getSharedPreferences("trainpilot_bilateral192",Context.MODE_PRIVATE).getString(id,null)?.let { WearBilateralConfig192.parse(JSONObject(it)) } }.getOrNull()
    fun save(context: Context,id: String,value: WearBilateralConfig192)=context.getSharedPreferences("trainpilot_bilateral192",Context.MODE_PRIVATE).edit().putString(id,value.json().toString()).commit()
}

internal object WearBilateral192 {
    fun isSide(ex: WearExercise?)=ex?.repUnit?.replace(" ","")?.lowercase()=="mp/oldal"
    fun config(ex: WearExercise?,a: WearAutomatic)=ex?.bilateral192 ?: WearBilateralConfig192(leftSeconds=a.workSeconds,rightSeconds=a.workSeconds,setRestSeconds=a.restSeconds)
    fun skipped(set: WearSet)=set.sideState192?.let { runCatching { JSONObject(it).optBoolean("skipped") }.getOrDefault(false) } ?: false
    fun account(workout: WearWorkout,a: WearAutomatic,work: Long,rest: Long,pause: Long,skip: Boolean=false): WearWorkout {
        val ex=workout.exercises.getOrNull(a.targetExercise) ?: return workout
        if(!isSide(ex)||a.side192.isEmpty())return workout
        val old=ex.sets.getOrNull(a.targetSet) ?: return workout
        val state=old.sideState192?.let { JSONObject(it) }?.takeIf { it.optString("key")==a.key } ?: JSONObject()
            .put("schema",1).put("key",a.key).put("baseLeftSeconds",old.leftSeconds).put("baseRightSeconds",old.rightSeconds)
            .put("leftWorkMs",0).put("rightWorkMs",0).put("sideRestMs",0).put("setRestMs",0).put("pausedMs",0).put("segments",JSONArray())
        val field=if(a.phase=="work")if(a.side192=="left")"leftWorkMs" else "rightWorkMs" else if(a.restKind192=="side")"sideRestMs" else "setRestMs"
        state.put(field,state.optLong(field)+work+rest).put("pausedMs",state.optLong("pausedMs")+pause)
        if(skip)state.put("skipped",true).put(if(a.side192=="left")"skippedLeft192" else "skippedRight192",true)
        val parts=state.getJSONArray("segments")
        val token="${ex.id}:${old.setId.ifBlank { old.number.toString() }}:${a.round}:${a.phase}:${a.side192}:${a.restKind192}:${a.phaseNumber}"
        var part=(0 until parts.length()).map { parts.getJSONObject(it) }.firstOrNull { it.optString("id")==token }
        if(part==null){part=JSONObject().put("id",token).put("round",a.round).put("side",a.side192)
            .put("kind",if(a.phase=="work")"work" else if(a.restKind192=="side")"sideRest" else "setRest")
            .put("durationMs",0).put("pausedMs",0).put("targetMs",a.phaseSeconds192*1000L);parts.put(part)}
        part!!.put("durationMs",part.optLong("durationMs")+work+rest).put("pausedMs",part.optLong("pausedMs")+pause)
        if(skip)part.put("skipped",true)
        if(a.phase=="work"&&!part.optBoolean("skipped")&&part.optLong("durationMs")>=part.optLong("targetMs")&&!part.optBoolean("counted192")){
            val completed=a.side192+"CompletedRounds192";state.put(completed,state.optInt(completed)+1);part.put("counted192",true)
        }
        val left=state.optLong("leftWorkMs");val right=state.optLong("rightWorkMs")
        state.put("left",if(state.optInt("leftCompletedRounds192")>=a.rounds)"complete" else if(left>0)"partial" else if(state.optBoolean("skippedLeft192"))"skipped" else "pending")
            .put("right",if(state.optInt("rightCompletedRounds192")>=a.rounds)"complete" else if(right>0)"partial" else if(state.optBoolean("skippedRight192"))"skipped" else "pending")
        // Counters retain every measured phase. Keep bounded recent phase detail in
        // live snapshots; a long round sequence must still fit a Data Layer item.
        if(parts.length()>4){while(parts.length()>4)parts.remove(0);state.put("segmentsCompacted192",true)}
        val l=(state.optLong("baseLeftSeconds")+left/1000).toInt();val r=(state.optLong("baseRightSeconds")+right/1000).toInt()
        val updated=old.copy(leftSeconds=l,rightSeconds=r,reps=minOf(l,r).toString(),sideState192=state.toString())
        val current=workout.copy(exercises=workout.exercises.mapIndexed { ei,e -> if(ei!=a.targetExercise)e else e.copy(sets=e.sets.mapIndexed { si,s -> if(si==a.targetSet)updated else s }) })
        // The time ledger is cumulative and independent of editable/undoable set
        // values. Rebuilding it from a restored set would lose measured work.
        val log=JSONArray(a.sideLog192 ?: "[]")
        var entry=(0 until log.length()).map { log.getJSONObject(it) }.firstOrNull { it.optString("exerciseId")==ex.id&&it.optString("setId")==old.setId }
        if(entry==null){entry=JSONObject().put("exerciseId",ex.id).put("setId",old.setId).put("leftWorkMs",0).put("rightWorkMs",0).put("sideRestMs",0).put("setRestMs",0).put("pausedMs",0).put("segments192",JSONArray());log.put(entry)}
        entry!!.put(field,entry.optLong(field)+work+rest).put("pausedMs",entry.optLong("pausedMs")+pause)
            .put("workMs",entry.optLong("leftWorkMs")+entry.optLong("rightWorkMs")).put("restMs",entry.optLong("sideRestMs")+entry.optLong("setRestMs"))
            .put("leftState192",state.optString("left")).put("rightState192",state.optString("right"))
        if(skip)entry.put("skippedWork192",true)
        return current.copy(automation=a.copy(sideLog192=log.toString()))
    }
}

internal data class WearSideCopy192(val left: String,val right: String,val swap: String,val rest: String,val next: String,val order: String,val rounds: String,val work: String) {
    fun side(value: String)=if(value=="right")right else left
    companion object {
        fun language(lang: String)=when(lang.substringBefore('-').substringBefore('_')) {
            "en"->WearSideCopy192("Left","Right","Switch rest","Set rest","Next","First side","Rounds","Work")
            "de"->WearSideCopy192("Links","Rechts","Wechselpause","Satzpause","Danach","Erste Seite","Runden","Arbeit")
            "ro"->WearSideCopy192("Stânga","Dreapta","Pauză schimb","Pauză serie","Urmează","Prima parte","Runde","Efort")
            else->WearSideCopy192("Bal","Jobb","Oldalcsere","Sorozatpihenő","Következő","Kezdő oldal","Kör","Munka")
        }
    }
}
