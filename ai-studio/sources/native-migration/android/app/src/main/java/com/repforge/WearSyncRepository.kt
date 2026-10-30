package com.repforge

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONObject

data class ConnectedWatchNode(
    val nodeId: String,
    val displayName: String,
    val isNearby: Boolean
)

/**
 * A Wear OS felé történő Data Layer szinkronizációt végző Repository.
 */
class WearSyncRepository(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    private val tag = "WearSyncRepository"

    private val messageClient: MessageClient = Wearable.getMessageClient(context)
    private val dataClient: DataClient = Wearable.getDataClient(context)
    private val nodeClient: NodeClient = Wearable.getNodeClient(context)

    companion object {
        const val CAPABILITY_WEAR = "trainpilot_wear_app"
        const val PATH_WORKOUT_STATE = "/trainpilot/workout/state"
        const val PATH_WORKOUT_COMMAND = "/trainpilot/workout/command"
        const val PATH_HANDOFF = "/trainpilot/workout/handoff"
        const val PATH_ROUTE = "/trainpilot/workout/route"
    }

    fun observeConnectedWatches(): Flow<List<ConnectedWatchNode>> = flow {
        while (true) {
            try {
                val nodesTask = nodeClient.connectedNodes
                val nodes = Tasks.await(nodesTask)
                val mapped = nodes.map { ConnectedWatchNode(it.id, it.displayName, it.isNearby) }
                emit(mapped)
            } catch (e: Exception) {
                Log.w(tag, "Hiba az óracsomópontok lekérésekor: ${e.message}")
                emit(emptyList())
            }
            delay(5000L)
        }
    }.flowOn(Dispatchers.IO)

    suspend fun syncWorkoutState(
        stateName: String,
        durationSeconds: Long,
        restSecondsRemaining: Int,
        isRestActive: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val putDataReq = PutDataMapRequest.create(PATH_WORKOUT_STATE).apply {
                dataMap.putString("state", stateName)
                dataMap.putLong("durationSeconds", durationSeconds)
                dataMap.putInt("restSecondsRemaining", restSecondsRemaining)
                dataMap.putBoolean("isRestActive", isRestActive)
                dataMap.putLong("timestamp", System.currentTimeMillis())
            }.asPutDataRequest().setUrgent()

            Tasks.await(dataClient.putDataItem(putDataReq))
            true
        } catch (e: Exception) {
            Log.e(tag, "Nem sikerült szinkronizálni az állapotot DataClient-tel", e)
            false
        }
    }

    suspend fun sendCommandToWear(command: String, payloadJson: JSONObject = JSONObject()): Boolean = withContext(Dispatchers.IO) {
        try {
            val nodes = Tasks.await(nodeClient.connectedNodes)
            if (nodes.isEmpty()) {
                Log.d(tag, "Nincs csatlakoztatott Wear OS óra a parancs küldéséhez ($command)")
                return@withContext false
            }

            payloadJson.put("command", command)
            payloadJson.put("timestamp", System.currentTimeMillis())
            val bytes = payloadJson.toString().toByteArray(Charsets.UTF_8)

            var success = false
            for (node in nodes) {
                val sendTask = messageClient.sendMessage(node.id, PATH_WORKOUT_COMMAND, bytes)
                Tasks.await(sendTask)
                success = true
            }
            success
        } catch (e: Exception) {
            Log.e(tag, "Hiba a parancs órára küldésekor: $command", e)
            false
        }
    }

    suspend fun sendOwnershipHandoffMessage(payload: JSONObject): Boolean = withContext(Dispatchers.IO) {
        try {
            val nodes = Tasks.await(nodeClient.connectedNodes)
            if (nodes.isEmpty()) return@withContext false

            val bytes = payload.toString().toByteArray(Charsets.UTF_8)
            var success = false
            for (node in nodes) {
                val sendTask = messageClient.sendMessage(node.id, PATH_HANDOFF, bytes)
                Tasks.await(sendTask)
                success = true
            }
            success
        } catch (e: Exception) {
            Log.e(tag, "Hiba az Ownership Handoff elküldésekor", e)
            false
        }
    }
}
