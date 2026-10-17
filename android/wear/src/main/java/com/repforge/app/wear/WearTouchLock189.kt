package com.repforge.app.wear

import android.content.Context

/** Local input protection only: never enters workout snapshots, timer state or the outbox. */
object WearTouchLock189 {
    private const val PREFS="trainpilot_wear_touch_lock189"
    fun identity(workout: WearWorkout?)=workout?.let { it.workoutId+"\n"+it.started }.orEmpty()
    fun locked(context: Context,workout: WearWorkout?): Boolean {
        val key=identity(workout)
        return key.isNotBlank()&&context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString("workout",null)==key
    }
    fun set(context: Context,workout: WearWorkout,locked: Boolean): Boolean {
        val edit=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit()
        return (if(locked)edit.putString("workout",identity(workout)) else edit.remove("workout")).commit()
    }
    fun retain(context: Context,workout: WearWorkout?) {
        val prefs=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
        if(prefs.contains("workout")&&prefs.getString("workout",null)!=identity(workout))prefs.edit().clear().commit()
    }
}

/** One uninterrupted three-second hold unlocks immediately; drags/multi-touch cancel. */
internal class WearUnlockGesture189 {
    companion object { const val HOLD_MS=3000L }
    private var downAt: Long?=null
    val holding get()=downAt!=null
    fun down(now: Long) { downAt=now }
    fun tryUnlock(now: Long): Boolean {
        val started=downAt ?: return false
        if(now-started<HOLD_MS)return false
        cancel()
        return true
    }
    fun up(now: Long): Boolean {
        val unlock=tryUnlock(now)
        cancel()
        return unlock
    }
    fun cancel() { downAt=null }
    fun progress(now: Long)=downAt?.let { ((now-it).toFloat()/HOLD_MS).coerceIn(0f,1f) } ?: 0f
}

internal data class WearTouchCopy189(val lock: String,val locked: String,val hold: String,
    val work: String,val rest: String,val paused: String,val ready: String,val saved: String) {
    companion object {
        fun language(language: String)=when(language.substringBefore('-').substringBefore('_')) {
            "en"->WearTouchCopy189("Touch lock","Locked","Hold the lock for 3 seconds","Work","Rest","Paused","Ready","Recorded")
            "de"->WearTouchCopy189("Berührungssperre","Gesperrt","Schloss 3 Sekunden halten","Arbeit","Pause","Pausiert","Bereit","Erfasst")
            "ro"->WearTouchCopy189("Blocare atingere","Blocat","Ține lacătul apăsat 3 secunde","Lucru","Pauză","În pauză","Pregătit","Înregistrat")
            else->WearTouchCopy189("Érintészár","Zárolva","Tartsd a lakatot 3 mp-ig","Munka","Pihenő","Szünet","Készen áll","Rögzítve")
        }
    }
}
