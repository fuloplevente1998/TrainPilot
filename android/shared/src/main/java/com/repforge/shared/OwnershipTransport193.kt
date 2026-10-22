package com.repforge.shared

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import org.json.JSONObject
import java.nio.charset.StandardCharsets

object OwnershipTransport193 {
    private val attempts = HashMap<String, Long>()

    fun interface Receiver {
        @Throws(Exception::class)
        fun receive(message: JSONObject, origin: String, localNode: String)
    }

    fun interface Nodes {
        @Throws(Exception::class)
        fun ready(localNode: String, peerNode: String)
    }

    @JvmStatic
    fun nodes(context: Context, callback: Nodes) {
        Wearable.getNodeClient(context).localNode.addOnSuccessListener { local ->
            Wearable.getNodeClient(context).connectedNodes.addOnSuccessListener { peers ->
                if (peers.size != 1) return@addOnSuccessListener
                try {
                    callback.ready(local.id, peers[0].id)
                } catch (_: Exception) {
                    // No transfer without durable state.
                }
            }
        }
    }

    @JvmStatic
    @Throws(Exception::class)
    fun send(context: Context, journal: OwnershipJournal193) {
        val outgoing = journal.read().optJSONObject("outgoing") ?: return
        val id = outgoing.getString("id")
        val kind = outgoing.getString("kind")
        val attempt = outgoing.getString("sender") + ":" + id + ":" + kind
        synchronized(attempts) {
            val now = SystemClock.elapsedRealtime()
            val lastAttempt = attempts[attempt]
            if (lastAttempt != null && now - lastAttempt < 5_000L) return
            if (attempts.size > 64) attempts.clear()
            attempts[attempt] = now
        }
        val item = PutDataMapRequest.create(WorkoutOwnership193.PATH + outgoing.getString("sender") + "/" + id)
        item.dataMap.putString("ownership", outgoing.toString())
        item.dataMap.putLong("retryAt", System.currentTimeMillis())
        Wearable.getDataClient(context).putDataItem(item.asPutDataRequest().setUrgent())
            .addOnSuccessListener {
                if ("installed" == kind) {
                    try {
                        journal.installedReceiptPublished(id)
                    } catch (_: Exception) {
                        // Replayable until durable.
                    }
                }
            }
    }

    @JvmStatic
    fun receive(context: Context, item: DataItem?, callback: Receiver): Boolean {
        val itemUri = item?.uri ?: return false
        val path = itemUri.path ?: return false
        if (!path.startsWith(WorkoutOwnership193.PATH)) return false
        try {
            val raw = DataMapItem.fromDataItem(item).dataMap.getString("ownership")
            if (raw == null || raw.toByteArray(StandardCharsets.UTF_8).size > 96_000) return true
            val message = JSONObject(raw)
            val origin = itemUri.host ?: return true
            if ((WorkoutOwnership193.PATH + origin + "/" + message.optString("id")) != path) {
                return true
            }
            Wearable.getNodeClient(context).localNode.addOnSuccessListener { local ->
                if (local.id == origin) return@addOnSuccessListener
                Handler(Looper.getMainLooper()).post {
                    try {
                        callback.receive(message, origin, local.id)
                        if ("installed" == message.optString("kind")) {
                            // The source durably settled before returning. Remove only this acknowledged transfer pair.
                            Wearable.getDataClient(context).deleteDataItems(itemUri)
                            val own = Uri.Builder()
                                .scheme("wear")
                                .authority(local.id)
                                .path(WorkoutOwnership193.PATH + local.id + "/" + message.getString("id"))
                                .build()
                            Wearable.getDataClient(context).deleteDataItems(own)
                        }
                    } catch (_: Exception) {
                        // Invalid/stale messages keep the existing owner.
                    }
                }
            }
        } catch (_: Exception) {
            // A bad item cannot change ownership.
        }
        return true
    }
}
