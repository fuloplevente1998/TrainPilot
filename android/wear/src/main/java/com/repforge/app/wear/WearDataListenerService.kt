package com.repforge.app.wear

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import com.google.android.gms.wearable.MessageEvent

class WearDataListenerService : WearableListenerService() {
    override fun onPeerConnected(peer: com.google.android.gms.wearable.Node) {
        WearCommandOutbox.flush(this,true)
    }
    override fun onMessageReceived(event: MessageEvent) {
        if(event.path!=WearWorkoutHandoff.REPLY || event.data.size>100000)return
        if(WearWorkoutHandoff.accept(this,event.data.toString(Charsets.UTF_8)))WearSurfaces.refresh(this)
    }
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type != DataEvent.TYPE_CHANGED) continue
            val item = event.dataItem
            if(item.uri.path?.startsWith(WearCommandOutbox.RECEIPT_PREFIX)==true) {
                if(WearCommandOutbox.acceptReceipt(this,item.uri.path,DataMapItem.fromDataItem(item).dataMap.getString("receipt")))
                    com.google.android.gms.wearable.Wearable.getDataClient(this).deleteDataItems(item.uri)
                continue
            }
            if(item.uri.path?.startsWith("/trainpilot/automatic-command/")==true){
                val command=DataMapItem.fromDataItem(item).dataMap.getString("command")
                if(command!=null&&command.length<16000)try { WearAutomaticController.remote(this,org.json.JSONObject(command)) }catch(_:Exception){}
                com.google.android.gms.wearable.Wearable.getDataClient(this).deleteDataItems(item.uri)
                continue
            }
            val raw = DataMapItem.fromDataItem(item).dataMap.getString("snapshot")
            when (item.uri.path) {
                ACTIVE_WORKOUT_PATH -> WorkoutSnapshotStore.save(this, raw)
                WearPhoneAutomatic.PATH -> WearPhoneAutomatic.receive(this,DataMapItem.fromDataItem(item).dataMap.getString("automatic"))
                WATCH_HOME_PATH -> WatchHomeStore.save(this, raw, DataMapItem.fromDataItem(item).dataMap.getLong("publishedAt"))
            }
            WearCommandOutbox.flush(this)
            WearPhoneAutomatic.applyCached(this)
            if(WorkoutSnapshotStore.load(this)==null && WearHealthStore.active(this).isNotBlank())WearHealthService.stop(this)
            WearSurfaces.refresh(this,true)
        }
    }

    companion object {
        const val ACTIVE_WORKOUT_PATH = "/trainpilot/active-workout"
        const val WATCH_HOME_PATH = "/trainpilot/watch-home"
        const val COMMAND_PATH_PREFIX = "/trainpilot/workout-command/"
    }
}
