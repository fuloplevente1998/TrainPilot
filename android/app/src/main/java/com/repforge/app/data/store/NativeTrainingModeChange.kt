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
        val after: NativeTrainingModes.Effective, val inheritGlobal: Boolean = false
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
        val before = if (scope == "global") NativeTrainingModes.globalChoice(raw)
            else NativeTrainingModes.resolveSavedJson(raw, pid)
        return Preview(scope, pid, raw, activeProgramId, before,
            NativeTrainingModes.Effective(programId = pid, trainingStyle = style,
                progressionMode = progression, explicitChoice = true,
                source = if (scope == "global") "global" else "program"))
    }

    /** Explicitly stops overriding an active program; only then does global inheritance apply. */
    fun prepareInherit(raw: String?, activeProgramId: String, programId: String): Preview {
        require(programId.isNotBlank() && programId.length < 256 && programId == activeProgramId)
        val base = runCatching { raw?.let(::JSONObject) }.getOrNull() ?: JSONObject()
        val prospective = JSONObject(base.toString())
        val overrides = prospective.optJSONObject("byProgram")?.let { JSONObject(it.toString()) } ?: JSONObject()
        overrides.put(programId, JSONObject().put("explicitChoice", false))
        prospective.put("byProgram", overrides)
        return Preview("program", programId, raw, activeProgramId,
            NativeTrainingModes.resolveSavedJson(raw, programId),
            NativeTrainingModes.resolve(prospective, programId), inheritGlobal = true)
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
            val override = if (preview.inheritGlobal)
                JSONObject().put("explicitChoice", false).put("updatedAt", now)
            else JSONObject().put("trainingStyle", preview.after.trainingStyle)
                .put("progressionMode", preview.after.progressionMode)
                .put("explicitChoice", true).put("updatedAt", now)
            overrides.put(requireNotNull(preview.programId), override)
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
        return store.saveApprovedTrainingModes197(preview.expectedRaw, next(preview), preview.expectedActiveProgramId)
    }

    data class UndoValue(val previous: JSONObject?)

    fun undoValue(raw: String?, scope: String, programId: String?): UndoValue? {
        val key = if (scope == "global") "global" else "program:$programId"
        if (scope != "global" && (programId.isNullOrBlank() || programId.length >= 256)) return null
        val current = runCatching { raw?.let(::JSONObject) }.getOrNull() ?: return null
        val undo = current.optJSONObject("undo") ?: return null
        if (undo.optString("scope") != key || !undo.has("previous")) return null
        val previous = undo.opt("previous")
        if (previous == JSONObject.NULL) return UndoValue(null)
        if (previous !is JSONObject) return null
        return UndoValue(JSONObject(previous.toString()).apply { remove("undo") })
    }

    suspend fun undo(store: TrainPilotSettingsStore, scope: String, programId: String?): Boolean {
        val current = store.current()
        if (scope == "program" && programId != current.activeProgramId) return false
        val raw = current.trainingModes197Json
        val result = undoValue(raw, scope, programId) ?: return false
        return store.saveApprovedTrainingModes197(raw, result.previous,
            if (scope == "program") current.activeProgramId else null, restoringUndo = true)
    }
}
