package com.repforge.wear

import android.util.Log
import com.google.android.gms.wearable.*
import org.json.JSONObject

/**
 * A Wear OS órán futó listener, amely fogadja a telefon által küldött edzésállapotot
 * és Ownership Handoff eseményeket.
 */
class PhoneMessageListenerService : WearableListenerService() {

    private val tag = "PhoneMessageListener"

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == "/trainpilot/workout/state") {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val stateName = dataMap.getString("state", "IDLE")
                val duration = dataMap.getLong("durationSeconds", 0L)
                val rest = dataMap.getInt("restSecondsRemaining", 0)
                val isRest = dataMap.getBoolean("isRestActive", false)

                WearWorkoutManager.getInstance(applicationContext).updateFromPhone(
                    stateName = stateName,
                    elapsedSeconds = duration,
                    restSeconds = rest,
                    isRestActive = isRest
                )
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val path = messageEvent.path
        val payload = messageEvent.data?.let { String(it, Charsets.UTF_8) } ?: ""

        when (path) {
            "/trainpilot/workout/handoff" -> {
                Log.i(tag, "Ownership Handoff üzenet érkezett a telefontól: $payload")
                try {
                    val json = JSONObject(payload)
                    val target = json.optString("target")
                    if (target == "WATCH") {
                        WearWorkoutManager.getInstance(applicationContext).updateFromPhone(
                            stateName = "WORK",
                            elapsedSeconds = 0L,
                            restSeconds = 0,
                            isRestActive = false
                        )
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Hiba a Handoff értelmezésekor", e)
                }
            }
        }
    }
}
