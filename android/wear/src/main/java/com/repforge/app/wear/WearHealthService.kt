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
    private var lifecycle: WearRecordingLifecycle?=null
    private var registered=false
    private var afterRegistration: (() -> Unit)?=null
    private val stopReceiver=object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) { end() }
    }
    override fun onCreate() {
        super.onCreate()
        if(Build.VERSION.SDK_INT>=33)registerReceiver(stopReceiver,IntentFilter(STOP),Context.RECEIVER_NOT_EXPORTED)
        else registerReceiver(stopReceiver,IntentFilter(STOP))
    }
    private var destroyed=false
    private var watchdog: Runnable?=null
    private val handler=Handler(Looper.getMainLooper())
    override fun onBind(intent: Intent?) = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(intent?.action==STOP) { end();return START_NOT_STICKY }
        val id=intent?.getStringExtra("workoutId") ?: WearHealthStore.active(this)
        if(id.isBlank()) { stopSelf();return START_NOT_STICKY }
        if(summary?.optString("workoutId")==id)return START_STICKY
        if(summary!=null) { end();return START_STICKY }
        val oldId=WearHealthStore.active(this)
        val closePrevious=oldId.isNotBlank() && oldId!=id
        summary=WearHealthStore.begin(this,if(closePrevious)oldId else id)
        if(summary!!.optString("state") in listOf("ended","error")) { stopSelf();return START_NOT_STICKY }
        val session=WearRecordingLifecycle(summary!!).also { lifecycle=it }
        val request=WearRecordingRequest.restore(summary!!,intent?.getStringExtra("exerciseType"),
            intent?.takeIf { it.hasExtra("gps") }?.getBooleanExtra("gps",false))
        request.save(summary!!)
        if(closePrevious || !WearHealthStore.enabled(this) || WearClosureStore.isClosed(this,summary!!.optString("workoutId")) || WorkoutSnapshotStore.load(this)?.workoutId!=summary!!.optString("workoutId"))session.requestStop()
        try {
            val manager=getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel("workout-health","Edzésmérés",NotificationManager.IMPORTANCE_LOW))
            val pending=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java).putExtra("destination","workout"),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val stop=PendingIntent.getBroadcast(this,129,Intent(STOP).setPackage(packageName),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val notification=Notification.Builder(this,"workout-health").setSmallIcon(R.drawable.ic_workout_recording)
                .setContentTitle("TrainPilot edzésmérés").setContentText("A mérés kikapcsolt kijelzővel is folytatódik")
                .setContentIntent(pending).setOngoing(true).setCategory(Notification.CATEGORY_SERVICE)
                .addAction(Notification.Action.Builder(android.R.drawable.ic_media_pause,"Mérés leállítása",stop).build()).build()
            val location=request.gps && checkSelfPermission("android.permission.ACCESS_FINE_LOCATION")==android.content.pm.PackageManager.PERMISSION_GRANTED
            if(Build.VERSION.SDK_INT>=34) startForeground(129,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH or if(location)ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0)
            else startForeground(129,notification,if(location)ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0)
            WearHealthStore.save(this,summary!!)
            // Register before resuming/starting, so the final update cannot be missed.
            afterRegistration = {
                await(client.getCurrentExerciseInfoAsync()) { info ->
                    when(info.exerciseTrackedStatus) {
                        ExerciseTrackedStatus.OTHER_APP_IN_PROGRESS -> fail("Másik alkalmazás már edzést mér. A TrainPilot nem szakította meg.")
                        ExerciseTrackedStatus.OWNED_EXERCISE_IN_PROGRESS -> {
                            session.confirmOwnership()
                            summary?.put("state",if(session.stopRequested)"ending" else "active");persist()
                            if(session.stopRequested)end()
                        }
                        else -> if(session.stopRequested){finishWithoutExercise()}else capabilities(request.exerciseType,location)
                    }
                }
            }
            client.setUpdateCallback(executor,this)
        } catch(error: Exception) { fail("A mérés nem indítható: ${error.message ?: "ellenőrizd az engedélyeket"}") }
        return START_STICKY
    }
    private fun capabilities(requested: String?, gps: Boolean) {
        if(lifecycle?.stopRequested==true){finishWithoutExercise();return}
        await(client.getCapabilitiesAsync()) { capabilities ->
            if(lifecycle?.stopRequested==true){finishWithoutExercise();return@await}
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
            summary?.let { if(it.optDouble("activeDurationSeconds",0.0)>0)it.put("partial",true);WearHealthAccumulator(it).beginSegment()
                it.put("exerciseType",type.toString()).put("gps",config.isGpsEnabled) }
            persist()
            awaitCompletion(client.startExerciseAsync(config)) {
                lifecycle?.confirmOwnership()
                summary?.put("state",if(lifecycle?.stopRequested==true)"ending" else "active");persist()
                if(lifecycle?.stopRequested==true)end()
            }
        }
    }
    private fun <T : Any> await(future: ListenableFuture<T>, success: (T)->Unit) {
        WearHealthFutures.awaitValue(future,executor,{ !destroyed && lifecycle?.finished!=true },::futureFailed,success)
    }
    private fun awaitCompletion(future: ListenableFuture<Void>, success: ()->Unit) {
        WearHealthFutures.awaitCompletion(future,executor,{ !destroyed && lifecycle?.finished!=true },::futureFailed,success)
    }
    private fun futureFailed(error: Exception) { fail("Mérési hiba: ${error.cause?.message ?: error.message}") }
    override fun onRegistered() {
        registered=true
        val action=afterRegistration;afterRegistration=null
        if(!destroyed && lifecycle?.finished!=true)try { action?.invoke() }catch(error:Exception) { futureFailed(error) }
    }
    override fun onRegistrationFailed(throwable: Throwable) { fail("A mérési kapcsolat nem indult: ${throwable.message}") }
    override fun onAvailabilityChanged(dataType: DataType<*,*>, availability: Availability) { /* Missing measurements stay absent. */ }
    override fun onLapSummaryReceived(lapSummary: ExerciseLapSummary) {}
    override fun onExerciseUpdateReceived(update: ExerciseUpdate) {
        val data=summary ?: return
        if(lifecycle?.finished==true || destroyed || data.optString("state")=="error")return
        val accumulator=WearHealthAccumulator(data)
        val currentOrigin=System.currentTimeMillis()-SystemClock.elapsedRealtime()
        val oldOrigin=data.optLong("bootOrigin",currentOrigin)
        val origin=if(kotlin.math.abs(currentOrigin-oldOrigin)<10000)oldOrigin else currentOrigin
        data.put("bootOrigin",origin)
        update.latestMetrics.getData(DataType.HEART_RATE_BPM).sortedBy { it.timeDurationFromBoot }.forEach { accumulator.heartRate(origin+it.timeDurationFromBoot.toMillis(),it.value) }
        accumulator.segmentTotal("totalCalories",update.latestMetrics.getData(DataType.CALORIES_TOTAL)?.total)
        accumulator.segmentTotal("steps",update.latestMetrics.getData(DataType.STEPS_TOTAL)?.total?.toDouble())
        accumulator.segmentTotal("distanceMeters",update.latestMetrics.getData(DataType.DISTANCE_TOTAL)?.total)
        accumulator.sample("speedMps",update.latestMetrics.getData(DataType.SPEED).lastOrNull()?.value)
        update.activeDurationCheckpoint?.let { checkpoint ->
            val elapsed=if(update.exerciseStateInfo.state.isPaused || update.exerciseStateInfo.state.isEnded)0L else java.time.Duration.between(checkpoint.time,java.time.Instant.now()).toMillis().coerceAtLeast(0L)
            accumulator.segmentTotal("activeDurationSeconds",(checkpoint.activeDuration.toMillis()+elapsed)/1000.0)
        }
        val ended=update.exerciseStateInfo.state.isEnded
        data.put("state",if(ended)"ended" else if(lifecycle?.stopRequested==true)"ending" else "active")
        persist()
        if(ended)finish()
    }
    private fun persist() { summary?.let { WearHealthStore.save(this,it);WearSurfaces.refresh(this) } }
    private fun finishWithoutExercise() {
        summary?.put("partial",true);finish()
    }
    private fun finish() {
        if(lifecycle?.finish()!=true)return
        watchdog?.let { handler.removeCallbacks(it) };watchdog=null
        persist();summary?.let { WearHealthStore.publish(this,it) };stopSelf()
    }
    private fun fail(message: String) {
        if(lifecycle?.finished==true)return
        lifecycle?.finish()
        summary?.put("state","error")?.put("partial",true)?.put("message",message);persist();summary?.let { WearHealthStore.publish(this,it) }
        if(lifecycle?.owned==true)try { client.endExerciseAsync() }catch(_:Exception){}
        stopSelf()
    }
    private fun end() {
        val session=lifecycle ?: return
        session.requestStop();persist()
        // Do not end a foreign exercise while the ownership check/start future is still pending.
        if(!session.beginEnding())return
        try {
            // End flushes the final cumulative values through the callback.
            awaitCompletion(client.endExerciseAsync()) {
                // Completion is not the final metrics callback. Keep receiving the flush.
                watchdog=Runnable {
                    summary?.put("partial",true)?.put("message","Az utolsó mérési csomag nem érkezett meg; a megőrzött adatok láthatók.")
                    finish()
                }.also { handler.postDelayed(it,5000) }
            }
        } catch(error: Exception) { fail("A mérés lezárása nem sikerült: ${error.message}") }
    }
    override fun onDestroy() {
        destroyed=true;afterRegistration=null;try { unregisterReceiver(stopReceiver) }catch(_:Exception){};watchdog?.let { handler.removeCallbacks(it) }
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
