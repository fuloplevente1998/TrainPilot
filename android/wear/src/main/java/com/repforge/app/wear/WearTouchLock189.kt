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

/** A short tap arms a separate three-second hold. Release is required; drags/multi-touch cancel. */
internal class WearUnlockGesture189 {
    companion object { const val HOLD_MS=3000L;const val ARM_MS=5000L;const val MAX_HOLD_MS=6000L }
    private var downAt: Long?=null
    private var holding=false
    private var armedUntil=0L
    fun down(now: Long) { holding=armedUntil>0&&now<=armedUntil;downAt=now;if(!holding)armedUntil=0 }
    fun up(now: Long): Boolean {
        val started=downAt ?: return false
        val duration=now-started
        val unlock=holding&&duration in HOLD_MS..MAX_HOLD_MS
        if(!holding&&duration in 0..350)armedUntil=now+ARM_MS else armedUntil=0
        downAt=null;holding=false
        return unlock
    }
    fun cancel() { downAt=null;holding=false;armedUntil=0 }
    fun phase(now: Long): Int {
        val duration=downAt?.let { now-it } ?: 0
        if(holding&&duration>MAX_HOLD_MS){cancel();return 0}
        return if(holding)if(duration>=HOLD_MS)3 else 2 else if(armedUntil>0&&now<=armedUntil)1 else 0
    }
    fun progress(now: Long)=if(holding)((now-(downAt ?: now)).toFloat()/HOLD_MS).coerceIn(0f,1f) else 0f
}

internal data class WearTouchCopy189(val lock: String,val locked: String,val tap: String,val hold: String,val release: String,
    val work: String,val rest: String,val paused: String,val ready: String,val saved: String) {
    companion object {
        fun language(language: String)=when(language.substringBefore('-').substringBefore('_')) {
            "en"->WearTouchCopy189("Touch lock","Locked","Tap the lock, then hold","Hold for 3 seconds","Release to unlock","Work","Rest","Paused","Ready","Recorded")
            "de"->WearTouchCopy189("Berührungssperre","Gesperrt","Schloss antippen, dann halten","3 Sekunden halten","Zum Entsperren loslassen","Arbeit","Pause","Pausiert","Bereit","Erfasst")
            "ro"->WearTouchCopy189("Blocare atingere","Blocat","Atinge lacătul, apoi ține apăsat","Ține apăsat 3 secunde","Eliberează pentru deblocare","Lucru","Pauză","În pauză","Pregătit","Înregistrat")
            else->WearTouchCopy189("Érintészár","Zárolva","Koppints a lakatra, majd tartsd","Tartsd nyomva 3 mp-ig","Engedd el a feloldáshoz","Munka","Pihenő","Szünet","Készen áll","Rögzítve")
        }
    }
}
