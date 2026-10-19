package com.repforge.app.wear

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.*
import android.os.*
import com.repforge.shared.GpsRoutePacket193
import com.repforge.shared.GpsRouteSampler193
import org.json.JSONObject

/** User-armed coordinate recording, independent of Health Services and the workout timer. */
class WearGpsRouteService193 : Service(),LocationListener {
    private val handler=Handler(Looper.getMainLooper())
    private var workoutId=""
    private var sourceNode=""
    private var targetNode=""
    private var binding=""
    private var sampler: GpsRouteSampler193?=null
    private var listening=false
    private var stopping=false
    private var totalPoints=0
    private var manager: LocationManager?=null
    private val poll=object:Runnable { override fun run() { if(!stopping) { reconcile();if(!stopping)handler.postDelayed(this,1000) } } }
    override fun onBind(intent: Intent?)=null
    override fun onCreate() {
        super.onCreate();instance=this;manager=getSystemService(LocationManager::class.java)
        getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("gps-route193","TrainPilot GPS",NotificationManager.IMPORTANCE_LOW))
    }
    override fun onStartCommand(intent: Intent?,flags: Int,startId: Int): Int {
        if(intent?.action==STOP) {
            val requested=intent.getStringExtra("workoutId").orEmpty();val scoped=workoutId.ifBlank { WearGps193.scope(this).optString("workoutId") }
            if(requested!=scoped)return START_NOT_STICKY
            workoutId=requested
            try{WearGps193.arm(this,workoutId,false);finish(false,"stopped")}catch(_:Exception){finish(true,"storage")};return START_NOT_STICKY
        }
        val current=WorkoutSnapshotStore.load(this)
        if(intent==null||current==null||current.workoutId!=intent.getStringExtra("workoutId")||!WearGps193.armed(this,current.workoutId)||!owned(current)) { if(workoutId.isEmpty())stopSelf();return START_NOT_STICKY }
        workoutId=current.workoutId;sourceNode=intent.getStringExtra("sourceNode").orEmpty();targetNode=intent.getStringExtra("targetNode").orEmpty()
        if(!GpsRoutePacket193.id(sourceNode)||!GpsRoutePacket193.id(targetNode)||sourceNode==targetNode) { finish(true,"unavailable");return START_NOT_STICKY }
        if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED) { finish(true,"permission");return START_NOT_STICKY }
        try {
            val notice=notification()
            if(Build.VERSION.SDK_INT>=34)startForeground(193,notice,ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)else startForeground(193,notice)
            if(manager?.allProviders?.contains(LocationManager.GPS_PROVIDER)!=true) { finish(true,"unavailable");return START_NOT_STICKY }
            if(!locationAvailable()) { finish(true,"locationOff");return START_NOT_STICKY }
            if(binding.isEmpty()&&sampler==null)WearGps193.recover(this,true)
            totalPoints=WearGps193.status(this,workoutId).optInt("points")
            stopping=false;handler.removeCallbacks(poll);reconcile();if(!stopping)handler.postDelayed(poll,1000)
        }catch(_:SecurityException){finish(true,"permission")}catch(_:Exception){finish(true,"storage")}
        return START_NOT_STICKY
    }
    private fun owned(w: WearWorkout)=WearGps193.owns(w)
    private fun locationAvailable()=manager?.isLocationEnabled==true&&manager?.isProviderEnabled(LocationManager.GPS_PROVIDER)==true
    private fun eligible(w: WearWorkout): Boolean {
        if(!WearGps193.distance(w.exercise)||w.currentSet==null||w.currentSet!!.done)return false
        val a=w.automation;return a?.enabled!=true||!a.paused&&a.phase !in listOf("rest","complete")
    }
    private fun reconcile() {
        try {
            val current=WorkoutSnapshotStore.load(this)
            if(current==null||current.workoutId!=workoutId||!owned(current)||!WearGps193.armed(this,workoutId)) { finish(false,"paused");return }
            if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED) { finish(true,"permission");return }
            if(!locationAvailable()) { finish(true,"locationOff");return }
            if(!eligible(current)) { stopLocations();close(false);WearGps193.update(this,workoutId,"paused",totalPoints);return }
            val h=current.handoff193?.let(::JSONObject)
            if(h!=null&&(h.optString("target")!="watch"||h.optString("state")!="committed"||h.optString("targetNode")!=sourceNode||h.optString("sourceNode")!=targetNode)) { finish(true,"unavailable");return }
            val exercise=current.exercise!!;val set=current.currentSet!!
            val next="${current.ownerEpoch193}:${exercise.id}:${set.setId}"
            if(next!=binding) {
                stopLocations();close(false)
                val meta=JSONObject().put("routeKey",GpsRoutePacket193.key(workoutId,set.setId)).put("workoutId",workoutId).put("exerciseId",exercise.id).put("setId",set.setId)
                    .put("sourceNode",sourceNode).put("targetNode",targetNode).put("ownerEpoch193",current.ownerEpoch193)
                if(WearGps193.spool(this).revoked(meta.getString("routeKey"))) { finish(false,"deleted");return }
                WearGps193.spool(this).begin(meta,WearGps193.queue(this));binding=next
                sampler=GpsRouteSampler193 { point -> WearGps193.spool(this).append(point,WearGps193.queue(this)) }
                WearGps193.update(this,workoutId,"acquiring",totalPoints)
            }
            if(!listening) { manager!!.requestLocationUpdates(LocationManager.GPS_PROVIDER,4000,3f,this,Looper.getMainLooper());listening=true }
        }catch(_:SecurityException){finish(true,"permission")}catch(_:Exception){finish(true,"storage")}
    }
    override fun onLocationChanged(location: Location) {
        if(stopping||!listening)return
        try {
            val current=WorkoutSnapshotStore.load(this) ?: return
            val selected="${current.ownerEpoch193}:${current.exercise?.id}:${current.currentSet?.setId}"
            if(current.workoutId!=workoutId||!owned(current)||!eligible(current)||selected!=binding) { reconcile();return }
            val mock=if(Build.VERSION.SDK_INT>=31)location.isMock else location.isFromMockProvider
            val elapsed=location.elapsedRealtimeNanos/1000000L;val age=SystemClock.elapsedRealtime()-elapsed
            val rejection=when {mock->"mock";location.provider!=LocationManager.GPS_PROVIDER->"provider";!location.hasAccuracy()->"accuracy";age !in 0..15000->"stale";else->""}
            if(rejection.isNotEmpty()) { WearGps193.received(this,workoutId,rejection);sampler?.breakSegment();return }
            WearGps193.received(this,workoutId,"filtered")
            val accepted=sampler?.add(location.latitude,location.longitude,location.accuracy.toDouble(),elapsed,location.time,if(location.hasSpeed())location.speed.toDouble()else Double.NaN)==true
            if(accepted) { totalPoints++;WearGps193.update(this,workoutId,"recording",totalPoints,location.accuracy.toDouble()) }
            else if(sampler?.partial()==true)finish(true,"storage")
        }catch(_:SecurityException){finish(true,"permission")}catch(_:Exception){finish(true,"storage")}
    }
    override fun onProviderDisabled(provider: String) { if(provider==LocationManager.GPS_PROVIDER)finish(true,"locationOff") }
    private fun stopLocations() { if(listening) { try{manager?.removeUpdates(this)}catch(_:Exception){};listening=false } }
    private fun close(partial: Boolean) { WearGps193.spool(this).close(partial,WearGps193.queue(this));sampler=null;binding="" }
    private fun finish(partial: Boolean,state: String) {
        if(stopping)return;stopping=true;handler.removeCallbacks(poll);stopLocations()
        var result=state;try{close(partial)}catch(_:Exception){result="storage"}
        if(workoutId.isNotBlank())WearGps193.update(this,workoutId,result,totalPoints)
        stopForeground(STOP_FOREGROUND_REMOVE);stopSelf()
    }
    private fun notification(): Notification {
        val open=PendingIntent.getActivity(this,193,Intent(this,MainActivity::class.java).putExtra("destination","gpsRoute193"),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stop=PendingIntent.getService(this,193,Intent(this,WearGpsRouteService193::class.java).setAction(STOP).putExtra("workoutId",workoutId),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this,"gps-route193").setSmallIcon(R.drawable.ic_workout_recording).setContentTitle(WearGpsText193.text("title"))
            .setContentText(WearGpsText193.text("notification")).setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true)
            .addAction(Notification.Action.Builder(android.R.drawable.ic_media_pause,WearGpsText193.text("stop"),stop).build()).build()
    }
    override fun onDestroy() { if(!stopping)finish(true,"interrupted");handler.removeCallbacks(poll);stopLocations();if(instance===this)instance=null;super.onDestroy() }
    companion object {
        const val STOP="com.repforge.app.wear.STOP_ROUTE193"
        private var instance: WearGpsRouteService193?=null
        fun active(workoutId: String)=instance?.let { it.workoutId==workoutId&&!it.stopping }==true
        fun freeze(context: Context) {
            val live=instance
            if(live!=null)live.finish(false,"paused")
            else WearGps193.spool(context).close(true,WearGps193.queue(context))
        }
        fun start(context: Context,workout: WearWorkout,sourceNode: String,targetNode: String) {
            if(active(workout.workoutId))return
            context.startForegroundService(Intent(context,WearGpsRouteService193::class.java).putExtra("workoutId",workout.workoutId).putExtra("sourceNode",sourceNode).putExtra("targetNode",targetNode))
        }
    }
}
