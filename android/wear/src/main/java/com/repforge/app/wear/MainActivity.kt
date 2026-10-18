package com.repforge.app.wear

import android.Manifest
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.wear.remote.interactions.RemoteActivityHelper
import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import org.json.JSONObject
import java.time.Instant
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

class MainActivity : ComponentActivity(), DataClient.OnDataChangedListener {
    private val workout = mutableStateOf<WearWorkout?>(null)
    private val home = mutableStateOf<WatchHomeSnapshot?>(null)
    private var lastHomeRequestAt = 0L
    private val saveSync = mutableStateOf<WearSaveSyncState?>(null)
    private val closure = mutableStateOf<WearClosure?>(null)
    private val restRemaining = mutableIntStateOf(0)
    private var restTimer: CountDownTimer? = null
    private val health=mutableStateOf<JSONObject?>(null)
    private val routeRequest=mutableStateOf<String?>(null)
    private val measuring=mutableStateOf(false)
    private val gpsEnabled=mutableStateOf(false)
    private val gpsRoute193=mutableStateOf<JSONObject?>(null)
    private data class GpsRequest193(val id: String,val epoch: Long,val setId: String,val at: Long)
    private var gpsRequest193: GpsRequest193?=null
    private var gpsPermission193=false
    private var gpsSettings193=false
    private var gpsResolving193=false
    private val gpsTimeout193=Runnable { if(gpsResolving193)gpsFailed193("unavailable") }
    private val phoneConnected=mutableStateOf<Boolean?>(null)
    private val batteryLevel=mutableStateOf<Int?>(null)
    private var lastIndicatorRefreshAt=0L
    private val uiHandler=Handler(Looper.getMainLooper())
    private var activityResumed=false
    private var remoteMeasurementRequested=false
    private var remoteMeasurementStart: WearMeasurementStart?=null
    private var remoteStartNoticeShown=false
    private var remoteStartFinished=false
    private var remoteWorkoutAccepted=false
    private var lastRemoteRefreshAt=0L
    private fun releaseRemoteStart() {
        remoteStartFinished=true;uiHandler.removeCallbacks(remoteStartTimeout)
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);setTurnScreenOn(false)
    }
    private val remoteStartTimeout=Runnable {
        releaseRemoteStart()
        val current=workout.value
        if(activityResumed && remoteMeasurementRequested) {
            val pending=remoteMeasurementStart ?: return@Runnable
            val message=if(!pending.matches(current?.workoutId,current?.revision ?: 0L))
                "Az edzés még nem érkezett meg. Ellenőrizd a telefon és az óra kapcsolatát."
            else "A mérés indulása még nem igazolt. Nyisd meg az Órás mérések oldalt."
            WearLiveHealth.publish(this,pending.workoutId,"startTimeout",message)
            Toast.makeText(this,message,Toast.LENGTH_LONG).show()
        }
    }
    private val healthTick=object : Runnable {
        override fun run() {
            refreshHealth();refreshHomeIndicators()
            WearCommandOutbox.flush(this@MainActivity)
            refreshSaveSync()
            if(workout.value==null && WearHealthStore.active(this@MainActivity).isNotBlank())WearHealthService.stop(this@MainActivity)
            if(remoteMeasurementRequested && !remoteWorkoutAccepted && !remoteStartFinished && remoteMeasurementStart?.matches(workout.value?.workoutId,workout.value?.revision ?: 0L)!=true && android.os.SystemClock.elapsedRealtime()-lastRemoteRefreshAt>=2000L) {
                lastRemoteRefreshAt=android.os.SystemClock.elapsedRealtime()
                val cached=WorkoutSnapshotStore.load(this@MainActivity)
                if(remoteMeasurementStart?.matches(cached?.workoutId,cached?.revision ?: 0L)==true) {
                    workout.value=cached;restoreRestFromSnapshot(cached);startHealthIfEnabled()
                } else remoteMeasurementStart?.let { WearWorkoutHandoff.request(this@MainActivity,it) }
                refreshFromDataLayer()
            }
            if(WearHealthStore.active(this@MainActivity).isBlank() && workout.value?.workoutId?.let { WearHealthStore.load(this@MainActivity,it)==null }==true)startHealthIfEnabled()
            uiHandler.postDelayed(this,1000)
        }
    }
    private val locationSettingsRequest=registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // Returning without enabling location must not reopen Settings in a loop.
        if(WearHealthStore.enabled(this) && recordingAccess().systemLocationEnabled)continueHealthRequest()
        else refreshHealth()
    }
    private val permissionRequest=registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        val allowed=healthAllowed()
        WearHealthStore.enable(this,allowed);measuring.value=allowed
        val gpsDenied=WearHealthStore.gps(this) && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED
        if(gpsDenied)WearHealthStore.gps(this,false)
        gpsEnabled.value=WearHealthStore.gps(this)
        if(allowed){
            continueHealthRequest()
            if(gpsDenied)Toast.makeText(this,"Pontos helyengedély nélkül az órás GPS kikapcsolva marad. Az engedélyezett szenzorok mérése folytatódhat.",Toast.LENGTH_LONG).show()
        } else Toast.makeText(this,"Órás méréshez engedély szükséges.",Toast.LENGTH_LONG).show()
    }
    private val routePermission193=registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        gpsPermission193=false;gpsRequest193=gpsRequest193?.copy(at=android.os.SystemClock.elapsedRealtime())
        if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED)uiHandler.post { continueGps193() }
        else gpsFailed193("permission")
    }
    private val routeSettings193=registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        gpsSettings193=false;gpsRequest193=gpsRequest193?.copy(at=android.os.SystemClock.elapsedRealtime())
        val manager=getSystemService(android.location.LocationManager::class.java)
        if(manager?.isLocationEnabled==true&&manager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER))uiHandler.post { continueGps193() }
        else gpsFailed193("locationOff")
    }
    private fun gpsAllowed193(current: WearWorkout?)=current!=null&&WearGps193.owns(current)&&
        WearGps193.distance(current.exercise)&&current.currentSet?.let { !it.done&&it.setId.isNotBlank() }==true
    private fun gpsFailed193(state: String) {
        val pending=gpsRequest193;gpsRequest193=null;gpsResolving193=false;gpsPermission193=false;gpsSettings193=false;uiHandler.removeCallbacks(gpsTimeout193)
        if(pending!=null)WearGps193.update(this,pending.id,state,WearGps193.status(this,pending.id).optInt("points"))
        if(activityResumed&&pending?.id==WorkoutSnapshotStore.load(this)?.workoutId)Toast.makeText(this,WearGpsText193.text(state),Toast.LENGTH_LONG).show()
        refreshHealth()
    }
    private fun requestGps193() {
        val current=WorkoutSnapshotStore.load(this)
        if(!activityResumed||gpsRequest193!=null)return
        if(!gpsAllowed193(current)){Toast.makeText(this,WearGpsText193.text("distanceOnly"),Toast.LENGTH_LONG).show();return}
        gpsRequest193=GpsRequest193(current!!.workoutId,current.ownerEpoch193,current.currentSet!!.setId,android.os.SystemClock.elapsedRealtime())
        val missing=mutableListOf<String>()
        if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)missing.addAll(listOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION))
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)missing.add(Manifest.permission.POST_NOTIFICATIONS)
        if(missing.isNotEmpty()){gpsPermission193=true;routePermission193.launch(missing.toTypedArray())}else continueGps193()
    }
    private fun continueGps193() {
        if(!activityResumed||gpsPermission193||gpsSettings193||gpsResolving193)return
        val request=gpsRequest193 ?: return
        fun current(): WearWorkout?=WorkoutSnapshotStore.load(this)?.takeIf { gpsAllowed193(it)&&it.workoutId==request.id&&it.ownerEpoch193==request.epoch&&it.currentSet?.setId==request.setId&&android.os.SystemClock.elapsedRealtime()-request.at<=15000 }
        if(current()==null){gpsFailed193("unavailable");return}
        if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){gpsFailed193("permission");return}
        val manager=getSystemService(android.location.LocationManager::class.java)
        if(manager?.allProviders?.contains(android.location.LocationManager.GPS_PROVIDER)!=true){gpsFailed193("unavailable");return}
        if(!manager.isLocationEnabled||!manager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)){
            gpsSettings193=true;try{routeSettings193.launch(wearLocationSettingsIntent())}catch(_:Exception){gpsFailed193("locationOff")};return
        }
        gpsResolving193=true;uiHandler.postDelayed(gpsTimeout193,15000)
        Wearable.getNodeClient(this).localNode.addOnSuccessListener { local ->
            if(gpsRequest193!==request)return@addOnSuccessListener
            val latest=current();if(!activityResumed||latest==null){gpsFailed193("unavailable");return@addOnSuccessListener}
            val h=latest.handoff193?.let(::JSONObject)
            val target=if(h!=null)h.optString("sourceNode")else WatchHomeStore.phoneNode193(this)
            if(!com.repforge.shared.GpsRoutePacket193.id(target)||h!=null&&(h.optString("target")!="watch"||h.optString("targetNode")!=local.id)) { gpsFailed193("unavailable");return@addOnSuccessListener }
            try {
                WearGps193.arm(this,latest.workoutId,true,local.id,target);gpsRequest193=null;gpsResolving193=false;uiHandler.removeCallbacks(gpsTimeout193)
                WearGps193.update(this,latest.workoutId,"acquiring",WearGps193.status(this,latest.workoutId).optInt("points"));WearGpsRouteService193.start(this,latest,local.id,target);refreshHealth()
            }catch(_:SecurityException){WearGps193.update(this,latest.workoutId,"permission");refreshHealth()}
             catch(_:Exception){WearGps193.update(this,latest.workoutId,"storage");refreshHealth()}
        }.addOnFailureListener { if(gpsRequest193===request)gpsFailed193("unavailable") }
    }
    private fun stopGps193() {
        gpsRequest193=null;gpsResolving193=false;uiHandler.removeCallbacks(gpsTimeout193)
        val current=WorkoutSnapshotStore.load(this) ?: return
        try { WearGps193.arm(this,current.workoutId,false);WearGpsRouteService193.freeze(this);WearGps193.update(this,current.workoutId,"stopped",WearGps193.status(this,current.workoutId).optInt("points")) }
        catch(_:Exception){WearGps193.update(this,current.workoutId,"storage")};refreshHealth()
    }
    private fun healthAllowed(): Boolean = checkSelfPermission(if(Build.VERSION.SDK_INT>=36) "android.permission.health.READ_HEART_RATE" else Manifest.permission.BODY_SENSORS)==PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION)==PackageManager.PERMISSION_GRANTED
    private fun recordingAccess(): WearRecordingAccess = wearRecordingAccess(this,WearHealthStore.gps(this),
        WearHealthStore.providerRequiresLocation(this))
    private fun refreshHealth() {
        // Data Layer message replies update the durable snapshot independently
        // of Activity callbacks. Resolve both the workout and metrics afresh.
        val current=WorkoutSnapshotStore.load(this)
        if(workout.value!=current) {
            workout.value=current;restoreRestFromSnapshot(current)
        }
        if(current?.automation?.enabled==true)restoreRestFromSnapshot(current)
        val published=WearLiveHealth.current(this)
        val latest=if(current==null)WearHealthStore.latest(this) else null
        val id=current?.workoutId ?: closure.value?.takeIf { it.status in listOf("pending","error") }?.workoutId
            ?: latest?.optString("workoutId") ?: ""
        health.value=wearHealthDisplaySummary(id,WearHealthStore.load(this,id),published)
        measuring.value=WearHealthStore.enabled(this);gpsEnabled.value=WearHealthStore.gps(this)
        val route=current?.takeIf { it.ownershipProtocol193==1 }?.let { WearGps193.status(this,it.workoutId) }
        if(gpsRoute193.value?.toString()!=route?.toString())gpsRoute193.value=route
        if(remoteMeasurementRequested && !remoteStartFinished && remoteMeasurementStart?.matches(workout.value?.workoutId,workout.value?.revision ?: 0L)==true && health.value?.optString("state") in listOf("active","paused","ended","error"))releaseRemoteStart()
    }
    private fun refreshHomeIndicators(force: Boolean=false) {
        val now=android.os.SystemClock.elapsedRealtime()
        if(!force && lastIndicatorRefreshAt>0 && now-lastIndicatorRefreshAt<10000L)return
        lastIndicatorRefreshAt=now
        val battery=registerReceiver(null,IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level=battery?.getIntExtra(BatteryManager.EXTRA_LEVEL,-1) ?: -1
        val scale=battery?.getIntExtra(BatteryManager.EXTRA_SCALE,-1) ?: -1
        batteryLevel.value=if(level>=0 && scale>0)(level*100L/scale).toInt().coerceIn(0,100)else null
        Wearable.getNodeClient(this).connectedNodes
            .addOnSuccessListener { phoneConnected.value=it.isNotEmpty() }
            .addOnFailureListener { phoneConnected.value=null }
    }
    private fun requestHealth(gps: Boolean=false) {
        // An explicit measurement action uses the current durable workout. A
        // previous phone launch must not keep a later workout behind its guard.
        workout.value=WorkoutSnapshotStore.load(this)
        remoteMeasurementRequested=false;remoteMeasurementStart=null;releaseRemoteStart()
        WearHealthStore.gps(this,gps)
        if(workout.value?.let { WearHealthStore.load(this,it.workoutId) }?.let(::wearHasLocationFailure)==true)
            WearHealthStore.requireProviderLocation(this)
        val permissions=mutableListOf(if(Build.VERSION.SDK_INT>=36) "android.permission.health.READ_HEART_RATE" else Manifest.permission.BODY_SENSORS,Manifest.permission.ACTIVITY_RECOGNITION)
        if(Build.VERSION.SDK_INT>=33)permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        if(recordingAccess().needsFineLocation)permissions.addAll(listOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION))
        val missing=permissions.filter { checkSelfPermission(it)!=PackageManager.PERMISSION_GRANTED }
        if(missing.isEmpty()) {
            WearHealthStore.enable(this,healthAllowed());continueHealthRequest()
        } else permissionRequest.launch(missing.toTypedArray())
    }
    private fun continueHealthRequest() {
        val access=recordingAccess()
        if(access.locationBlocked){
            workout.value?.let { WearHealthStore.blockForLocation(this,it.workoutId,access) }
            refreshHealth()
            if(access.fineLocationGranted && access.locationServicesBlocked){
                try { locationSettingsRequest.launch(wearLocationSettingsIntent()) }
                catch(_:android.content.ActivityNotFoundException) {
                    Toast.makeText(this,WEAR_LOCATION_SERVICES_MESSAGE,Toast.LENGTH_LONG).show()
                }
            } else Toast.makeText(this,WEAR_LOCATION_PERMISSION_MESSAGE,Toast.LENGTH_LONG).show()
            return
        }
        workout.value?.let { WearHealthStore.retry(this,it.workoutId) }
        refreshHealth();startHealthIfEnabled()
    }
    private fun startHealthIfEnabled() {
        WearHealthService.syncPause(this)
        if(!activityResumed)return
        val current=workout.value ?: return
        if(current.workoutPaused)return
        if(!wearMeasurementMayStart(remoteMeasurementRequested,remoteWorkoutAccepted,remoteMeasurementStart,current.workoutId,current.revision))return
        if(remoteMeasurementRequested)remoteWorkoutAccepted=true
        var previous=WearHealthStore.load(this,current.workoutId)
        if(previous?.let(::wearHasLocationFailure)==true)
            WearHealthStore.requireProviderLocation(this)
        val access=recordingAccess()
        val locationRequired=access.locationBlocked
        if(locationRequired && WearHealthStore.enabled(this))WearHealthStore.blockForLocation(this,current.workoutId,access)
        if(remoteMeasurementRequested){
            if(!remoteStartNoticeShown){
                remoteStartNoticeShown=true
                val state=if(!WearHealthStore.enabled(this))"disabled" else if(!healthAllowed()||locationRequired)"permissionRequired" else null
                if(state!=null) {
                    val message=when {
                        state=="disabled" -> "Órás mérés kikapcsolva. Az Órás mérések oldalon bekapcsolhatod."
                        locationRequired && access.fineLocationGranted && access.locationServicesBlocked -> WEAR_LOCATION_SERVICES_MESSAGE
                        locationRequired -> WEAR_LOCATION_PERMISSION_MESSAGE
                        else -> "Az órás méréshez szenzorengedély szükséges."
                    }
                    WearLiveHealth.publish(this,current.workoutId,state,message);releaseRemoteStart()
                    routeRequest.value="metrics";Toast.makeText(this,message,Toast.LENGTH_LONG).show()
                }
            }
        }
        if(!WearHealthStore.enabled(this) || !healthAllowed() || locationRequired)return
        if(previous?.optString("state")=="error" && (wearHasLocationFailure(previous) || WearHealthFutures.isLegacyNullCompletionError(previous.optString("message")))) {
            // Retry known startup failures with the corrected config; keep measured totals.
            WearHealthStore.retry(this,current.workoutId)
            previous=WearHealthStore.load(this,current.workoutId)
        }
        if(previous?.optString("state") in listOf("ended","error")) {
            if(remoteMeasurementRequested && !remoteStartFinished) {
                WearLiveHealth.publish(this,current.workoutId,previous!!.optString("state"),previous.optString("message"),previous);releaseRemoteStart()
            }
            return
        }
        try { WearHealthService.start(this,current) }catch(error:Exception) {
            val message="A mérés nem indítható: ${error.message}"
            WearLiveHealth.publish(this,current.workoutId,"error",message);releaseRemoteStart()
            Toast.makeText(this,message,Toast.LENGTH_LONG).show()
        }
    }
    private fun readLaunchIntent(intent: Intent) {
        val uri=intent.data
        if(intent.action==Intent.ACTION_VIEW&&uri?.scheme=="trainpilot"&&uri.host=="wear"&&uri.path=="/automatic-command"){
            val raw=uri.getQueryParameter("command")
            if(raw!=null&&raw.length<16000)try {
                val command=JSONObject(raw);val current=WorkoutSnapshotStore.load(this);val a=current?.automation
                if(command.optString("action")=="undo"){
                    WearAutomaticController.remote(this,command);workout.value=WorkoutSnapshotStore.load(this);restoreRestFromSnapshot(workout.value)
                }
                if(command.optString("action")!="undo"&&current!=null&&current.timerOwner=="watch"&&current.workoutId==command.optString("workoutId")&&
                    command.optString("key")==a?.key.orEmpty()&&System.currentTimeMillis()-command.optLong("createdAt") in 0..15000&&
                    (command.optString("action")=="toggle"||command.optLong("phaseNumber")==a?.phaseNumber)&&
                    a?.handledCommands?.contains(command.optString("commandId"))!=true){
                    if(command.optString("action")=="toggle")workout.value=WearAutomaticController.toggle(this,command.optBoolean("enabled"))
                    else {
                        WearAutomaticService.ensure(this,current)
                        if(command.optString("action")=="record"){
                            val e=command.optInt("exerciseIndex",-1);val s=command.optInt("setIndex",-1)
                            if(current.exercises.getOrNull(e)?.sets?.getOrNull(s)?.setId==command.optString("setId"))
                                workout.value=WearAutomaticController.record(this,e,s,command.optString("reps"),command.optString("weight"),command.optInt("leftSeconds"),command.optInt("rightSeconds"),command.optString("commandId"))
                        }else workout.value=WearAutomaticController.command(this,command.optString("action"),current.workoutId)
                    }
                    workout.value?.automation?.let { state -> workout.value=WorkoutSnapshotStore.saveWorkout(this,workout.value!!.copy(automation=state.copy(handledCommands=(state.handledCommands+command.optString("commandId")).takeLast(128)))) }
                    workout.value?.let { WearAutomaticController.publish(this,it) };restoreRestFromSnapshot(workout.value)
                }
            }catch(_:Exception){}
            setTurnScreenOn(true);uiHandler.postDelayed({setTurnScreenOn(false)},5000);routeRequest.value="workout";return
        }
        remoteMeasurementRequested=intent.action==Intent.ACTION_VIEW && uri?.scheme=="trainpilot" && uri?.host=="wear" && uri?.path=="/start-measurement"
        remoteMeasurementStart=if(remoteMeasurementRequested)WearMeasurementStart.parse(uri?.getQueryParameter("workoutId"),uri?.getQueryParameter("revision"))else null
        remoteStartNoticeShown=false;remoteStartFinished=false;remoteWorkoutAccepted=false;lastRemoteRefreshAt=0L;uiHandler.removeCallbacks(remoteStartTimeout)
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setTurnScreenOn(remoteMeasurementRequested && remoteMeasurementStart!=null)
        if(remoteMeasurementRequested && remoteMeasurementStart!=null){
            // Hold only the initial handoff, then let the screen sleep normally.
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            uiHandler.postDelayed(remoteStartTimeout,15000)
            WearLiveHealth.publish(this,remoteMeasurementStart!!.workoutId,"waitingWorkout")
            WearWorkoutHandoff.request(this,remoteMeasurementStart!!)
        }
        routeRequest.value=if(remoteMeasurementRequested)"workout" else intent.getStringExtra("destination")?.takeIf { it in listOf("home","workout","summary","menu","calendar","gpsRoute193") }
        if(remoteMeasurementRequested && remoteMeasurementStart==null)Toast.makeText(this,"Érvénytelen edzésindítás. Nyisd meg az aktuális edzést.",Toast.LENGTH_LONG).show()
    }
    private fun openOnPhone() {
        try {
            val task=RemoteActivityHelper(this,mainExecutor).startRemoteActivity(Intent(Intent.ACTION_VIEW).setData(Uri.parse("trainpilot://wear/open")).addCategory(Intent.CATEGORY_BROWSABLE),null)
            task.addListener({ try { task.get();Toast.makeText(this,"Megnyitva a telefonon",Toast.LENGTH_SHORT).show() }catch(_:Exception) { Toast.makeText(this,"A telefon nem érhető el. Ellenőrizd a kapcsolatot.",Toast.LENGTH_LONG).show() } },mainExecutor)
        }catch(_:Exception) { Toast.makeText(this,"A telefon nem érhető el.",Toast.LENGTH_LONG).show() }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent);setIntent(intent);readLaunchIntent(intent)
        refreshFromDataLayer();startHealthIfEnabled()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WearUndo188.recover(this)
        workout.value = WearAutomaticController.restore(this)
        WearGps193.recover(this,!WearGpsRouteService193.active(workout.value?.workoutId.orEmpty()))
        home.value = WatchHomeStore.load(this)
        closure.value = WearClosureStore.load(this)
        restoreRestFromSnapshot(workout.value)
        refreshHealth()
        readLaunchIntent(intent)
        setContent {
            TrainPilotWearApp(
                workout = workout,
                home = home,
                restRemaining = restRemaining,
                closure = closure,
                saveSync = saveSync,
                onRetrySync = { id -> WearCommandOutbox.retry(this,id);refreshSaveSync() },
                health = health,
                measuring = measuring,
                gpsEnabled = gpsEnabled,
                gpsRoute193 = gpsRoute193,
                onStartGpsRoute193 = ::requestGps193,
                onStopGpsRoute193 = ::stopGps193,
                phoneConnected = phoneConnected,
                batteryLevel = batteryLevel,
                requestedRoute = routeRequest,
                onRouteConsumed = { routeRequest.value=null },
                onEnableHealth = { requestHealth(WearHealthStore.gps(this)) },
                onDisableHealth = { WearHealthStore.enable(this,false);WearHealthService.stop(this);refreshHealth() },
                onToggleGps = { if(WearHealthStore.gps(this)){WearHealthStore.gps(this,false);refreshHealth()}else requestHealth(true) },
                onOpenPhone = ::openOnPhone,
                onRefreshJournal = ::requestHomeSnapshot,
                onFinish = { closeWorkout("finishWorkout") },
                onWorkoutFeedback = ::saveWorkoutFeedback,
                onDiscard = { closeWorkout("discardWorkout") },
                onRetry = ::retryClosure,
                onDismissClosure = { if (closure.value?.status in listOf("saved", "discarded")) { WearClosureStore.dismiss(this); closure.value = null } },
                onResumeWorkout = {
                    val current=WorkoutSnapshotStore.load(this)
                    if(current?.ownershipProtocol193==1&&current.timerOwner=="watch") {
                        try {workout.value=WearOwnership193.resume(this);workout.value?.let { WearGps193.resumeIfArmed(this,it) };restoreRestFromSnapshot(workout.value)}
                        catch(_:Exception){workout.value=WorkoutSnapshotStore.load(this);android.widget.Toast.makeText(this,WearOwnershipText193.text("failed"),android.widget.Toast.LENGTH_LONG).show()}
                    }else if(current?.workoutPaused==true)sendCommand("resumeWorkout")
                },
                onRequestOwnership193 = { WearOwnership193.request(this) },
                onRetryOwnership193 = { WearOwnership193.recover(this);workout.value=WorkoutSnapshotStore.load(this) },
                onSelectExercise = ::selectExercise,
                onStart = ::startWorkoutFromWatch,
                onStartQuick = ::startQuickWorkoutFromWatch,
                onAddQuick = ::addQuickExerciseFromWatch,
                onChange = ::changeCurrentSet,
                onComplete = ::completeCurrentSet,
                onUndo = { id -> workout.value=WearUndo188.undo(this,id);restoreRestFromSnapshot(workout.value) },
                onPrevious = { navigateExercise(-1) },
                onNext = { navigateExercise(1) },
                onSkipRest = ::skipRest,
                onToggleAutomatic = {
                    val current=WorkoutSnapshotStore.load(this)
                    if(current!=null){
                        if(current.timerOwner=="phone")sendCommand("toggleAutomatic",value=current.automation?.enabled!=true)
                        else {workout.value=WearAutomaticController.toggle(this,current.automation?.enabled!=true);restoreRestFromSnapshot(workout.value)}
                    }
                },
                onEditorSave = ::saveEditorValue,
                onAutomaticCommand = { action ->
                    val current=WorkoutSnapshotStore.load(this)
                    if(current!=null){
                        if(current.timerOwner=="phone")sendCommand("automaticControl",value=if(action=="resume"&&current.automation?.key?.startsWith("undo188:")==true)"resumeUndo" else action)
                        else {workout.value=WearAutomaticController.command(this,action,current.workoutId);restoreRestFromSnapshot(workout.value)}
                    }
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        activityResumed=true
        WearOwnership193.recover(this)
        refreshHomeIndicators(true)
        WearUndo188.recover(this)
        workout.value = WearAutomaticController.restore(this)
        workout.value?.takeIf { it.automation?.enabled==true && it.automation.owner=="watch" }?.let { WearAutomaticService.ensure(this,it) }
        home.value = WatchHomeStore.load(this)
        closure.value = WearClosureStore.load(this)
        restoreRestFromSnapshot(workout.value)
        Wearable.getDataClient(this).addListener(this)
        refreshFromDataLayer()
        WearCommandOutbox.flush(this)
        uiHandler.removeCallbacks(healthTick);uiHandler.post(healthTick)
        if(workout.value==null && WearHealthStore.active(this).isNotBlank())WearHealthService.stop(this)
        startHealthIfEnabled()
        continueGps193()
    }

    override fun onPause() {
        activityResumed=false
        uiHandler.removeCallbacks(healthTick)
        Wearable.getDataClient(this).removeListener(this)
        super.onPause()
    }

    override fun onDestroy() {
        gpsRequest193=null;uiHandler.removeCallbacks(gpsTimeout193)
        uiHandler.removeCallbacks(remoteStartTimeout)
        restTimer?.cancel()
        super.onDestroy()
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type != DataEvent.TYPE_CHANGED) continue
            val item = event.dataItem
            if(item.uri.path?.startsWith(WearCommandOutbox.RECEIPT_PREFIX)==true) {
                if(WearCommandOutbox.acceptReceipt(this,item.uri.path,DataMapItem.fromDataItem(item).dataMap.getString("receipt"),item.uri.host))
                    Wearable.getDataClient(this).deleteDataItems(item.uri)
                runOnUiThread { refreshSaveSync() }
                continue
            }
            val raw = DataMapItem.fromDataItem(item).dataMap.getString("snapshot")
            when (item.uri.path) {
                WearPhoneAutomatic.PATH -> WearPhoneAutomatic.receive(this,DataMapItem.fromDataItem(item).dataMap.getString("automatic"))
                WearDataListenerService.ACTIVE_WORKOUT_PATH -> {
                    runOnUiThread {
                        val parsed = WorkoutSnapshotStore.save(this, raw)
                        workout.value = parsed
                        restoreRestFromSnapshot(parsed)
                        if(parsed==null)WearHealthService.stop(this) else startHealthIfEnabled()
                        WearSurfaces.refresh(this,true)
                    }
                }
                WearDataListenerService.WATCH_HOME_PATH -> {
                    val parsed = WatchHomeStore.save(this, raw, DataMapItem.fromDataItem(item).dataMap.getLong("publishedAt"),item.uri.host)
                    runOnUiThread { home.value = parsed; closure.value = WearClosureStore.load(this) }
                }
            }
        }
    }

    private fun requestHomeSnapshot() {
        val now = android.os.SystemClock.elapsedRealtime()
        if (lastHomeRequestAt > 0 && now - lastHomeRequestAt < 3000L) return
        lastHomeRequestAt = now
        WearCommandOutbox.enqueue(this, JSONObject()
            .put("schema", 1)
            .put("commandId", UUID.randomUUID().toString())
            .put("createdAt", System.currentTimeMillis())
            .put("action", "requestHome"))
        refreshFromDataLayer()
    }

    private fun refreshFromDataLayer() {
        Wearable.getDataClient(this).getDataItems()
            .addOnSuccessListener { items ->
                try {
                    var newestRaw: String? = null
                    var newestRevision = Long.MIN_VALUE
                    var newestHome: WatchHomeSnapshot? = null
                    for (item in items) {
                        try {
                            if(com.repforge.shared.OwnershipTransport193.receive(this,item) { message,origin,local ->
                                WearOwnership193.receive(this,message,origin,local)
                            })continue
                            if(item.uri.path?.startsWith(WearCommandOutbox.RECEIPT_PREFIX)==true) {
                                if(WearCommandOutbox.acceptReceipt(this,item.uri.path,DataMapItem.fromDataItem(item).dataMap.getString("receipt"),item.uri.host))
                                    Wearable.getDataClient(this).deleteDataItems(item.uri)
                                continue
                            }
                            val raw = DataMapItem.fromDataItem(item).dataMap.getString("snapshot") ?: continue
                            when (item.uri.path) {
                                WearDataListenerService.ACTIVE_WORKOUT_PATH -> {
                                    val revision = JSONObject(raw).optLong("revision", 0L)
                                    if (revision >= newestRevision) { newestRevision = revision; newestRaw = raw }
                                }
                                WearDataListenerService.WATCH_HOME_PATH -> newestHome = WatchHomeStore.save(this, raw,
                                    DataMapItem.fromDataItem(item).dataMap.getLong("publishedAt"),item.uri.host) ?: newestHome
                            }
                        } catch (_: Exception) { /* Keep the cache when a DataItem is malformed. */ }
                    }
                    newestRaw?.let {
                        workout.value = WorkoutSnapshotStore.save(this, it)
                        restoreRestFromSnapshot(workout.value)
                    }
                    if (newestHome != null) home.value = newestHome
                    closure.value = WearClosureStore.load(this)
                    refreshSaveSync()
                    // The remote Activity may arrive before the urgent DataItem.
                    startHealthIfEnabled()
                } finally {
                    items.release()
                }
            }
    }

    private fun startWorkoutFromWatch(day: WatchHomeDay, scheduleId: String) {
        val source = home.value ?: return
        if (workout.value != null || source.hasDraft || closure.value?.status in listOf("pending", "error") || day.exercises.isEmpty()) return
        remoteMeasurementRequested=false;remoteMeasurementStart=null;setTurnScreenOn(false)
        val started = Instant.ofEpochMilli(System.currentTimeMillis()).toString()
        val localWorkout = source.createWorkout(day, scheduleId, started)
        workout.value = WorkoutSnapshotStore.saveWorkout(this, localWorkout)
        sendStartCommand(day, scheduleId, started)
        startHealthIfEnabled();WearSurfaces.refresh(this,true)
    }

    private fun startQuickWorkoutFromWatch(exercise: WatchHomeExercise) {
        val source = home.value ?: return
        if (workout.value != null || source.hasDraft ||
            closure.value?.status in listOf("pending", "error") ||
            source.quickExercises.none { it.id == exercise.id }) return
        remoteMeasurementRequested=false;remoteMeasurementStart=null;setTurnScreenOn(false)
        val started = Instant.ofEpochMilli(System.currentTimeMillis()).toString()
        val localWorkout = source.createQuickWorkout(exercise, started)
        workout.value = WorkoutSnapshotStore.saveWorkout(this, localWorkout)
        val command = JSONObject()
            .put("schema", 1)
            .put("commandId", UUID.randomUUID().toString())
            .put("workoutId", started)
            .put("action", "startQuickWorkout")
            .put("exerciseId", exercise.id)
            .put("started", started)
            .put("createdAt", System.currentTimeMillis())
            .put("finalSnapshot",WorkoutSnapshotStore.encode(workout.value ?: return))
        val sequence = WearCommandOutbox.enqueue(this, command)
        workout.value?.let { workout.value = WorkoutSnapshotStore.saveWorkout(this, it.copy(localSequence = maxOf(it.localSequence,sequence))) }
        startHealthIfEnabled()
        WearSurfaces.refresh(this, true)
    }

    private fun addQuickExerciseFromWatch(exercise: WatchHomeExercise) {
        synchronized(WearAutomaticController) { synchronized(WorkoutSnapshotStore) {
        var current = WorkoutSnapshotStore.load(this) ?: return
        val source = home.value ?: return
        if (!current.quickWorkout || current.exercises.size >= 20 ||
            current.exercises.any { it.id == exercise.id } ||
            source.quickExercises.none { it.id == exercise.id }) return
        if(current.automation?.enabled==true&&current.automation.owner=="watch")
            current=WearAutomaticController.command(this,"disable",current.workoutId) ?: return
        val updated = current.copy(exercises = current.exercises + source.toWearExercise(exercise),
            currentExercise = current.exercises.size, restEndAt = 0L)
        workout.value = WorkoutSnapshotStore.saveWorkout(this, updated)
        sendCommand("addQuickExercise", exerciseIndex = updated.currentExercise)
        workout.value?.let { WearAutomaticController.publish(this,it) }
        WearSurfaces.refresh(this, true)

        }}
    }

    private fun sendStartCommand(day: WatchHomeDay, scheduleId: String, started: String) {
        val commandId=UUID.randomUUID().toString()
        val command = JSONObject()
            .put("schema", 1)
            .put("commandId", commandId)
            .put("workoutId", started)
            .put("action", "startWorkout")
            .put("programId", day.programId)
            .put("programName", day.programName)
            .put("dayId", day.id)
            .put("scheduleId", scheduleId)
            .put("started", started)
            .put("createdAt", System.currentTimeMillis())
            .put("finalSnapshot",WorkoutSnapshotStore.encode(workout.value ?: return))
        val sequence = WearCommandOutbox.enqueue(this, command)
        workout.value?.let { workout.value = WorkoutSnapshotStore.saveWorkout(this, it.copy(localSequence = maxOf(it.localSequence,sequence))) }
    }

    private fun restoreRestFromSnapshot(value: WearWorkout?) {
        if(value?.automation?.enabled==true){restTimer?.cancel();restRemaining.intValue=if(value.automation.phase=="rest")((value.automation.remaining(android.os.SystemClock.elapsedRealtime())+999)/1000).toInt()else 0;return}
        val endAt = value?.restEndAt ?: 0L
        if (endAt > System.currentTimeMillis()) startRest(max(1, ((endAt - System.currentTimeMillis() + 999L) / 1000L).toInt()), true)
        else { restTimer?.cancel(); restRemaining.intValue = 0 }
    }

    private fun startRest(seconds: Int, vibrateAtEnd: Boolean) {
        restTimer?.cancel()
        if (seconds <= 0) { restRemaining.intValue = 0; return }
        restRemaining.intValue = seconds
        restTimer = object : CountDownTimer(seconds * 1000L, 250L) {
            override fun onTick(millisUntilFinished: Long) {
                restRemaining.intValue = max(0, ((millisUntilFinished + 999L) / 1000L).toInt())
            }
            override fun onFinish() {
                restRemaining.intValue = 0
                workout.value?.let { workout.value = WorkoutSnapshotStore.saveWorkout(this@MainActivity, it.endRest()) }
                if (vibrateAtEnd) getSystemService(Vibrator::class.java)?.vibrate(
                    VibrationEffect.createOneShot(300L, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            }
        }.start()
    }

    private fun skipRest() {
        val active=workout.value
        if(active?.automation?.enabled==true){
            if(active.automation.owner=="watch")workout.value=WearAutomaticController.command(this,"skipRest",active.workoutId)
            else sendCommand("automaticControl",value="skipRest")
            restoreRestFromSnapshot(workout.value);return
        }
        restTimer?.cancel()
        restRemaining.intValue = 0
        workout.value?.let { workout.value = WorkoutSnapshotStore.saveWorkout(this, it.endRest()) }
        sendCommand("skipRest")
    }

    private fun changeCurrentSet(field: String, delta: Double) {
        synchronized(WearAutomaticController) { synchronized(WorkoutSnapshotStore) {
        val current = WorkoutSnapshotStore.load(this) ?: return
        if(current.workoutPaused||WearUndo188.blocked(current))return
        if(current.automation?.enabled==true && current.automation.owner=="watch" && current.automation.phase=="work" && field=="reps")return
        val exerciseIndex = current.currentExercise
        val exercise = current.exercise ?: return
        val setIndex = current.currentSetIndex
        val set = exercise.sets.getOrNull(setIndex) ?: return
        val updated = when (field) {
            "weight" -> set.copy(weight = trimNumber(max(0.0, (set.weight.toDoubleOrNull() ?: 0.0) + delta)))
            "reps" -> {
                val value = max(0, (set.reps.toIntOrNull() ?: 0) + delta.roundToInt())
                set.copy(reps = if (value == 0) "" else value.toString())
            }
            "leftSeconds" -> set.copy(leftSeconds = max(0, set.leftSeconds + delta.roundToInt()))
            "rightSeconds" -> set.copy(rightSeconds = max(0, set.rightSeconds + delta.roundToInt()))
            else -> return
        }
        val normalized = if ((field == "leftSeconds" || field == "rightSeconds") && updated.leftSeconds > 0 && updated.rightSeconds > 0)
            updated.copy(reps = minOf(updated.leftSeconds, updated.rightSeconds).toString()) else updated
        replaceSet(current, exerciseIndex, setIndex, normalized)
        sendCommand(
            action = "updateSet",
            exerciseIndex = exerciseIndex,
            setIndex = setIndex,
            field = field,
            value = when (field) {
                "weight" -> normalized.weight
                "reps" -> normalized.reps.ifBlank { "0" }
                "leftSeconds" -> normalized.leftSeconds
                "rightSeconds" -> normalized.rightSeconds
                else -> 0
            }
        )

        }}
    }

    private fun saveEditorValue(field: String,value: Double,expected: String): Boolean {
        synchronized(WearAutomaticController){synchronized(WorkoutSnapshotStore){
            val current=WorkoutSnapshotStore.load(this) ?: return false
        if(current.workoutPaused||WearUndo188.blocked(current))return false
            if(!value.isFinite()||current.currentSet?.done!=false||WearAutomaticEngine.targetId(current,current.currentExercise,current.currentSetIndex)!=expected||
                current.automation?.let { it.enabled&&it.phase=="work"&&field=="reps" }==true||
                current.automation?.let { it.counting&&!it.paused }==true){Toast.makeText(this,"A sorozat közben változott. Nyisd meg újra.",Toast.LENGTH_SHORT).show();return false}
            val set=current.currentSet ?: return false
            val old=when(field){"weight"->set.weight.toDoubleOrNull() ?: 0.0;"reps"->set.reps.toDoubleOrNull() ?: 0.0;"leftSeconds"->set.leftSeconds.toDouble();"rightSeconds"->set.rightSeconds.toDouble();else->return false}
            changeCurrentSet(field,value-old);return true
        }}
    }
    private fun completeCurrentSet() {
        synchronized(WearAutomaticController) { synchronized(WorkoutSnapshotStore) {
        if (restRemaining.intValue > 0) return
        val current = WorkoutSnapshotStore.load(this) ?: return
        if(current.workoutPaused||WearUndo188.blocked(current))return
        if(current.automation?.awaitingPhone==true)return
        if(current.automation?.enabled==true && current.automation.owner=="watch" && current.automation.counting)return
        val exerciseIndex = current.currentExercise
        val exercise = current.exercise ?: return
        val setIndex = current.currentSetIndex
        val set = exercise.sets.getOrNull(setIndex) ?: return
        if (set.done) return
        val perSide = normalizeUnit(exercise.repUnit) == "mp/oldal"
        val valid = if (perSide) set.leftSeconds > 0 && set.rightSeconds > 0 else (set.reps.toIntOrNull() ?: 0) > 0
        if (!valid) return
        if(current.automation?.enabled==true&&current.automation.owner=="watch"){
            workout.value=WearAutomaticController.record(this,exerciseIndex,setIndex,set.reps,set.weight,set.leftSeconds,set.rightSeconds)
            WearStopwatchStore.clear(this);restoreRestFromSnapshot(workout.value);return
        }
        replaceSet(current, exerciseIndex, setIndex, set.copy(done = true))
        val recordId=UUID.randomUUID().toString()
        workout.value=WorkoutSnapshotStore.saveWorkout(this,WearUndo188.capture(current,workout.value!!,exerciseIndex,setIndex,recordId,System.currentTimeMillis()))
        sendCommand(
            action = "completeSet",commandId=recordId,
            exerciseIndex = exerciseIndex,
            setIndex = setIndex,
            reps = set.reps.ifBlank { "0" },
            weight = set.weight,
            leftSeconds = set.leftSeconds,
            rightSeconds = set.rightSeconds
        )
        val finishedLastExercise = workout.value?.let { it.currentExercise == it.exercises.lastIndex && it.exerciseComplete } == true
        workout.value?.let { workout.value = WorkoutSnapshotStore.saveWorkout(this, it.copy(restEndAt = if (finishedLastExercise) 0L else System.currentTimeMillis() + current.restSeconds * 1000L)) }
        WearStopwatchStore.clear(this)
        if(current.automation?.enabled==true){
            if(current.automation.owner=="watch")workout.value=WearAutomaticController.afterRecord(this)
            else workout.value=WorkoutSnapshotStore.saveWorkout(this,(workout.value ?: return).copy(restEndAt=0,automation=current.automation.copy(phase="ready",paused=true,remainingMs=0,awaitingPhone=true)))
            restoreRestFromSnapshot(workout.value);return
        }
        startRest(if (finishedLastExercise) 0 else current.restSeconds, true)

        }}
    }

    private fun navigateExercise(delta: Int) {
        val current = workout.value ?: return
        val next = (current.currentExercise + delta).coerceIn(0, current.exercises.lastIndex)
        selectExercise(next)
    }

    private fun replaceSet(current: WearWorkout, exerciseIndex: Int, setIndex: Int, updated: WearSet) {
        val exercise = current.exercises[exerciseIndex]
        val sets = exercise.sets.toMutableList()
        sets[setIndex] = updated
        val exercises = current.exercises.toMutableList()
        exercises[exerciseIndex] = exercise.copy(sets = sets)
        workout.value = current.copy(exercises = exercises)
        WorkoutSnapshotStore.saveWorkout(this, workout.value ?: return)
    }

    private fun sendCommand(
        action: String,
        exerciseIndex: Int? = null,
        setIndex: Int? = null,
        field: String? = null,
        value: Any? = null,
        reps: String? = null,
        weight: String? = null,
        leftSeconds: Int? = null,
        rightSeconds: Int? = null,
        commandId: String = UUID.randomUUID().toString()
    ) {
        val current = WorkoutSnapshotStore.load(this) ?: return
        if(WearUndo188.blocked(current))return
        if(current.workoutPaused&&action !in listOf("resumeWorkout","finishWorkout","discardWorkout"))return
        val exercise = exerciseIndex?.let { current.exercises.getOrNull(it) }
        val set = if (exercise != null && setIndex != null) exercise.sets.getOrNull(setIndex) else null
        val command = JSONObject()
            .put("schema", 1)
            .put("commandId", commandId)
            .put("baseRevision", current.revision).put("pauseRevision",current.pauseRevision)
            .put("watchSequence", current.localSequence)
            .put("workoutId", current.workoutId)
            .put("action", action)
            .put("createdAt", System.currentTimeMillis())
        if (nativePhoneExecution(current, action, exercise, value)) {
            command.put("nativeExecutionProtocol", 2).put("generation", current.generation).put("executionRevision", current.executionRevision)
        }
        if (action == "automaticControl" && value == "start") {
            command.put("exerciseId", current.exercise?.id).put("setId", current.currentSet?.setId)
        }
        WearUndo188.marker(current)?.let { command.put("undoDependency188",it.optString("commandId")) }
        if (action == "addQuickExercise") command.put("finalSnapshot",WorkoutSnapshotStore.encode(current))
        if (exerciseIndex != null) command.put("exerciseIndex", exerciseIndex)
        if (exercise != null) command.put("exerciseId", exercise.id)
        if (setIndex != null) command.put("setIndex", setIndex)
        if (set != null) command.put("setNumber", set.number)
        if (set != null) command.put("setId",set.setId)
        current.automation?.let { command.put("automation",it.json()) }
        if (field != null) command.put("field", field)
        if (value != null) command.put("value", value)
        if (reps != null) command.put("reps", reps)
        if (weight != null) command.put("weight", weight)
        if (leftSeconds != null) command.put("leftSeconds", leftSeconds)
        if (rightSeconds != null) command.put("rightSeconds", rightSeconds)
        val sequence = WearCommandOutbox.enqueue(this, command)
        workout.value?.let { workout.value = WorkoutSnapshotStore.saveWorkout(this, it.copy(localSequence = maxOf(it.localSequence,sequence))) }
    }

    private fun selectExercise(index: Int) {
        synchronized(WearAutomaticController) { synchronized(WorkoutSnapshotStore) {
        val current = WorkoutSnapshotStore.load(this) ?: return
        if (current.selectExercise(index) === current) return
        if(current.automation?.enabled==true&&current.automation.owner=="watch")workout.value=WearAutomaticController.command(this,"disable",current.workoutId)
        WearStopwatchStore.pause(this)
        // Order the durable commands: end the old rest first, then select an
        // absolute exercise so a different phone screen cannot offset the move.
        if (restRemaining.intValue > 0 || current.restEndAt > System.currentTimeMillis()) skipRest()
        val latest = workout.value ?: return
        workout.value = WorkoutSnapshotStore.saveWorkout(this, latest.selectExercise(index))
        sendCommand("selectExercise", exerciseIndex = index)

        }}
    }

    private fun refreshSaveSync() {
        closure.value=WearClosureStore.load(this)
        saveSync.value=WearCommandOutbox.state(this,workout.value?.workoutId ?: closure.value?.takeIf { it.status in listOf("pending","error") }?.workoutId)
    }

    private fun saveWorkoutFeedback(pending: WatchWorkoutFeedback, effort: String?) {
        val command = JSONObject().put("schema", 1).put("commandId", UUID.randomUUID().toString())
            .put("action", "saveWorkoutFeedback").put("workoutId", pending.workoutId)
            .put("createdAt", System.currentTimeMillis()).put("feedback", WearWorkoutFeedback.answer(effort))
        if(WearCommandOutbox.enqueue(this, command)==0L) {
            refreshSaveSync();Toast.makeText(this,"A mentés nem sikerült. Próbáld újra a Mentés és szinkron oldalon.",Toast.LENGTH_LONG).show();return
        }
        WearClosureStore.answer(this, pending.workoutId, command.getJSONObject("feedback"))
        closure.value = WearClosureStore.load(this)
        WearWorkoutFeedback.markAnswered(this, pending.workoutId)
    }

    private fun closeWorkout(action: String) {
        synchronized(WearAutomaticController) { synchronized(WorkoutSnapshotStore) {
        val visible=workout.value ?: return
        var current=WorkoutSnapshotStore.load(this)?.takeIf { it.workoutId==visible.workoutId } ?: return
        if(WearUndo188.blocked(current)||WearOwnership193.pending(current))return
        if(current.ownershipProtocol193==1&&current.timerOwner=="watch") {
            current=WearOwnership193.freeze(this,current.workoutId) ?: return
            workout.value=current
        }
        if (action == "finishWorkout" && current.completedSets == 0) return
        if(current.ownershipProtocol193==1)try {
            if(action=="discardWorkout")WearGps193.discard(this,current)
            else { WearGps193.arm(this,current.workoutId,false);WearGpsRouteService193.freeze(this) }
        }catch(_:Exception){Toast.makeText(this,WearGpsText193.text("storage"),Toast.LENGTH_LONG).show();return}
        val command = JSONObject()
            .put("schema", 1).put("commandId", UUID.randomUUID().toString())
            .put("workoutId", current.workoutId).put("baseRevision", current.revision)
            .put("watchSequence", current.localSequence)
            .put("action", action).put("confirmed", true)
            .put("finishedAt", Instant.now().toString()).put("createdAt", System.currentTimeMillis())
            .put("finalSnapshot", WorkoutSnapshotStore.encode(current))
        WearUndo188.marker(current)?.let { command.put("undoDependency188",it.optString("commandId")) }
        WearHealthStore.load(this,current.workoutId)?.let { command.put("healthSummary",WearHealthAccumulator(it).snapshot()) }
        WearHealthService.stop(this)
        WearAutomaticService.stop(this)
        closure.value = WearClosureStore.close(this, current, command)
        if(closure.value?.status=="error" || WearCommandOutbox.enqueue(this, command)==0L) {
            refreshSaveSync();Toast.makeText(this,"A mentés nem sikerült. Az edzés adatai megmaradtak az órán.",Toast.LENGTH_LONG).show();return
        }
        WorkoutSnapshotStore.clear(this)
        WearStopwatchStore.clear(this)
        workout.value = null
        refreshHealth()
        restTimer?.cancel()
        restRemaining.intValue = 0
        WearSurfaces.refresh(this,true)
        }}
    }

    private fun retryClosure() {
        closure.value?.let { local ->
            val status=WearCommandOutbox.state(this,local.workoutId)
            if(status?.retryable==true) { WearCommandOutbox.retry(this,local.workoutId);refreshSaveSync();return }
        }
        val command = WearClosureStore.retry(this) ?: return
        closure.value = WearClosureStore.load(this)
        WearCommandOutbox.enqueue(this, command)
    }

    private fun trimNumber(value: Double): String {
        val rounded = (value * 10.0).roundToInt() / 10.0
        return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
    }
}
