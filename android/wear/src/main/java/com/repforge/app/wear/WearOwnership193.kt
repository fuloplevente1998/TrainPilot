package com.repforge.app.wear

import android.content.Context
import android.os.SystemClock
import android.widget.Toast
import com.google.android.gms.wearable.Wearable
import com.repforge.shared.OwnershipJournal193
import com.repforge.shared.OwnershipTransport193
import com.repforge.shared.WorkoutOwnership193
import org.json.JSONObject

internal object WearOwnership193 {
    private var cached: OwnershipJournal193?=null
    private var cachedContext: Context?=null
    @Synchronized fun journal(context: Context): OwnershipJournal193 {
        val app=context.applicationContext
        if(cachedContext!==app) {
            cachedContext=app
            cached=OwnershipJournal193(app,object: OwnershipJournal193.Host {
                override fun current(): JSONObject?=WorkoutSnapshotStore.load(app)?.let(WorkoutSnapshotStore::encode)
                override fun install(value: JSONObject) {
                    var next=WorkoutSnapshotStore.parse(value.toString()) ?: error("Invalid transferred workout")
                    val current=WorkoutSnapshotStore.load(app)
                    if(next.timerOwner=="watch"&&WorkoutOwnership193.transfer(value)?.optString("state")=="committed"
                        &&(current==null||current.ownerEpoch193<next.ownerEpoch193||pending(current))) {
                        val a=next.automation
                        if(a!=null&&a.owner=="watch") {
                            val target=next.exercises.getOrNull(a.targetExercise)?.sets?.indexOfFirst { it.setId==a.targetId }
                                ?.takeIf { it>=0 } ?: next.exercises.getOrNull(a.targetExercise)?.sets?.indexOfFirst { !it.done } ?: -1
                            next=next.copy(automation=a.copy(key="ownership193:${WorkoutOwnership193.transfer(value)!!.optString("id")}",
                                targetSet=target.coerceAtLeast(0),targetId=next.exercises.getOrNull(a.targetExercise)?.sets?.getOrNull(target)?.setId.orEmpty(),
                                anchorMs=SystemClock.elapsedRealtime(),boot=WearAutomaticService.boot(app),paused=true,deadlineAt=0))
                        }
                    }
                    if(current!=null&&current.ownerEpoch193>next.ownerEpoch193)error("Stale transferred workout")
                    WorkoutSnapshotStore.saveWorkout(app,next)
                    if(next.timerOwner!="watch"||next.workoutPaused)WearAutomaticService.stop(app)
                    WearHealthService.syncPause(app);WearSurfaces.refresh(app,true)
                }
            })
        }
        return cached!!
    }
    fun pending(workout: WearWorkout)=workout.handoff193?.let { JSONObject(it).optString("state")!="committed" }==true
    fun request(context: Context) {
        val current=WorkoutSnapshotStore.load(context) ?: return
        if(current.timerOwner!="phone"||current.ownershipProtocol193!=1)return
        if((WearCommandOutbox.state(context,current.workoutId)?.pending ?: 0)>0) {
            Toast.makeText(context,"Várd meg az adatok telefonos visszaigazolását, majd próbáld újra.",Toast.LENGTH_LONG).show();return
        }
        Wearable.getNodeClient(context).localNode.addOnSuccessListener { local ->
            Wearable.getNodeClient(context).connectedNodes.addOnSuccessListener peersReady@{ peers ->
                if(peers.size!=1){Toast.makeText(context,"Az átvételhez csatlakoztasd a telefont.",Toast.LENGTH_LONG).show();return@peersReady}
                try {
                    journal(context).request(local.id,peers.single().id,System.currentTimeMillis());recover(context)
                    Toast.makeText(context,"Átvétel kérése elküldve. Tartsd megnyitva a telefonos edzést.",Toast.LENGTH_LONG).show()
                }catch(_:Exception){Toast.makeText(context,"Az átvétel nem menthető. Próbáld újra.",Toast.LENGTH_LONG).show()}
            }
        }
    }
    fun recover(context: Context) {
        if(android.os.Looper.myLooper()!=android.os.Looper.getMainLooper()) {
            android.os.Handler(android.os.Looper.getMainLooper()).post { recover(context.applicationContext) };return
        }
        try { val store=journal(context);store.recover();OwnershipTransport193.send(context,store) }catch(_:Exception){/* Never start from an unreadable journal. */}
    }
    fun receive(context: Context,message: JSONObject,origin: String,localNode: String) {
        synchronized(WearAutomaticController) { synchronized(WorkoutSnapshotStore) {
            val store=journal(context)
            store.recover();val state=store.receive(message,origin,localNode,System.currentTimeMillis())
            if(state.optString("stage")=="incoming") {
                var current=WorkoutSnapshotStore.load(context) ?: return
                if(current.timerOwner!="watch")return
                val beforeAuto=current.automation
                val resume=current.resumeAutomaticOnContinue||!current.workoutPaused&&beforeAuto?.enabled==true&&!beforeAuto.paused
                if(!current.workoutPaused&&current.automation?.enabled==true)
                    current=WearAutomaticController.command(context,"pause",current.workoutId) ?: current
                current=WearStopwatchStore.freezeIntoWorkout193(context,current)
                current=WorkoutSnapshotStore.saveWorkout(context,current.copy(workoutPaused=true,resumeAutomaticOnContinue=resume,
                    pauseRevision=maxOf(current.pauseRevision+1,System.currentTimeMillis()),
                    pausedRestMillis=maxOf(current.pausedRestMillis,current.restEndAt-System.currentTimeMillis()),restEndAt=0,
                    activeIntervals=current.activeIntervals?.map { if(it.end==0L)it.copy(end=System.currentTimeMillis())else it }))
                WearAutomaticService.stop(context);WearHealthService.syncPause(context)
                store.prepare(WorkoutSnapshotStore.encode(current),localNode,System.currentTimeMillis())
            }
            OwnershipTransport193.send(context,store);WearSurfaces.refresh(context,true)
        }}
    }
    fun resume(context: Context): WearWorkout? {
        synchronized(WearAutomaticController) { synchronized(WorkoutSnapshotStore) {
            var current=WorkoutSnapshotStore.load(context) ?: return null
            if(current.timerOwner!="watch"||pending(current)||!current.workoutPaused)return current
            val now=System.currentTimeMillis()
            current=current.copy(workoutPaused=false,pauseRevision=maxOf(now,current.pauseRevision+1),
                activeIntervals=(current.activeIntervals ?: emptyList())+WearActiveInterval(now),
                restEndAt=if(current.pausedRestMillis>0)now+current.pausedRestMillis else 0,pausedRestMillis=0)
            if(current.resumeAutomaticOnContinue&&current.automation?.enabled==true)
                current=WearAutomaticEngine.resume(current,SystemClock.elapsedRealtime(),WearAutomaticService.boot(context))
            current=WorkoutSnapshotStore.saveWorkout(context,current.copy(resumeAutomaticOnContinue=false))
            val command=JSONObject().put("schema",1).put("commandId",java.util.UUID.randomUUID().toString())
                .put("action","resumeWorkout").put("ownerEpoch193",current.ownerEpoch193).put("workoutId",current.workoutId)
                .put("pauseRevision",current.pauseRevision).put("createdAt",now).put("finalSnapshot",WorkoutSnapshotStore.encode(current))
            val sequence=WearCommandOutbox.enqueue(context,command)
            current=WorkoutSnapshotStore.saveWorkout(context,current.copy(localSequence=maxOf(sequence,current.localSequence)))
            if(current.automation?.enabled==true)WearAutomaticService.ensure(context,current)
            WearHealthService.syncPause(context);WearSurfaces.refresh(context,true);return current
        }}
    }
}
