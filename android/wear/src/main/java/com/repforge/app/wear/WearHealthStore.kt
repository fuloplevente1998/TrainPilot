package com.repforge.app.wear

import android.content.Context
import org.json.JSONObject
import java.util.UUID

/** Bounded measured summary. Unsupported metrics stay absent; totals are never summed on replay. */
class WearHealthAccumulator(val value: JSONObject) {
    fun heartRate(time: Long, bpm: Double) {
        if (!bpm.isFinite() || bpm < 20 || bpm > 250 || time <= value.optLong("lastSampleAt")) return
        val count = value.optLong("heartRateSamples") + 1
        val sum = value.optDouble("heartRateSum", 0.0) + bpm
        value.put("lastSampleAt",time).put("heartRate",bpm).put("heartRateSamples",count).put("heartRateSum",sum)
            .put("averageHeartRate",sum/count).put("maxHeartRate",maxOf(value.optDouble("maxHeartRate",bpm),bpm))
    }
    fun total(key: String, number: Double?) {
        if (number == null || !number.isFinite() || number < 0) return
        value.put(key,maxOf(value.optDouble(key,0.0),number))
    }
    fun sample(key: String, number: Double?) {
        if (number != null && number.isFinite() && number >= 0) value.put(key,number)
    }
    fun snapshot(): JSONObject = JSONObject(value.toString()).apply { remove("heartRateSum"); remove("bootOrigin"); remove("lastPublishedAt") }
}

object WearHealthStore {
    private const val PREFS = "trainpilot_wear_health"
    @Synchronized fun enabled(context: Context) = context.getSharedPreferences(PREFS,0).getBoolean("enabled",false)
    @Synchronized fun enable(context: Context, enabled: Boolean) { context.getSharedPreferences(PREFS,0).edit().putBoolean("enabled",enabled).commit() }
    @Synchronized fun gps(context: Context) = context.getSharedPreferences(PREFS,0).getBoolean("gps",false)
    @Synchronized fun gps(context: Context, enabled: Boolean) { context.getSharedPreferences(PREFS,0).edit().putBoolean("gps",enabled).commit() }
    @Synchronized fun load(context: Context, id: String): JSONObject? = context.getSharedPreferences(PREFS,0).getString("summary:$id",null)?.let { try { JSONObject(it) } catch (_: Exception) { null } }
    @Synchronized fun begin(context: Context, id: String): JSONObject {
        load(context,id)?.let { return it }
        val prefs=context.getSharedPreferences(PREFS,0)
        val watchId=prefs.getString("watchId",null) ?: UUID.randomUUID().toString().also { prefs.edit().putString("watchId",it).commit() }
        return JSONObject().put("schema",1).put("workoutId",id).put("watchId",watchId).put("source","wear_health_services").put("revision",0).put("state","starting")
    }
    @Synchronized fun save(context: Context, value: JSONObject) {
        val id=value.optString("workoutId"); if(id.isBlank())return
        value.put("revision",value.optLong("revision")+1).put("updatedAt",System.currentTimeMillis())
        val prefs=context.getSharedPreferences(PREFS,0)
        val ids=prefs.getStringSet("ids",emptySet())!!.toMutableSet();ids.add(id)
        val editor=prefs.edit().putString("summary:$id",value.toString()).putString("active",if(value.optString("state") in listOf("starting","active"))id else "")
        while(ids.size>16) { val oldest=ids.minByOrNull { load(context,it)?.optLong("updatedAt") ?: 0L } ?: break;ids.remove(oldest);editor.remove("summary:$oldest") }
        editor.putStringSet("ids",ids).commit()
    }
    @Synchronized fun active(context: Context): String = context.getSharedPreferences(PREFS,0).getString("active","") ?: ""
    @Synchronized fun publish(context: Context, summary: JSONObject) {
        if(summary.optLong("revision")<=0)return
        val cleaned=WearHealthAccumulator(summary).snapshot()
        val command=JSONObject().put("schema",1).put("commandId",UUID.randomUUID().toString()).put("createdAt",System.currentTimeMillis())
            .put("workoutId",summary.getString("workoutId")).put("action","healthSummary").put("healthSummary",cleaned)
        WearCommandOutbox.enqueue(context,command)
    }
}
