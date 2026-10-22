package com.repforge.app

import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

/** Synthetic, non-personal data shared by the old-APK seed and native assertions. */
internal object UpgradeFixture {
    const val PHOTO_ID = "00000000-0000-0000-0000-000000000123"
    const val ROUTE_KEY = "migration-route"
    val notes = "Árvíztűrő 🏋️ \"idézet\"\n".repeat(2400)
    val routeText = "1\t47.5000000\t19.0000000\t5.0\t0\t1792659600000\n1\t47.5001000\t19.0001000\t5.0\t1000\t1792659601000\n"
    fun digest(text: String) = MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it.toInt() and 255) }
    fun workout(id: String) = JSONObject().put("id", id).put("syncId", id)
        .put("started", "2026-10-21T09:00:00Z").put("finished", "2026-10-21T09:30:00Z")
        .put("programId", "migration-program").put("workout", "A").put("notes", notes)
        .put("photos", JSONArray().put(JSONObject().put("id", PHOTO_ID).put("width", 1).put("height", 1)))
        .put("exercises", JSONArray().put(JSONObject().put("id", "side-plank").put("name", "Oldalsó plank")
            .put("repUnit", "mp/oldal").put("sets", JSONArray().put(JSONObject().put("setId", "migration-set")
                .put("reps", "26").put("weight", 0).put("done", true).put("leftSeconds", 12).put("rightSeconds", 14)
                .put("actualRir", 2).put("gpsRouteKey193", ROUTE_KEY)))))
    fun values(): Map<String, String> = linkedMapOf(
        "repforge:history" to JSONArray().put(workout("migration-workout")).put(workout("deleted-workout")).toString(),
        "repforge:phase3HistoryTombstones" to JSONArray().put(JSONObject().put("ids", JSONArray().put("deleted-workout")).put("key", "deleted-workout").put("deletedAt", 1792659700000L)).toString(),
        "repforge:programs" to JSONArray().put(JSONObject().put("id", "migration-program").put("name", "Saját terv 🏋️")
            .put("days", JSONArray().put(JSONObject().put("id", "A").put("name", "Első nap").put("exercises", JSONArray().put("side-plank"))))).toString(),
        "repforge:weights" to JSONArray().put(JSONObject().put("id", "migration-weight").put("date", "2026-10-21").put("kg", 77.5)).toString(),
        "repforge:scheduled" to JSONArray().put(JSONObject().put("id", "migration-calendar").put("date", "2026-10-23").put("programId", "migration-program").put("dayId", "A")).toString(),
        "repforge:settings" to JSONObject().put("hapticSetRecorded227", false).toString(),
        "repforge:activeProgramId" to "\"migration-program\"",
        "repforge:language" to "\"de\"",
        "repforge:draft" to JSONObject().put("syncId", "archived-draft").put("started", "2026-10-21T11:00:00Z").toString(),
        "repforge:futureSetting" to JSONObject().put("text", "Ismeretlen mező megmarad").toString()
    )
}
