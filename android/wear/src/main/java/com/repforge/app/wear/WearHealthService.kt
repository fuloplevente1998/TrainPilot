package com.repforge.app.wear

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.os.*
import androidx.health.services.client.HealthServices
import androidx.health.services.client.ExerciseUpdateCallback
import androidx.health.services.client.data.*
import com.google.common.util.concurrent.ListenableFuture
import org.json.JSONObject
import java.util.concurrent.Executor

/** Started with permission from a visible Activity; survives the watch/phone screens turning off. */
class WearHealthService : Service(), ExerciseUpdateCallback {
    private val executor=Executor { command -> Handler(Looper.getMainLooper()).post(command) }
    private val client by lazy { HealthServices.getClient(this).exerciseClient }
    private var summary: JSONObject?=null
    private var registered=false
    private var owned=false
    private val stopReceiver=object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) { end() }
    }
    override fun onCreate() {
        super.onCreate()
        if(Build.VERSION.SDK_INT>=33)registerReceiver(stopReceiver,IntentFilter(STOP),Context.RECEIVER_NOT_EXPORTED)
        else registerReceiver(stopReceiver,IntentFilter(STOP))
    }
    private var ending=false
    private var destroyed=false
    private var watchdog: Runnable?=null
    private val handler=Handler(Looper.getMainLooper())
    override fun onBind(intent: Intent?) = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(intent?.action==STOP) { end();return START_NOT_STICKY }
        val id=intent?.getStringExtra("workoutId") ?: WearHealthStore.active(this)
        if(id.isBlank()) { stopSelf();return START_NOT_STICKY }
        if(summary?.optString("workoutId")==id)return START_STICKY
        if(summary!=null) { stopSelf();return START_NOT_STICKY }
        summary=WearHealthStore.begin(this,id)
        if(summary!!.optString("state")=="ended") { stopSelf();return START_NOT_STICKY }
        try {
            val manager=getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel("workout-health","Edzésmérés",NotificationManager.IMPORTANCE_LOW))
            val pending=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val notification=Notification.Builder(this,"workout-health").setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle("TrainPilot edzésmérés").setContentText("Pulzus és támogatott edzésadatok rögzítése").setContentIntent(pending).setOngoing(true).build()
            val location=intent?.getBooleanExtra("gps",false)==true && checkSelfPermission("android.permission.ACCESS_FINE_LOCATION")==android.content.pm.PackageManager.PERMISSION_GRANTED
            if(Build.VERSION.SDK_INT>=34) startForeground(129,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH or if(location)ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0)
            else startForeground(129,notification,if(location)ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0)
            WearHealthStore.save(this,summary!!)
            client.setUpdateCallback(executor,this)
            await(client.getCurrentExerciseInfoAsync()) { info ->
                when(info.exerciseTrackedStatus) {
                    ExerciseTrackedStatus.OTHER_APP_IN_PROGRESS -> fail("Másik alkalmazás már edzést mér. A TrainPilot nem szakította meg.")
                    ExerciseTrackedStatus.OWNED_EXERCISE_IN_PROGRESS -> { owned=true;summary?.put("state","active");persist() }
                    else -> capabilities(intent?.getStringExtra("exerciseType"),location)
                }
            }
        } catch(error: Exception) { fail("A mérés nem indítható: ${error.message ?: "ellenőrizd az engedélyeket"}") }
        return START_STICKY
    }
    private fun capabilities(requested: String?, gps: Boolean) {
        await(client.getCapabilitiesAsync()) { capabilities ->
            val wanted=when(requested) { "running" -> ExerciseType.RUNNING; "walking" -> ExerciseType.WALKING; "cycling" -> ExerciseType.BIKING; else -> ExerciseType.STRENGTH_TRAINING }
            val type=listOf(wanted,ExerciseType.WORKOUT).firstOrNull { capabilities.supportedExerciseTypes.contains(it) }
            if(type==null) { fail("Ezen az órán ez az edzéstípus nem mérhető.");return@await }
            val supported=capabilities.getExerciseTypeCapabilities(type).supportedDataTypes
            val requestedMetrics=setOf(DataType.HEART_RATE_BPM,DataType.CALORIES_TOTAL,DataType.STEPS_TOTAL,DataType.DISTANCE_TOTAL,DataType.SPEED)
            val heart=checkSelfPermission(if(Build.VERSION.SDK_INT>=36) "android.permission.health.READ_HEART_RATE" else "android.permission.BODY_SENSORS")==android.content.pm.PackageManager.PERMISSION_GRANTED
            val activity=checkSelfPermission("android.permission.ACTIVITY_RECOGNITION")==android.content.pm.PackageManager.PERMISSION_GRANTED
            val metrics=requestedMetrics.filter { supported.contains(it) && if(it==DataType.HEART_RATE_BPM)heart else activity }.toSet()
            if(metrics.isEmpty()) { fail("Az óra nem kínál támogatott mérési adatot.");return@await }
            val config=ExerciseConfig(type,metrics,isAutoPauseAndResumeEnabled=false,isGpsEnabled=gps && type in listOf(ExerciseType.RUNNING,ExerciseType.WALKING,ExerciseType.BIKING))
            summary?.let { if(it.optDouble("activeDurationSeconds",0.0)>0)it.put("partial",true);it.put("exerciseType",type.toString()).put("gps",config.isGpsEnabled) }
            await(client.startExerciseAsync(config)) { owned=true;summary?.put("state","active");persist() }
        }
    }
    private fun <T> await(future: ListenableFuture<T>, success: (T)->Unit) {
        future.addListener({ if(!destroyed)try { success(future.get()) } catch(error: Exception) { fail("Mérési hiba: ${error.cause?.message ?: error.message}") } },executor)
    }
    override fun onRegistered() { registered=true }
    override fun onRegistrationFailed(throwable: Throwable) { fail("A mérési kapcsolat nem indult: ${throwable.message}") }
    override fun onAvailabilityChanged(dataType: DataType<*,*>, availability: Availability) { /* Missing measurements stay absent. */ }
    override fun onLapSummaryReceived(lapSummary: ExerciseLapSummary) {}
    override fun onExerciseUpdateReceived(update: ExerciseUpdate) {
        val data=summary ?: return
        if(data.optString("state")=="error")return
        val accumulator=WearHealthAccumulator(data)
        val currentOrigin=System.currentTimeMillis()-SystemClock.elapsedRealtime()
        val oldOrigin=data.optLong("bootOrigin",currentOrigin)
        val origin=if(kotlin.math.abs(currentOrigin-oldOrigin)<10000)oldOrigin else currentOrigin
        data.put("bootOrigin",origin)
        update.latestMetrics.getData(DataType.HEART_RATE_BPM).sortedBy { it.timeDurationFromBoot }.forEach { accumulator.heartRate(origin+it.timeDurationFromBoot.toMillis(),it.value) }
        accumulator.total("totalCalories",update.latestMetrics.getData(DataType.CALORIES_TOTAL)?.total)
        accumulator.total("steps",update.latestMetrics.getData(DataType.STEPS_TOTAL)?.total?.toDouble())
        accumulator.total("distanceMeters",update.latestMetrics.getData(DataType.DISTANCE_TOTAL)?.total)
        accumulator.sample("speedMps",update.latestMetrics.getData(DataType.SPEED).lastOrNull()?.value)
        accumulator.total("activeDurationSeconds",update.activeDuration.toMillis()/1000.0)
        val ended=update.exerciseStateInfo.state.isEnded
        data.put("state",if(ended)"ended" else if(ending)"ending" else "active")
        persist()
        if(ended) { WearHealthStore.publish(this,data);stopSelf() }
    }
    private fun persist() { summary?.let { WearHealthStore.save(this,it);WearSurfaces.refresh(this) } }
    private fun fail(message: String) {
        summary?.put("state","error")?.put("message",message);persist()
        if(owned)try { client.endExerciseAsync() }catch(_:Exception){}
        stopSelf()
    }
    private fun end() {
        if(ending)return
        if(summary==null) {
            val id=WearHealthStore.active(this);summary=WearHealthStore.load(this,id)
        }
        if(summary==null) { stopSelf();return }
        ending=true
        try {
            // End flushes the final cumulative values through the callback.
            await(client.endExerciseAsync()) {
                summary?.put("state","ended");persist();summary?.let { WearHealthStore.publish(this,it) }
                watchdog=Runnable { stopSelf() }.also { handler.postDelayed(it,2500) }
            }
        } catch(error: Exception) { fail("A mérés lezárása nem sikerült: ${error.message}") }
    }
    override fun onDestroy() {
        destroyed=true;try { unregisterReceiver(stopReceiver) }catch(_:Exception){};watchdog?.let { handler.removeCallbacks(it) }
        if(registered)try { client.clearUpdateCallbackAsync(this) }catch(_:Exception){}
        super.onDestroy()
    }
    companion object {
        const val STOP="com.repforge.app.wear.STOP_HEALTH"
        fun start(context: Context, workout: WearWorkout) {
            val cardio=workout.exercises.firstOrNull { it.measurementType == "distance" || it.repUnit.lowercase() in listOf("m","km") }
            val name=cardio?.name?.lowercase().orEmpty()
            val type=when { cardio==null -> "strength"; name.contains("kerék") || name.contains("cycl") || name.contains("bic") -> "cycling";name.contains("séta") || name.contains("walk") -> "walking";else -> "running" }
            context.startForegroundService(Intent(context,WearHealthService::class.java).putExtra("workoutId",workout.workoutId).putExtra("exerciseType",type).putExtra("gps",cardio!=null && WearHealthStore.gps(context)))
        }
        fun stop(context: Context) { context.sendBroadcast(Intent(STOP).setPackage(context.packageName)) }
    }
}
