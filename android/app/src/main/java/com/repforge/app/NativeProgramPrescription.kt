package com.repforge.app

import com.repforge.app.data.db.ExerciseLibraryEntity
import com.repforge.app.data.db.ProgramEntity
import org.json.JSONObject
import org.json.JSONTokener

/** Main tp152Rx/tp107SaveProgramRow: one program-level prescription per exercise ID.
 * Only explicitly edited fields become a patch; inline variation/lineage and unknown fields survive.
 */
internal object NativeProgramPrescription {
    data class Fields(val sets: String, val reps: String, val weight: String, val rest: String)
    data class Source(val loadType: String, val repUnit: String, val measurementType: String)
    data class Edit(val patch: String, val source: Source)
    fun source(exercise: ExerciseLibraryEntity) = Source(exercise.loadType, exercise.repUnit, exercise.measurementType)
    private val keys = setOf("sets", "reps", "weight", "rest", "trial")
    fun allowsTrial(loadType: String) = loadType in setOf("total", "per_hand", "single_dumbbell")

    fun effective(program: ProgramEntity?, entry: String, source: ExerciseLibraryEntity,
        edits: Map<String, Edit>, defaultRest: Int, parsedPrescriptions: JSONObject? = null): JSONObject {
        val id = NativeProgramEditor.exerciseId(entry)
        require(id == source.id)
        val result = JSONObject().put("sets", source.defaultSets).put("reps", source.defaultReps)
            .put("weight", source.defaultWeight).put("rest", defaultRest).put("loadType", source.loadType)
            .put("repUnit", source.repUnit).put("measurementType", source.measurementType)
        fun overlay(value: JSONObject?) {
            if (value != null) for (key in value.keys()) if (key != "id") result.put(key, value.get(key))
        }
        val prescriptions = parsedPrescriptions ?: program?.rawJson?.let(::JSONObject)?.optJSONObject("prescriptions")
        overlay(prescriptions?.optJSONObject(id))
        overlay(JSONTokener(entry).nextValue() as? JSONObject)
        overlay(edits[id]?.patch?.let(::JSONObject))
        return result
    }

    fun fields(effective: JSONObject) = Fields(effective.get("sets").toString(), effective.optString("reps"),
        if (allowsTrial(effective.optString("loadType")) && effective.optBoolean("trial") &&
            effective.optDouble("weight") == 0.0) "" else effective.get("weight").toString(),
        effective.get("rest").toString())

    fun edit(fields: Fields, loadType: String): JSONObject {
        val sets = fields.sets.trim().toIntOrNull() ?: throw IllegalArgumentException("Invalid sets")
        val rest = fields.rest.trim().toIntOrNull() ?: throw IllegalArgumentException("Invalid rest")
        val trial = loadType != "bodyweight" && fields.weight.isBlank() && allowsTrial(loadType)
        val weight = if (loadType == "bodyweight" || trial) 0.0
            else fields.weight.trim().replace(',', '.').toDoubleOrNull() ?: throw IllegalArgumentException("Invalid weight")
        require(weight.isFinite() && weight in 0.0..10000.0)
        return JSONObject().put("sets", sets).put("reps", fields.reps.trim()).put("weight", weight)
            .put("rest", rest).put("trial", trial).also { validate(it, loadType) }
    }

    fun validate(patch: JSONObject, loadType: String? = null) {
        require(patch.keys().asSequence().toSet() == keys) { "Invalid prescription fields" }
        fun integer(key: String, range: IntRange) {
            val value = patch.opt(key) as? Number ?: throw IllegalArgumentException("Invalid $key")
            require(value.toDouble().isFinite() && value.toDouble() == value.toInt().toDouble() && value.toInt() in range)
        }
        integer("sets", 1..10); integer("rest", 30..300)
        val reps = patch.opt("reps") as? String ?: throw IllegalArgumentException("Invalid target")
        require(reps.isNotBlank() && reps.length <= 40 && reps.none(Char::isISOControl))
        val weight = (patch.opt("weight") as? Number)?.toDouble() ?: throw IllegalArgumentException("Invalid weight")
        require(weight.isFinite() && weight in 0.0..10000.0)
        val trial = patch.opt("trial") as? Boolean ?: throw IllegalArgumentException("Invalid trial state")
        require(!trial || (weight == 0.0 && (loadType == null || allowsTrial(loadType))))
        require(loadType != "bodyweight" || (weight == 0.0 && !trial))
    }

    fun applyToInline(entry: String, patch: JSONObject?): Any {
        val decoded = JSONTokener(entry).nextValue()
        if (decoded is JSONObject && patch != null) for (key in patch.keys()) decoded.put(key, patch.get(key))
        return decoded
    }
}
