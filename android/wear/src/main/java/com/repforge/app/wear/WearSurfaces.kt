package com.repforge.app.wear

import android.app.PendingIntent
import android.content.*
import androidx.wear.tiles.TileService
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester

internal data class WearGlance(val title: String, val detail: String, val short: String, val destination: String)
internal fun wearGlance(workout: WearWorkout?, home: WatchHomeSnapshot?, closure: WearClosure?, now: Long): WearGlance {
    if(workout!=null) {
        val rest=((workout.restEndAt-now+999)/1000).coerceAtLeast(0)
        return WearGlance(workout.exercise?.name ?: workout.programName,
            if(rest>0)"Pihenő ${rest/60}:${(rest%60).toString().padStart(2,'0')}" else "${workout.completedSets} rögzített sorozat",
            if(rest>0)"${rest}s" else "${workout.currentExercise+1}/${workout.exercises.size}","workout")
    }
    if(closure?.status in listOf("pending","error"))return WearGlance("Mentés várakozik","Az edzés az órán megmaradt","Szinkron","summary")
    val rec=home?.recommended
    return WearGlance(rec?.day?.name ?: "TrainPilot",rec?.day?.programName ?: "Nyisd meg az alkalmazást",rec?.day?.id ?: "TP","home")
}
object WearSurfaces {
    private var lastRefresh=0L
    fun glance(context: Context) = wearGlance(WorkoutSnapshotStore.load(context),WatchHomeStore.load(context),WearClosureStore.load(context),System.currentTimeMillis())
    fun launch(context: Context): PendingIntent = PendingIntent.getActivity(context,129,
        Intent(context,MainActivity::class.java).putExtra("destination",glance(context).destination).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    @Synchronized fun refresh(context: Context, force: Boolean=false) {
        val now=android.os.SystemClock.elapsedRealtime();if(!force && now-lastRefresh<10000)return;lastRefresh=now
        try { TileService.getUpdater(context).requestUpdate(TrainPilotTileService::class.java) }catch(_: Exception){}
        try { ComplicationDataSourceUpdateRequester.create(context,ComponentName(context,TrainPilotComplicationService::class.java)).requestUpdateAll() }catch(_: Exception){}
    }
}
