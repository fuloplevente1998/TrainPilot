package com.repforge.app.wear

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

class WearDataListenerService : WearableListenerService() {
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type != DataEvent.TYPE_CHANGED) continue
            val item = event.dataItem
            val raw = DataMapItem.fromDataItem(item).dataMap.getString("snapshot")
            when (item.uri.path) {
                ACTIVE_WORKOUT_PATH -> WorkoutSnapshotStore.save(this, raw)
                WATCH_HOME_PATH -> WatchHomeStore.save(this, raw)
            }
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
