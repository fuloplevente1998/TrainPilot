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
        override fun onReceive(context: Context?, intent: Intent?) { if(intent?.action==SYNC_PAUSE)syncWorkoutPause() else end() }
    }
    override fun onCreate() {
        super.onCreate()
        if(Build.VERSION.SDK_INT>=33)registerReceiver(stopReceiver,IntentFilter(STOP).apply { addAction(SYNC_PAUSE) },Context.RECEIVER_NOT_EXPORTED)
        else registerReceiver(stopReceiver,IntentFilter(STOP).apply { addAction(SYNC_PAUSE) })
    }
    private var pauseInFlight=false
    private var appliedPause=false
    private var pauseRetries=0
    private fun pauseFailed(error: Exception) {
        pauseInFlight=false
        val message=error.cause?.message ?: error.message.orEmpty()
        if(WearHealthFutures.isRecoverableHealthError(message)&&pauseRetries<3) {
            pauseRetries++
            handler.postDelayed({ if(!destroyed&&lifecycle?.finished!=true)syncWorkoutPause() },2000L)
        }else futureFailed(error)
    }
    private fun syncWorkoutPause() {
        val data=summary ?: return
        val session=lifecycle ?: return
        if(!session.owned||session.stopRequested||session.finished||destroyed||pauseInFlight)return
        val workout=WorkoutSnapshotStore.load(this) ?: return
        if(workout.workoutId!=data.optString("workoutId"))return
        val desired=workout.workoutPaused
        if(data.optBoolean("workoutPauseRequested201")!=desired||!data.has("workoutPauseRequested201")){data.put("workoutPauseRequested201",desired);persist()}
        if(desired==appliedPause)return
        pauseInFlight=true
        try {
            WearHealthFutures.awaitCompletion(
                if(desired)client.pauseExerciseAsync() else client.resumeExerciseAsync(),
                executor,
                { !destroyed && lifecycle?.finished!=true },
                onFailure = ::pauseFailed,
                onSuccess = {
                    pauseInFlight=false;pauseRetries=0;appliedPause=desired
                    data.put("workoutPauseApplied201",desired).put("state",if(desired)"paused" else "active");persist()
                    syncWorkoutPause() // A newer explicit continuation may have arrived during the future.
                }
            )
        }catch(error:Exception){pauseFailed(error)}
    }

    private var destroyed=false
    private var watchdog: Runnable?=null
    private var sensorWatchdog: Runnable?=null
    private var reconnectAttempts=0
    private val reconnectRunnable=Runnable {
        if(!destroyed && lifecycle?.finished!=true) {
            try { client.setUpdateCallback(executor,this) } catch(error:Exception) { onRegistrationFailed(error) }
        }
    }
    private val handler=Handler(Looper.getMainLooper())
    private var lastLiveAt=0L
    private var lastLiveState=""
    private var lastUpdateReceivedAt=0L
    private fun scheduleSensorWatchdog() {
        if(destroyed || lifecycle?.finished==true)return
        sensorWatchdog?.let { handler.removeCallbacks(it) }
        sensorWatchdog=Runnable {
            checkSensorHealth()
            scheduleSensorWatchdog()
        }.also { handler.postDelayed(it, 15000L) }
    }
    private fun checkSensorHealth() {
        val data=summary ?: return
        val session=lifecycle ?: return
        if(destroyed || session.finished || !session.owned || session.stopRequested)return
        val now=SystemClock.elapsedRealtime()
        if(data.optString("state")=="active" && lastUpdateReceivedAt>0L && (now-lastUpdateReceivedAt>30000L)) {
            WearHealthFutures.awaitValue(
                client.getCurrentExerciseInfoAsync(),
                executor,
                { !destroyed && lifecycle?.finished!=true },
                onFailure = { /* Transient check failure - keep current active state */ },
                onSuccess = { info ->
                    if(lifecycle!==session || summary!==data || session.finished || session.stopRequested || destroyed)return@awaitValue
                    if(info.exerciseTrackedStatus!=ExerciseTrackedStatus.OWNED_EXERCISE_IN_PROGRESS) {
                        fail("A mérés megszakadt a háttérben.")
                    } else {
                        if(data.optString("heartRateAvailability")!="acquiring") {
                            data.put("heartRateAvailability","acquiring")
                            persist()
                        }
                    }
                }
            )
        }
    }
    override fun onBind(intent: Intent?) = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(intent?.action==STOP) { end();return START_NOT_STICKY }
        val id=intent?.getStringExtra("workoutId") ?: WearHealthStore.active(this)
        if(id.isBlank()) { stopSelf();return START_NOT_STICKY }
        if(summary?.optString("workoutId")==id){syncWorkoutPause();return START_STICKY}
        if(summary!=null) { end();return START_STICKY }
        val oldId=WearHealthStore.active(this)
        val closePrevious=oldId.isNotBlank() && oldId!=id
        summary=WearHealthStore.begin(this,if(closePrevious)oldId else id)
        if(summary!!.optString("state") in listOf("ended","error")) { stopSelf();return START_NOT_STICKY }
        appliedPause=summary!!.optBoolean("workoutPauseApplied201",summary!!.optString("state")=="paused")
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
            val access=wearRecordingAccess(this,request.gps,WearHealthStore.providerRequiresLocation(this))
            if(access.locationBlocked && !session.stopRequested){fail("A méréshez helyhozzáférés szükséges.",access);return START_NOT_STICKY}
            val location=access.foregroundLocation
            if(Build.VERSION.SDK_INT>=34) startForeground(129,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH or if(location)ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0)
            else startForeground(129,notification,if(location)ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0)
            WearHealthStore.save(this,summary!!)
            afterRegistration = {
                await(client.getCurrentExerciseInfoAsync()) { info ->
                    when(info.exerciseTrackedStatus) {
                        ExerciseTrackedStatus.OTHER_APP_IN_PROGRESS -> fail("Másik alkalmazás már edzést mér. A TrainPilot nem szakította meg.")
                        ExerciseTrackedStatus.OWNED_EXERCISE_IN_PROGRESS -> {
                            session.confirmOwnership()
                            lastUpdateReceivedAt=SystemClock.elapsedRealtime()
                            summary?.put("state",if(session.stopRequested)"ending" else if(appliedPause)"paused" else "active");persist()
                            scheduleSensorWatchdog()
                            if(session.stopRequested)end() else syncWorkoutPause()
                        }
                        else -> if(session.stopRequested){finishWithoutExercise()}else capabilities(request.exerciseType,access.gpsEnabled)
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
            val location=wearRecordingAccess(this,gps,WearHealthStore.providerRequiresLocation(this)).locationAvailable
            val type=wearRecordingType(wanted,capabilities.supportedExerciseTypes,location)
            if(type==null) { fail("Ezen az órán ez az edzéstípus nem mérhető.");return@await }
            val supported=capabilities.getExerciseTypeCapabilities(type).supportedDataTypes
            val heart=checkSelfPermission(if(Build.VERSION.SDK_INT>=36) "android.permission.health.READ_HEART_RATE" else "android.permission.BODY_SENSORS")==android.content.pm.PackageManager.PERMISSION_GRANTED
            val activity=checkSelfPermission("android.permission.ACTIVITY_RECOGNITION")==android.content.pm.PackageManager.PERMISSION_GRANTED
            val metrics=wearRecordingMetrics(supported,heart,activity,location)
            if(metrics.isEmpty()) { fail("Az óra nem kínál támogatott mérési adatot.");return@await }
            val config=ExerciseConfig(type,metrics,isAutoPauseAndResumeEnabled=false,isGpsEnabled=gps && location && type in listOf(ExerciseType.RUNNING,ExerciseType.WALKING,ExerciseType.BIKING))
            summary?.let { if(it.optDouble("activeDurationSeconds",0.0)>0)it.put("partial",true);WearHealthAccumulator(it).beginSegment()
                it.put("exerciseType",type.toString()).put("gps",config.isGpsEnabled) }
            persist()
            awaitCompletion(client.startExerciseAsync(config)) {
                lifecycle?.confirmOwnership();appliedPause=false
                summary?.put("workoutPauseApplied201",false)
                summary?.put("state",if(lifecycle?.stopRequested==true)"ending" else "active");persist()
                lastUpdateReceivedAt=SystemClock.elapsedRealtime()
                scheduleSensorWatchdog()
                if(lifecycle?.stopRequested==true)end() else syncWorkoutPause()
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
        reconnectAttempts=0
        summary?.let {
            if(it.optString("state")=="reconnecting") {
                it.put("state",if(appliedPause)"paused" else "active")
                persist()
            }
        }
        val action=afterRegistration;afterRegistration=null
        if(!destroyed && lifecycle?.finished!=true)try { action?.invoke() }catch(error:Exception) { futureFailed(error) }
    }
    override fun onRegistrationFailed(throwable: Throwable) {
        if(destroyed || lifecycle?.finished==true)return
        if(reconnectAttempts<3 && lifecycle?.stopRequested!=true) {
            reconnectAttempts++
            summary?.put("state","reconnecting")?.put("heartRateAvailability","acquiring")
            persist()
            handler.removeCallbacks(reconnectRunnable)
            handler.postDelayed(reconnectRunnable,reconnectAttempts*2000L)
        } else {
            fail("A mérési kapcsolat nem indult: ${throwable.message}")
        }
    }
    override fun onAvailabilityChanged(dataType: DataType<*,*>, availability: Availability) {
        if(dataType==DataType.HEART_RATE_BPM) {
            val status=when(availability) {
                DataTypeAvailability.AVAILABLE -> "available"
                DataTypeAvailability.ACQUIRING -> "acquiring"
                DataTypeAvailability.UNAVAILABLE -> "unavailable"
                else -> "unavailable"
            }
            summary?.let { data ->
                data.put("heartRateAvailability",status)
                persist()
            }
        }
    }
    override fun onLapSummaryReceived(lapSummary: ExerciseLapSummary) {}
    override fun onExerciseUpdateReceived(update: ExerciseUpdate) {
        val data=summary ?: return
        if(lifecycle?.finished==true || destroyed || data.optString("state")=="error")return
        lastUpdateReceivedAt=SystemClock.elapsedRealtime()
        val accumulator=WearHealthAccumulator(data)
        val currentOrigin=System.currentTimeMillis()-SystemClock.elapsedRealtime()
        val oldOrigin=data.optLong("bootOrigin",currentOrigin)
        val origin=if(kotlin.math.abs(currentOrigin-oldOrigin)<10000)oldOrigin else currentOrigin
        data.put("bootOrigin",origin)
        update.latestMetrics.getData(DataType.HEART_RATE_BPM).sortedBy { it.timeDurationFromBoot }.forEach { accumulator.heartRate(origin+it.timeDurationFromBoot.toMillis(),it.value) }
        accumulator.segmentTotal("totalCalories",update.latestMetrics.getData(DataType.CALORIES_TOTAL)?.total)
        accumulator.segmentTotal("steps",update.latestMetrics.getData(DataType.STEPS_TOTAL)?.total?.toDouble())
        accumulator.segmentTotal("distanceMeters",update.latestMetrics.getData(DataType.DISTANCE_TOTAL)?.total)
        update.latestMetrics.getData(DataType.SPEED).sortedBy { it.timeDurationFromBoot }.forEach {
            accumulator.sample("speedMps",it.value)
            accumulator.total("maxSpeedMps",it.value)
        }
        update.activeDurationCheckpoint?.let { checkpoint ->
            val elapsed=if(update.exerciseStateInfo.state.isPaused || update.exerciseStateInfo.state.isEnded)0L else java.time.Duration.between(checkpoint.time,java.time.Instant.now()).toMillis().coerceAtLeast(0L)
            accumulator.segmentTotal("activeDurationSeconds",(checkpoint.activeDuration.toMillis()+elapsed)/1000.0)
        }
        appliedPause=update.exerciseStateInfo.state.isPaused
        data.put("workoutPauseApplied201",appliedPause)
        val ended=update.exerciseStateInfo.state.isEnded
        data.put("state",if(ended)"ended" else if(lifecycle?.stopRequested==true)"ending" else if(update.exerciseStateInfo.state.isPaused)"paused" else "active")
        persist()
        if(ended)finish() else syncWorkoutPause()
    }
    private fun persist() { summary?.let {
        WearHealthStore.save(this,it);WearSurfaces.refresh(this)
        val now=SystemClock.elapsedRealtime();val state=it.optString("state")
        if(state!=lastLiveState || now-lastLiveAt>=5000L) {
            lastLiveAt=now;lastLiveState=state
            WearLiveHealth.publish(this,it.optString("workoutId"),state,it.optString("message"),it)
        }
    } }
    private fun finishWithoutExercise() {
        summary?.put("partial",true);finish()
    }
    private fun finish() {
        if(lifecycle?.finish()!=true)return
        watchdog?.let { handler.removeCallbacks(it) };watchdog=null
        sensorWatchdog?.let { handler.removeCallbacks(it) };sensorWatchdog=null
        handler.removeCallbacks(reconnectRunnable)
        persist();summary?.let { WearHealthStore.publish(this,it) };stopSelf()
    }
    private fun fail(message: String, blockedAccess: WearRecordingAccess?=null) {
        if(lifecycle?.finished==true)return
        lifecycle?.finish()
        val locationMissing=WearHealthFutures.isMissingLocationError(message)
        if(locationMissing)WearHealthStore.requireProviderLocation(this)
        summary?.put("state","error")?.put("partial",true)?.put("message",message)
        if(locationMissing || blockedAccess!=null)summary?.let {
            wearApplyLocationFailure(it,blockedAccess ?: wearRecordingAccess(this,
                WearHealthStore.gps(this),WearHealthStore.providerRequiresLocation(this)),message)
        }
        persist();summary?.let { WearHealthStore.publish(this,it) }
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
        sensorWatchdog?.let { handler.removeCallbacks(it) };sensorWatchdog=null
        handler.removeCallbacks(reconnectRunnable)
        if(registered)try { client.clearUpdateCallbackAsync(this) }catch(_:Exception){}
        super.onDestroy()
    }
    companion object {
        const val SYNC_PAUSE="com.repforge.app.wear.SYNC_WORKOUT_PAUSE"
        fun syncPause(context: Context) {
            val workout=WorkoutSnapshotStore.load(context) ?: return
            if(workout.workoutPaused){WearStopwatchStore.pause(context);context.getSystemService(Vibrator::class.java)?.cancel()}
            context.sendBroadcast(Intent(SYNC_PAUSE).setPackage(context.packageName))
        }
        const val STOP="com.repforge.app.wear.STOP_HEALTH"
        fun start(context: Context, workout: WearWorkout) {
            if(workout.workoutPaused)return
            val request=wearRecordingRequestForWorkout(workout,WearHealthStore.gps(context))
            context.startForegroundService(Intent(context,WearHealthService::class.java).putExtra("workoutId",workout.workoutId).putExtra("exerciseType",request.exerciseType).putExtra("gps",request.gps))
        }
        fun stop(context: Context) { context.sendBroadcast(Intent(STOP).setPackage(context.packageName)) }
    }
}
