package com.repforge.app.data.store

import org.json.JSONObject

/**
 * The read-only #197 policy resolver, matching main's final tp197TrainingModes.
 * Config is settings.trainingModes197, not a program prescription.
 *
 * No choice is activated by a default value alone: explicitChoice is required.
 * Malformed program overrides fail closed rather than silently taking the global mode.
 * Calling this never edits settings, history, active sessions or drafts.
 */
internal object NativeTrainingModes {
    data class Effective(
        val schemaVersion: Int = 1,
        val programId: String?,
        val trainingStyle: String = "normal",
        val progressionMode: String = "double",
        val explicitChoice: Boolean = false,
        val source: String = "legacy"
    )

    private fun validChoice(value: Any?): JSONObject? {
        val choice = value as? JSONObject ?: return null
        if (choice.optString("trainingStyle") !in setOf("normal", "hit")) return null
        if (choice.optString("progressionMode") !in setOf("double", "linear", "rir")) return null
        return choice
    }

    fun resolveSavedJson(json: String?, programId: String?): Effective {
        val obj = runCatching { json?.let(::JSONObject) }.getOrNull()
        return resolve(obj, programId)
    }

    fun resolve(config: JSONObject?, programId: String?): Effective {
        val pid = programId?.takeIf { it.isNotBlank() }
        val legacy = Effective(programId = pid)
        if (config == null) return legacy
        val version = config.opt("schemaVersion") as? Number ?: return legacy
        if (version.toDouble() != 1.0) return legacy
        val globalExplicit = config.opt("explicitChoice") as? Boolean ?: return legacy
        val defaults = validChoice(config.opt("defaults")) ?: return legacy
        val overrides = when {
            !config.has("byProgram") -> null
            config.opt("byProgram") is JSONObject -> config.optJSONObject("byProgram")
            else -> return legacy
        }
        if (pid != null && overrides?.has(pid) == true) {
            val override = overrides.opt(pid) as? JSONObject ?: return legacy
            val explicit = override.opt("explicitChoice") as? Boolean ?: return legacy
            if (explicit) {
                val mode = validChoice(override) ?: return legacy
                return Effective(programId = pid, trainingStyle = mode.getString("trainingStyle"),
                    progressionMode = mode.getString("progressionMode"),
                    explicitChoice = true, source = "program")
            }
        }
        if (!globalExplicit) return legacy
        return Effective(programId = pid, trainingStyle = defaults.getString("trainingStyle"),
            progressionMode = defaults.getString("progressionMode"),
            explicitChoice = true, source = "global")
    }
}
