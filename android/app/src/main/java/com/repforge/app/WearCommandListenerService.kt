package com.repforge.app

import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import com.repforge.shared.OwnershipTransport193
import com.repforge.shared.WearDataLayerPaths
import org.json.JSONObject
import java.nio.charset.StandardCharsets

class WearCommandListenerService : WearableListenerService() {
    companion object {
        const val COMMAND_PATH_PREFIX: String = WearDataLayerPaths.COMMAND_PATH_PREFIX
    }

    private fun queue(command: JSONObject, uri: Uri?) {
        try {
            WearCommandQueueStore.enqueue(this, command, uri?.toString())
        } catch (_: Exception) {
            // Retain the DataItem for scanning after storage recovers.
        }
    }

    override fun onMessageReceived(event: MessageEvent) {
        if (WearWorkoutHandoff.REQUEST != event.path || event.data.size > 4096) return
        try {
            val request = JSONObject(String(event.data, StandardCharsets.UTF_8))
            val snapshot = WearWorkoutHandoff.response(
                ActiveWorkoutStore.current(this),
                request,
                System.currentTimeMillis()
            ) ?: return
            Wearable.getMessageClient(this).sendMessage(
                event.sourceNodeId,
                WearWorkoutHandoff.REPLY,
                snapshot.toString().toByteArray(StandardCharsets.UTF_8)
            )
            val map = PutDataMapRequest.create(WearSyncRepository.ACTIVE_WORKOUT_PATH)
            map.dataMap.putString("snapshot", snapshot.toString())
            map.dataMap.putBoolean("active", true)
            map.dataMap.putLong("revision", snapshot.optLong("revision"))
            map.dataMap.putLong("refreshedAt", System.currentTimeMillis())
            Wearable.getDataClient(this).putDataItem(map.asPutDataRequest().setUrgent())
        } catch (_: Exception) {
        }
    }

    /** A reconnected watch can deliver its inverse before its completion; retry without waking any UI. */
    private fun drainNativeUndo188() {
        val pending = WearCommandQueueStore.pending(this)
        for (i in 0 until pending.length()) {
            try {
                val command = pending.getJSONObject(i)
                if ("undoSet" != command.optString("action") || !PhoneWorkoutExecution.compatible(command)) {
                    continue
                }
                val result = PhoneWorkoutExecution.execute(this, command)
                val status = result.optString("status")
                if (status in listOf("applied", "duplicate", "rejected")) {
                    PhoneWorkoutExecution.publish(this)
                    WearCommandReceipts.complete(
                        this,
                        command,
                        if ("rejected" == status) "error" else "applied",
                        null
                    )
                }
            } catch (_: Exception) {
                // Durable pending command remains retryable.
            }
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type != DataEvent.TYPE_CHANGED) continue
            val item = event.dataItem ?: continue
            val uri = item.uri ?: continue
            val path = uri.path
            if (OwnershipTransport193.receive(this, item) { message, origin, local ->
                    PhoneOwnership193.receive(this, message, origin, local)
                }
            ) {
                continue
            }
            if (WearLiveHealthStore.PATH == path) {
                try {
                    WearLiveHealthStore.save(
                        this,
                        DataMapItem.fromDataItem(item).dataMap.getString("health")
                    )
                } catch (_: Exception) {
                }
                continue
            }
            if (path == null || (!path.startsWith(COMMAND_PATH_PREFIX) && path != WearDataLayerPaths.WATCH_AUTOMATIC_PATH)) {
                continue
            }
            try {
                val raw = DataMapItem.fromDataItem(item).dataMap.getString("command")
                if (raw.isNullOrEmpty()) continue
                val command = JSONObject(raw)
                command.put("_uri", uri.toString())

                // 1. Private GPS route chunks (#193)
                if (PhoneGpsRoute193.receive(this, command, uri)) continue

                // 2. Ownership Epoch Fencing (#193)
                if (!PhoneOwnership193.commandAllowed(this, command)) {
                    WearCommandReceipts.complete(this, command, "superseded", uri)
                    continue
                }

                // 3. Deduplication Replay
                if (WearCommandQueueStore.seen(this, command.optString("commandId"))) {
                    WearCommandReceipts.replay(this, command, uri)
                    Handler(Looper.getMainLooper()).post(::drainNativeUndo188)
                    continue
                }

                // 4. Native Execution Protocol v2 (phone-owned sets, undoSet, timeline start)
                if (PhoneWorkoutExecution.compatible(command)) {
                    Handler(Looper.getMainLooper()).post {
                        try {
                            val result = PhoneWorkoutExecution.execute(this, command)
                            val status = result.optString("status")
                            if ("undoSet" == command.optString("action") && "rejected" == status) {
                                PhoneWorkoutExecution.publish(this)
                                if (!WearCommandReceipts.complete(this, command, "error", uri)) {
                                    queue(command, uri)
                                }
                            } else if (status !in listOf("applied", "duplicate") ||
                                !WearCommandReceipts.complete(this, command, "applied", uri)
                            ) {
                                queue(command, uri)
                            }
                            drainNativeUndo188()
                        } catch (_: Exception) {
                            queue(command, uri)
                        }
                    }
                    continue
                }

                // 5. Phone Interval Timer control (pause / resume / skipRest)
                if ("automaticControl" == command.optString("action")) {
                    Handler(Looper.getMainLooper()).post {
                        if (WearCommandQueueStore.seen(this, command.optString("commandId"))) {
                            WearCommandReceipts.replay(this, command, uri)
                            return@post
                        }
                        val timer = IntervalTimerService.snapshot(this)
                        val active = ActiveWorkoutStore.current(this)
                        val action = command.optString("value")
                        val autoObj = command.optJSONObject("automation")
                        if (active != null &&
                            autoObj != null &&
                            active.optString("workoutId") == command.optString("workoutId") &&
                            timer.optString("key") == autoObj.optString("key") &&
                            action in listOf("pause", "resume", "skipRest")
                        ) {
                            try {
                                val expectedPhase = autoObj.optLong("phaseNumber")
                                val phaseMatches = expectedPhase == timer.optLong("phaseNumber")
                                IntervalTimerService.command(
                                    this,
                                    timer.optString("key"),
                                    if (phaseMatches) action else "status"
                                )
                                WearCommandReceipts.complete(
                                    this,
                                    command,
                                    if (phaseMatches) "applied" else "superseded",
                                    uri
                                )
                                return@post
                            } catch (_: Exception) {
                            }
                        }
                        queue(command, uri)
                    }
                    continue
                }

                // 6. All other watch commands (startWorkout, finishWorkout, discardWorkout,
                //    saveWorkoutFeedback, healthSummary, updateSet, completeSet, requestHome, etc.)
                //    are now processed 100% natively in the background by WearSyncRepository!
                WearSyncRepository.processIncomingWatchCommandAsync(this, command, uri)
            } catch (_: Exception) {
                // Invalid or malformed watch commands must never affect the phone workout.
            }
        }
    }
}
