package com.repforge

import android.util.Log
import com.google.android.gms.wearable.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * A Wear OS okosóráról érkező parancsokat, kézfogásokat és méréseket fogadó Service.
 */
class WearCommandListenerService : WearableListenerService() {

    private val tag = "WearCommandListener"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val PATH_COMMAND = "/trainpilot/workout/command"
        const val PATH_HANDOFF = "/trainpilot/workout/handoff"
        const val PATH_ROUTE = "/trainpilot/workout/route"
        const val PATH_SPOOL = "/trainpilot/sync/outbox"
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val path = messageEvent.path
        val rawData = messageEvent.data?.let { String(it, Charsets.UTF_8) } ?: ""
        val senderNode = messageEvent.sourceNodeId

        Log.d(tag, "Üzenet érkezett a Wear OS óráról: Path=$path Node=$senderNode Payload=$rawData")

        when (path) {
            PATH_COMMAND -> handleCommandMessage(senderNode, rawData)
            PATH_HANDOFF -> handleHandoffMessage(senderNode, rawData)
            PATH_ROUTE -> handleRouteData(senderNode, rawData)
            PATH_SPOOL -> handleOutboxSpool(senderNode, rawData)
            else -> Log.w(tag, "Ismeretlen Data Layer path: $path")
        }
    }

    private fun handleCommandMessage(sourceNodeId: String, jsonString: String) {
        scope.launch {
            try {
                if (jsonString.isBlank()) return@launch
                val json = JSONObject(jsonString)
                val command = json.optString("command")

                when (command) {
                    "START" -> {
                        val title = json.optString("title", "Wear Edzés")
                        Log.i(tag, "Wear parancs: START ($title)")
                    }
                    "PAUSE" -> {
                        Log.i(tag, "Wear parancs: PAUSE")
                    }
                    "RESUME" -> {
                        Log.i(tag, "Wear parancs: RESUME")
                    }
                    "COMPLETE_SET" -> {
                        val setId = json.optLong("setId")
                        val reps = json.optInt("reps")
                        val weight = json.optDouble("weight")
                        val rir = json.optInt("rir", 2)
                        Log.i(tag, "Wear parancs: COMPLETE_SET (setId=$setId, reps=$reps, weight=$weight, rir=$rir)")
                    }
                    "UNDO_SET" -> {
                        val setId = json.optLong("setId")
                        Log.i(tag, "Wear parancs: UNDO_SET (setId=$setId)")
                    }
                    "SKIP_REST" -> {
                        Log.i(tag, "Wear parancs: SKIP_REST")
                    }
                    "ADJUST_REST" -> {
                        val delta = json.optInt("deltaSeconds", 30)
                        Log.i(tag, "Wear parancs: ADJUST_REST ($delta mp)")
                    }
                    "CANCEL" -> {
                        Log.i(tag, "Wear parancs: CANCEL (Megerősített edzésmegszakítás)")
                    }
                    "FINISH" -> {
                        Log.i(tag, "Wear parancs: FINISH")
                    }
                    else -> Log.w(tag, "Nem támogatott parancs: $command")
                }
            } catch (e: Exception) {
                Log.e(tag, "Hiba a Wear parancs feldolgozásakor: $jsonString", e)
            }
        }
    }

    private fun handleHandoffMessage(sourceNodeId: String, jsonString: String) {
        scope.launch {
            try {
                val json = JSONObject(jsonString)
                val action = json.optString("action")
                val seq = json.optLong("sequence")
                Log.i(tag, "Wear Ownership Handoff: Action=$action Seq=$seq Node=$sourceNodeId")
            } catch (e: Exception) {
                Log.e(tag, "Hiba a Handoff üzenet feldolgozásakor: $jsonString", e)
            }
        }
    }

    private fun handleRouteData(sourceNodeId: String, jsonString: String) {
        scope.launch {
            try {
                Log.d(tag, "Wear Route/GPS adatcsomag beérkezett (#193)")
            } catch (e: Exception) {
                Log.e(tag, "Hiba a route adat mentésekor: $jsonString", e)
            }
        }
    }

    private fun handleOutboxSpool(sourceNodeId: String, jsonString: String) {
        scope.launch {
            try {
                Log.d(tag, "Wear Outbox Spool szinkronizáció beérkezett")
            } catch (e: Exception) {
                Log.e(tag, "Hiba a Wear spool feldolgozásakor", e)
            }
        }
    }
}
