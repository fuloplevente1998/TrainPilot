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
                    if(WearClosureStore.isClosed(app,value.optString("workoutId")))error("Workout was closed")
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
                        if(next.timerCheckpoint193!=null)next=WearTransferredTimer193.install(next,SystemClock.elapsedRealtime(),WearAutomaticService.boot(app),
                            "ownership193:${WorkoutOwnership193.transfer(value)!!.optString("id")}")
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
            Toast.makeText(context,WearOwnershipText193.text("pendingCommands"),Toast.LENGTH_LONG).show();return
        }
        Wearable.getNodeClient(context).localNode.addOnSuccessListener { local ->
            Wearable.getNodeClient(context).connectedNodes.addOnSuccessListener peersReady@{ peers ->
                if(peers.size!=1){Toast.makeText(context,WearOwnershipText193.text("connected"),Toast.LENGTH_LONG).show();return@peersReady}
                try {
                    journal(context).request(local.id,peers.single().id,System.currentTimeMillis());recover(context)
                    Toast.makeText(context,WearOwnershipText193.text("requestSent"),Toast.LENGTH_LONG).show()
                }catch(_:Exception){Toast.makeText(context,WearOwnershipText193.text("failed"),Toast.LENGTH_LONG).show()}
            }.addOnFailureListener { Toast.makeText(context,WearOwnershipText193.text("connected"),Toast.LENGTH_LONG).show() }
        }.addOnFailureListener { Toast.makeText(context,WearOwnershipText193.text("connected"),Toast.LENGTH_LONG).show() }
    }
    fun recover(context: Context) {
        if(android.os.Looper.myLooper()!=android.os.Looper.getMainLooper()) {
            android.os.Handler(android.os.Looper.getMainLooper()).post { recover(context.applicationContext) };return
        }
        try {
            val store=journal(context);val state=store.read()
            val id=state.optJSONObject("snapshot")?.optString("workoutId") ?: state.optJSONObject("request")?.optString("workoutId")
            if(id!=null&&WearClosureStore.isClosed(context,id))store.close(id)
            store.recover();OwnershipTransport193.send(context,store);retryResume(context)
        }catch(_:Exception){/* Never start from an unreadable journal. */}
    }
    fun receive(context: Context,message: JSONObject,origin: String,localNode: String) {
        synchronized(WearAutomaticController) { synchronized(WorkoutSnapshotStore) {
            val store=journal(context)
            store.recover();val state=store.receive(message,origin,localNode,System.currentTimeMillis())
            if(state.optString("stage")=="incoming") {
                var current=WorkoutSnapshotStore.load(context) ?: return
                if(current.timerOwner!="watch"||current.ownershipProtocol193!=1)return
                current=freeze(context,current.workoutId) ?: return
                store.prepare(WorkoutSnapshotStore.encode(current),localNode,System.currentTimeMillis())
            }
            OwnershipTransport193.send(context,store);WearSurfaces.refresh(context,true)
        }}
    }
    /** Freeze the durable source, including the last partial seconds, before handing off or closing. */
    fun freeze(context: Context,expectedId: String): WearWorkout? {
        synchronized(WearAutomaticController) { synchronized(WorkoutSnapshotStore) {
            var current=WorkoutSnapshotStore.load(context) ?: return null
            if(current.workoutId!=expectedId||current.ownershipProtocol193!=1||current.timerOwner!="watch"||pending(current)||WearUndo188.blocked(current))return null
            val beforeAuto=current.automation
            val resume=current.resumeAutomaticOnContinue||!current.workoutPaused&&beforeAuto?.enabled==true&&!beforeAuto.paused
            if(!current.workoutPaused&&beforeAuto?.enabled==true)
                current=WearAutomaticController.command(context,"pause",current.workoutId) ?: return null
            if(beforeAuto?.enabled==true)WearStopwatchStore.pause(context)
            else current=WearStopwatchStore.freezeIntoWorkout193(context,current)
            val now=System.currentTimeMillis()
            current=WorkoutSnapshotStore.saveWorkout(context,current.copy(workoutPaused=true,resumeAutomaticOnContinue=resume,
                pauseRevision=if(current.workoutPaused)current.pauseRevision else maxOf(current.pauseRevision+1,now),
                pausedRestMillis=maxOf(current.pausedRestMillis,(current.restEndAt-now).coerceAtLeast(0)),restEndAt=0,
                activeIntervals=current.activeIntervals?.map { if(it.end==0L)it.copy(end=maxOf(it.start,now))else it }))
            WearAutomaticService.stop(context);WearHealthService.syncPause(context)
            return current
        }}
    }
    fun resume(context: Context): WearWorkout? {
        synchronized(WearAutomaticController) { synchronized(WorkoutSnapshotStore) {
            var current=WorkoutSnapshotStore.load(context) ?: return null
            if(current.timerOwner!="watch"||pending(current))return current
            if(!current.workoutPaused){retryResume(context);return WorkoutSnapshotStore.load(context)}
            val now=System.currentTimeMillis()
            current=current.copy(workoutPaused=false,pauseRevision=maxOf(now,current.pauseRevision+1),
                activeIntervals=(current.activeIntervals ?: emptyList())+WearActiveInterval(now),
                restEndAt=if(current.pausedRestMillis>0)now+current.pausedRestMillis else 0,pausedRestMillis=0)
            if(current.resumeAutomaticOnContinue&&current.automation?.enabled==true)
                current=WearAutomaticEngine.resume(current,SystemClock.elapsedRealtime(),WearAutomaticService.boot(context))
            val command=JSONObject().put("schema",1).put("commandId",java.util.UUID.randomUUID().toString())
                .put("action","resumeWorkout").put("ownerEpoch193",current.ownerEpoch193).put("workoutId",current.workoutId)
                .put("pauseRevision",current.pauseRevision).put("createdAt",now)
            current=WorkoutSnapshotStore.saveWorkout(context,current.copy(resumeAutomaticOnContinue=false,resumeIntent193=command.toString()))
            retryResume(context);current=WorkoutSnapshotStore.load(context) ?: current
            if(current.automation?.enabled==true)WearAutomaticService.ensure(context,current)
            WearHealthService.syncPause(context);WearSurfaces.refresh(context,true);return current
        }}
    }
    private fun retryResume(context: Context) {
        synchronized(WearAutomaticController) { synchronized(WorkoutSnapshotStore) {
            val current=WorkoutSnapshotStore.load(context) ?: return
            val command=current.resumeIntent193?.let(::JSONObject) ?: return
            if(current.ownershipProtocol193!=1||current.timerOwner!="watch"||pending(current)||current.workoutPaused
                ||command.optLong("ownerEpoch193")!=current.ownerEpoch193||command.optLong("pauseRevision")!=current.pauseRevision)return
            command.put("finalSnapshot",WorkoutSnapshotStore.encode(current.copy(resumeIntent193=null)))
            val sequence=WearCommandOutbox.enqueue(context,command)
            if(sequence>0)WorkoutSnapshotStore.saveWorkout(context,current.copy(localSequence=maxOf(sequence,current.localSequence),resumeIntent193=null))
        }}
    }
}
