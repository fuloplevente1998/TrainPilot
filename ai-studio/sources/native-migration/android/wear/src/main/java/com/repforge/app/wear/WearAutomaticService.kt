package com.repforge.app.wear

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.os.*
import android.provider.Settings
import org.json.JSONObject
import java.util.UUID

/** Workout-owned timer, independent of Compose, Activity and health permissions. */
class WearAutomaticService : Service() {
    private val handler=Handler(Looper.getMainLooper())
    private var id=""
    private var key=""
    private var lastSavedSecond=-1L
    private var haptics=WearAutomaticHaptics()
    private var wake: PowerManager.WakeLock?=null
    private var shuttingDown=false
    private var initialized=false
    private var lastMirrorAt=0L
    private val tick=object : Runnable {
        override fun run() {
            synchronized(WearAutomaticController) { synchronized(WorkoutSnapshotStore) {
            val current=WorkoutSnapshotStore.load(this@WearAutomaticService)
            if(current==null||current.workoutId!=id||current.automation?.enabled!=true||current.automation.owner!="watch"){finish();return}
            if(current.automation.key!=key){key=current.automation.key;haptics=WearAutomaticHaptics()}
            val before=current.automation
            val transition=WearAutomaticEngine.tick(current,SystemClock.elapsedRealtime(),boot(this@WearAutomaticService))
            val next=transition.workout;val a=next.automation ?: return
            val boundary=a.phaseNumber!=before.phaseNumber||a.paused!=before.paused||a.interrupted!=before.interrupted
            val second=a.remaining(SystemClock.elapsedRealtime())/1000
            if(boundary||second!=lastSavedSecond||next.exercises!=current.exercises){WorkoutSnapshotStore.saveWorkout(this@WearAutomaticService,next);lastSavedSecond=second}
            transition.completedExercise?.let { e ->
                val s=transition.completedSet ?: return@let
                WearAutomaticController.publishCompleted(this@WearAutomaticService,next,e,s,current)
            }
            if(boundary||SystemClock.elapsedRealtime()-lastMirrorAt>=5000){lastMirrorAt=SystemClock.elapsedRealtime();WearAutomaticController.publish(this@WearAutomaticService,WorkoutSnapshotStore.load(this@WearAutomaticService) ?: next)}
            val duration=haptics.next(a,SystemClock.elapsedRealtime())
            if(a.vibration&&duration>0)getSystemService(Vibrator::class.java)?.vibrate(VibrationEffect.createOneShot(duration,VibrationEffect.DEFAULT_AMPLITUDE))
            if(a.counting&&!a.paused){if(wake?.isHeld!=true)wake?.acquire(86400000L)}else release()
            if(boundary||second!=lastNotified){lastNotified=second;getSystemService(NotificationManager::class.java).notify(162,notification(next))}
            handler.postDelayed(this,if(a.counting&&!a.paused)200 else 1000)

            }}
        }
    }
    private var lastNotified=-1L
    override fun onBind(intent: Intent?) = null
    override fun onCreate() {
        super.onCreate();instance=this
        wake=getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"TrainPilot:wear-automatic")
        getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("workout-automatic","Automata edzés",NotificationManager.IMPORTANCE_LOW))
    }
    override fun onStartCommand(intent: Intent?,flags: Int,startId: Int): Int {
        val current=WorkoutSnapshotStore.load(this)
        if(intent!=null&&current!=null&&current.workoutId!=intent.getStringExtra("workoutId"))return START_NOT_STICKY
        if(intent==null||current==null||current.automation?.enabled!=true||current.automation.owner!="watch"){finish();return START_NOT_STICKY}
        if(!initialized&&!intent.getBooleanExtra("fresh",false)&&(current.automation.counting||current.automation.phase=="ready"&&!WearAutomaticEngine.timed(current.exercise))&&!current.automation.paused)
            WorkoutSnapshotStore.saveWorkout(this,WearAutomaticEngine.interrupt(current,current.automation.anchorMs))
        initialized=true;shuttingDown=false
        id=current.workoutId
        val notification=notification(current)
        if(Build.VERSION.SDK_INT>=34)startForeground(162,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)else startForeground(162,notification)
        intent.getStringExtra("action")?.let { WearAutomaticController.command(this,it,id) }
        handler.removeCallbacks(tick);handler.post(tick)
        return START_NOT_STICKY
    }
    private fun notification(workout: WearWorkout): Notification {
        val a=workout.automation!!
        val phase=when { a.interrupted->"Megszakadt";a.paused->"Szünetel";a.phase=="rest"->"Pihenő";a.phase=="work"->"Munka";a.phase=="complete"->"Kész";else->"Sorozatra vár" }
        val open=PendingIntent.getActivity(this,162,Intent(this,MainActivity::class.java).putExtra("destination","workout"),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        fun action(value: String)=PendingIntent.getService(this,value.hashCode(),Intent(this,WearAutomaticService::class.java).putExtra("workoutId",workout.workoutId).putExtra("action",value),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this,"workout-automatic").setSmallIcon(R.drawable.ic_workout_recording)
            .setContentTitle("TrainPilot · $phase").setContentText("${(a.remaining(SystemClock.elapsedRealtime())+999)/1000} s · ${workout.exercise?.name.orEmpty()}")
            .setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true)
            .addAction(Notification.Action.Builder(android.R.drawable.ic_media_pause,if(a.paused)"Folytatás" else "Szünet",action(if(a.paused)"resume" else "pause")).build())
            .addAction(Notification.Action.Builder(android.R.drawable.ic_delete,"Automata kikapcsolása",action("disable")).build()).build()
    }
    private fun release(){if(wake?.isHeld==true)wake?.release()}
    private fun finish(){shuttingDown=true;handler.removeCallbacks(tick);release();stopForeground(STOP_FOREGROUND_REMOVE);stopSelf()}
    override fun onDestroy() {
        handler.removeCallbacks(tick);release();getSystemService(Vibrator::class.java)?.cancel()
        if(!shuttingDown)WorkoutSnapshotStore.load(this)?.takeIf { it.workoutId==id && it.automation?.enabled==true }?.let {
            val paused=WearAutomaticEngine.interrupt(it,SystemClock.elapsedRealtime());WorkoutSnapshotStore.saveWorkout(this,paused);WearAutomaticController.publish(this,paused)
        }
        if(instance===this)instance=null;super.onDestroy()
    }
    companion object {
        private var instance: WearAutomaticService?=null
        fun boot(context: Context)=Settings.Global.getInt(context.contentResolver,Settings.Global.BOOT_COUNT,-1)
        fun ensure(context: Context,workout: WearWorkout) {
            if(workout.automation?.enabled!=true||workout.automation.owner!="watch")return
            context.startForegroundService(Intent(context,WearAutomaticService::class.java).putExtra("workoutId",workout.workoutId).putExtra("fresh",true))
        }
        fun active(workoutId: String)=instance?.id==workoutId
        fun stop(context: Context){instance?.shuttingDown=true;context.stopService(Intent(context,WearAutomaticService::class.java))}
        fun nudge(){instance?.let { it.handler.removeCallbacks(it.tick);it.handler.post(it.tick) }}
    }
}

object WearAutomaticController {
    private fun clock()=SystemClock.elapsedRealtime()
    @Synchronized fun restore(context: Context): WearWorkout? {
        synchronized(WorkoutSnapshotStore) {
        val current=WorkoutSnapshotStore.load(context) ?: return null
        if(current.automation?.enabled!=true||current.automation.owner!="watch")return current
        if(!WearAutomaticService.active(current.workoutId)&&current.automation.counting&&!current.automation.paused){
            val paused=WearAutomaticEngine.interrupt(current,current.automation.anchorMs);WorkoutSnapshotStore.saveWorkout(context,paused);publish(context,paused);return paused
        }
        return current

        }
    }
    @Synchronized fun toggle(context: Context,enabled: Boolean): WearWorkout? {
        synchronized(WorkoutSnapshotStore) {
        val current=restore(context) ?: return null
        if(current.workoutPaused||WearUndo188.blocked(current))return current
        if(enabled&&WatchHomeStore.automaticProtocol(context)!=1){android.widget.Toast.makeText(context,"Frissítsd a telefonos appot, majd frissítsd az órás adatokat.",android.widget.Toast.LENGTH_LONG).show();return current}
        if(current.timerOwner!="watch")return current
        if(current.automation?.owner=="phone" && current.automation.enabled)return current
        if(enabled&&!WearAutomaticEngine.durationAllowed(current,current.automation?.workSeconds ?: 30,
            current.automation?.restSeconds ?: current.restSeconds,current.automation?.rounds ?: 1)){
            android.widget.Toast.makeText(context,"Az automata menetrend legfeljebb 24 órás lehet.",android.widget.Toast.LENGTH_LONG).show();return current
        }
        var next=if(enabled)WearAutomaticEngine.enable(current,UUID.randomUUID().toString(),
            current.automation?.workSeconds ?: 30,current.automation?.restSeconds ?: current.restSeconds,
            current.automation?.vibration ?: true,clock(),WearAutomaticService.boot(context))
        else WearAutomaticEngine.disable(current,clock(),WearAutomaticService.boot(context))
        if(enabled)next=next.copy(automation=next.automation!!.copy(rounds=current.automation?.rounds ?: 1))
        WearStopwatchStore.clear(context)
        WorkoutSnapshotStore.saveWorkout(context,next);publish(context,next)
        if(enabled)WearAutomaticService.ensure(context,next)else WearAutomaticService.stop(context)
        return WorkoutSnapshotStore.load(context)

        }
    }
    @Synchronized fun command(context: Context,action: String,expectedId: String): WearWorkout? {
        synchronized(WorkoutSnapshotStore) {
        val current=WorkoutSnapshotStore.load(context) ?: return null
        if(current.workoutPaused||WearUndo188.blocked(current))return current
        if(current.workoutId!=expectedId||current.automation?.enabled!=true||current.automation.owner!="watch")return current
        val now=clock();val boot=WearAutomaticService.boot(context)
        val transition=WearAutomaticEngine.tick(current,now,boot)
        val updated=transition.workout
        transition.completedExercise?.let { publishCompleted(context,updated,it,transition.completedSet!!,current) }
        val next=when(action){"start"->if(updated.automation!!.phase=="ready")WearAutomaticEngine.ready(updated,now,boot)else updated;"pause"->WearAutomaticEngine.pause(updated,now,boot)
            "resume"->WearAutomaticEngine.resume(updated,now,boot);"skipRest"->WearAutomaticEngine.skipRest(updated,now,boot)
            "disable"->WearAutomaticEngine.disable(updated,now,boot)
            "workPlus","workMinus","restPlus","restMinus","roundsPlus","roundsMinus"->if(updated.automation!!.counting)updated else {
                val a=updated.automation!!
                updated.copy(automation=if(action.startsWith("work"))a.copy(workSeconds=(a.workSeconds+if(action.endsWith("Plus"))5 else -5).coerceIn(1,3600))
                    else if(action.startsWith("rounds"))a.copy(rounds=(a.rounds+if(action.endsWith("Plus"))1 else -1).coerceIn(1,100))
                    else a.copy(restSeconds=(a.restSeconds+if(action.endsWith("Plus"))5 else -5).coerceIn(0,3600)))
            }
            "vibration"->updated.copy(automation=updated.automation!!.copy(vibration=!updated.automation.vibration))
            else->return current}
        val configured=next.automation!!
        if(!WearAutomaticEngine.durationAllowed(next,configured.workSeconds,configured.restSeconds,configured.rounds))return current
        WorkoutSnapshotStore.saveWorkout(context,next.copy(lastRecord188=WorkoutSnapshotStore.load(context)?.lastRecord188));publish(context,WorkoutSnapshotStore.load(context) ?: next)
        if(next.automation?.paused==true||!next.automation!!.enabled)context.getSystemService(Vibrator::class.java)?.cancel()
        if(!next.automation.enabled)WearAutomaticService.stop(context)else WearAutomaticService.nudge()
        return WorkoutSnapshotStore.load(context)

        }
    }
    @Synchronized fun afterRecord(context: Context): WearWorkout? {
        synchronized(WorkoutSnapshotStore) {
        val current=WorkoutSnapshotStore.load(context) ?: return null
        val next=WearAutomaticEngine.afterRecord(current,clock(),WearAutomaticService.boot(context))
        WorkoutSnapshotStore.saveWorkout(context,next);publish(context,next);WearAutomaticService.nudge();return WorkoutSnapshotStore.load(context)

        }
    }
    @Synchronized fun record(context: Context,e: Int,s: Int,reps: String,weight: String,left: Int=0,right: Int=0,commandId: String=""): WearWorkout? {
        synchronized(WorkoutSnapshotStore) {
        val current=WorkoutSnapshotStore.load(context) ?: return null
        val a=current.automation ?: return current
        if(current.workoutPaused||WearUndo188.blocked(current))return current
        if(!a.enabled||a.owner!="watch"||a.counting||a.paused||commandId in a.handledCommands)return current
        val exercise=current.exercises.getOrNull(e) ?: return current
        val set=exercise.sets.getOrNull(s) ?: return current
        if(set.done||e!=current.currentExercise)return current
        val valid=if(exercise.repUnit=="mp/oldal")left>0&&right>0 else (reps.toIntOrNull() ?: 0)>0
        if(!valid)return current
        val completed=set.copy(reps=reps,weight=weight,done=true,leftSeconds=left,rightSeconds=right)
        val updated=current.copy(exercises=current.exercises.mapIndexed { index,ex -> if(index!=e)ex else ex.copy(sets=ex.sets.mapIndexed { si,old->if(si==s)completed else old }) })
        var next=WearAutomaticEngine.afterRecord(updated,clock(),WearAutomaticService.boot(context))
        if(commandId.isNotBlank())next=next.copy(automation=next.automation!!.copy(handledCommands=(a.handledCommands+commandId).takeLast(128)))
        next=WorkoutSnapshotStore.saveWorkout(context,next);publishCompleted(context,next,e,s,current)
        publish(context,WorkoutSnapshotStore.load(context) ?: next);WearAutomaticService.nudge()
        return WorkoutSnapshotStore.load(context)

        }
    }
    @Synchronized fun remote(context: Context,input: JSONObject) {
        synchronized(WorkoutSnapshotStore) {
        val current=restore(context) ?: return
        if(input.optLong("ownerEpoch193")!=current.ownerEpoch193)return
        val id=input.optString("commandId");val action=input.optString("action")
        if(current.workoutPaused||WearUndo188.blocked(current))return
        if(System.currentTimeMillis()-input.optLong("createdAt") !in 0..15000)return
        if(id.isBlank()||id.length>128||current.workoutId!=input.optString("workoutId")||current.timerOwner!="watch")return
        val a=current.automation
        if(a?.handledCommands?.contains(id)==true){publish(context,current);return}
        if(input.optString("key")!=(a?.key ?: "")){if(a!=null)publish(context,current);return}
        if(a!=null&&action!="toggle"&&input.optLong("phaseNumber",-1)!=a.phaseNumber){publish(context,current);return}
        if(action=="undo"){
            val recordId=input.optString("recordId")
            if(recordId.isBlank())return
            val next=WearUndo188.undo(context,recordId) ?: return
            if(WearUndo188.marker(next)?.optString("recordId")!=recordId){publish(context,next);return}
            val state=next.automation ?: WearAutomatic(enabled=false,owner="watch",key="undo188:$id",paused=true)
            val saved=WorkoutSnapshotStore.saveWorkout(context,next.copy(automation=state.copy(handledCommands=(state.handledCommands+id).takeLast(128))))
            publish(context,saved);return
        }
        if(action in listOf("start","resume","record")&&!WearAutomaticService.active(current.workoutId)){publish(context,current);return}
        if(action=="record"){
            val e=input.optInt("exerciseIndex",-1);val s=input.optInt("setIndex",-1)
            val set=current.exercises.getOrNull(e)?.sets?.getOrNull(s) ?: return
            if(set.setId!=input.optString("setId"))return
            record(context,e,s,input.optString("reps"),input.optString("weight"),input.optInt("leftSeconds"),input.optInt("rightSeconds"),id);return
        }
        if(action=="selectExercise"){
            val index=input.optInt("exerciseIndex",-1)
            if(index !in current.exercises.indices)return
            var selected=WearAutomaticEngine.disable(current,clock(),WearAutomaticService.boot(context)).selectExercise(index)
            selected=selected.copy(automation=selected.automation?.copy(handledCommands=((a?.handledCommands ?: emptyList())+id).takeLast(128)))
            selected=WorkoutSnapshotStore.saveWorkout(context,selected);publish(context,selected);WearAutomaticService.stop(context);return
        }
        var next=if(action=="toggle") {
            if(!WearAutomaticService.active(current.workoutId)&&input.optBoolean("enabled"))return
            toggle(context,input.optBoolean("enabled"))
        } else command(context,action,current.workoutId)
        next ?: return
        next.automation?.let { state -> next=WorkoutSnapshotStore.saveWorkout(context,next!!.copy(automation=state.copy(handledCommands=(state.handledCommands+id).takeLast(128)))) }
        publish(context,next!!)

        }
    }
    @Synchronized fun publish(context: Context,workout: WearWorkout) {
        synchronized(WorkoutSnapshotStore) {
        val a=workout.automation ?: return
        if(a.owner!="watch")return
        val snapshot=WorkoutSnapshotStore.encode(workout)
        snapshot.optJSONObject("undo188")?.remove("command")
        val remaining=a.remaining(clock())
        snapshot.put("automation",a.json().put("remainingMs",remaining).put("anchorMs",clock())
            .put("deadlineAt",if(a.counting&&!a.paused)System.currentTimeMillis()+remaining else 0))
        val command=JSONObject().put("schema",1).put("commandId",UUID.randomUUID().toString())
            .put("action","watchAutomationState").put("workoutId",workout.workoutId).put("createdAt",System.currentTimeMillis())
            .put("watchSequence",workout.localSequence).put("finalSnapshot",snapshot)
        WearUndo188.marker(workout)?.let { command.put("undoDependency188",it.optString("commandId")) }
        val sequence=WearCommandOutbox.enqueue(context,command)
        WorkoutSnapshotStore.saveWorkout(context,(WorkoutSnapshotStore.load(context) ?: workout).copy(localSequence=maxOf(workout.localSequence,sequence)))

        }
    }
    @Synchronized fun publishCompleted(context: Context,workout: WearWorkout,e: Int,s: Int,before: WearWorkout) {
        synchronized(WorkoutSnapshotStore) {
        val set=workout.exercises[e].sets[s]
        val recordId=UUID.randomUUID().toString()
        val captured=WearUndo188.capture(before,workout,e,s,recordId,System.currentTimeMillis())
        WorkoutSnapshotStore.saveWorkout(context,captured)
        val command=JSONObject().put("schema",1).put("commandId",recordId)
            .put("action","completeSet").put("workoutId",workout.workoutId).put("createdAt",System.currentTimeMillis())
            .put("watchSequence",workout.localSequence).put("exerciseIndex",e).put("exerciseId",workout.exercises[e].id)
            .put("setIndex",s).put("setNumber",set.number).put("setId",set.setId).put("reps",set.reps).put("weight",set.weight)
            .put("automation",workout.automation?.json())
        WearUndo188.marker(workout)?.let { command.put("undoDependency188",it.optString("commandId")) }
        val sequence=WearCommandOutbox.enqueue(context,command)
        WorkoutSnapshotStore.saveWorkout(context,(WorkoutSnapshotStore.load(context) ?: workout).copy(localSequence=maxOf(workout.localSequence,sequence)))

        }
    }
}
