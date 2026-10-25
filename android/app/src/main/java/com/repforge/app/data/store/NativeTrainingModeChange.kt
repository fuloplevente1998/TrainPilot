package com.repforge.app.data.store

import org.json.JSONObject
import java.time.Instant

/**
 * Mode changes are settings-only proposals. Preview is detached, approval is
 * guarded by the exact DataStore JSON snapshot, and undo is single-use.
 * Workout / program recipes are deliberately not part of this API.
 */
internal object NativeTrainingModeChange {
    data class Preview(
        val scope: String, val programId: String?, val expectedRaw: String?,
        val expectedActiveProgramId: String, val before: NativeTrainingModes.Effective,
        val after: NativeTrainingModes.Effective
    ) {
        val key: String get() = if (scope == "global") "global" else "program:$programId"
    }

    fun prepare(raw: String?, activeProgramId: String, scope: String, programId: String?,
        style: String, progression: String): Preview {
        require(scope == "global" || scope == "program" && !programId.isNullOrBlank() &&
            programId.length < 256 && programId == activeProgramId) { "Inactive or invalid program" }
        require(style in setOf("normal", "hit") && progression in setOf("double", "linear", "rir")) {
            "Invalid training mode"
        }
        val pid = if (scope == "global") null else programId
        val before = NativeTrainingModes.resolveSavedJson(raw, pid)
        return Preview(scope, pid, raw, activeProgramId, before,
            NativeTrainingModes.Effective(programId = pid, trainingStyle = style,
                progressionMode = progression, explicitChoice = true,
                source = if (scope == "global") "global" else "program"))
    }

    fun next(preview: Preview, now: String = Instant.now().toString()): JSONObject {
        val previous = runCatching { preview.expectedRaw?.let(::JSONObject) }.getOrNull()
        val base = if (previous?.optInt("schemaVersion", -1) == 1)
            JSONObject(previous.toString()) else JSONObject()
        val defaults = base.optJSONObject("defaults")?.takeIf {
            it.optString("trainingStyle") in setOf("normal", "hit") &&
                it.optString("progressionMode") in setOf("double", "linear", "rir")
        }?.let { JSONObject(it.toString()) } ?: JSONObject()
            .put("trainingStyle", "normal").put("progressionMode", "double")
        if (preview.scope == "global") {
            defaults.put("trainingStyle", preview.after.trainingStyle)
                .put("progressionMode", preview.after.progressionMode)
        }
        val overrides = base.optJSONObject("byProgram")?.let { JSONObject(it.toString()) } ?: JSONObject()
        if (preview.scope == "program") {
            overrides.put(requireNotNull(preview.programId), JSONObject()
                .put("trainingStyle", preview.after.trainingStyle)
                .put("progressionMode", preview.after.progressionMode)
                .put("explicitChoice", true).put("updatedAt", now))
        }
        val undoPrevious = previous?.let { JSONObject(it.toString()).apply { remove("undo") } }
        val rev = (base.opt("revision") as? Number)?.toLong()?.takeIf { it in 0..(Long.MAX_VALUE - 1) } ?: 0
        return base.put("schemaVersion", 1).put("defaults", defaults)
            .put("explicitChoice", if (preview.scope == "global") true else base.opt("explicitChoice") == true)
            .put("byProgram", overrides).put("revision", rev + 1).put("updatedAt", now)
            .put("undo", JSONObject().put("scope", preview.key)
                .put("previous", undoPrevious ?: JSONObject.NULL))
    }

    suspend fun approve(store: TrainPilotSettingsStore, preview: Preview): Boolean {
        if (store.current().activeProgramId != preview.expectedActiveProgramId) return false
        return store.saveApprovedTrainingModes197(preview.expectedRaw, next(preview))
    }

    fun undoValue(raw: String?, scope: String, programId: String?): JSONObject? {
        val key = if (scope == "global") "global" else "program:$programId"
        if (scope != "global" && (programId.isNullOrBlank() || programId.length >= 256)) return null
        val current = runCatching { raw?.let(::JSONObject) }.getOrNull() ?: return null
        val undo = current.optJSONObject("undo") ?: return null
        if (undo.optString("scope") != key || !undo.has("previous")) return null
        val previous = undo.opt("previous")
        if (previous == JSONObject.NULL) return JSONObject().put("_native197Clear", true)
        if (previous !is JSONObject) return null
        return JSONObject(previous.toString()).apply { remove("undo") }
    }

    suspend fun undo(store: TrainPilotSettingsStore, scope: String, programId: String?): Boolean {
        val current = store.current()
        if (scope == "program" && programId != current.activeProgramId) return false
        val raw = current.trainingModes197Json
        val result = undoValue(raw, scope, programId) ?: return false
        return store.saveApprovedTrainingModes197(raw, if (result.optBoolean("_native197Clear")) null else result)
    }
}
