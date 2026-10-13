package com.repforge.app.wear

import android.content.Context
import org.json.JSONObject

/** Native phone clock is a display source. It never starts a second watch timer. */
object WearPhoneAutomatic {
    const val PATH="/trainpilot/phone-automatic"
    fun receive(context: Context,raw: String?) {
        if(raw==null||raw.length>90000)return
        val value=try { JSONObject(raw) }catch(_:Exception){return}
        context.getSharedPreferences("trainpilot_phone_automatic",Context.MODE_PRIVATE).edit().putString("state",raw).apply()
        apply(context,value)
    }
    fun applyCached(context: Context) {
        val raw=context.getSharedPreferences("trainpilot_phone_automatic",Context.MODE_PRIVATE).getString("state",null) ?: return
        try { apply(context,JSONObject(raw)) }catch(_:Exception){}
    }
    private fun apply(context: Context,value: JSONObject) {
        val current=WorkoutSnapshotStore.load(context) ?: return
        if(current.timerOwner!="phone"||value.optString("workoutId")!=current.workoutId)return
        val previous=current.automation ?: return
        if(previous.key!=value.optString("key")||previous.interrupted||previous.awaitingPhone)return
        val a=WearAutomatic.parse(value) ?: return
        if(a.owner!="phone"||a.phaseNumber<previous.phaseNumber)return
        WorkoutSnapshotStore.saveWorkout(context,projectPhoneAutomatic(current,value,a))
    }
}

