package com.repforge.app.ui.onboarding

import org.json.JSONObject

/** No independent profile or workout engine: this only decides whether to show the first-run flow. */
internal object NativeOnboardingPolicy {
    const val WELCOME = "welcome"
    const val IN_PROGRESS = "in_progress"
    const val SKIPPED = "skipped"
    const val COMPLETE = "complete"

    enum class Destination { WELCOME, PROFILE, HOME }

    fun destination(state: String): Destination = when (state) {
        WELCOME, "pending" -> Destination.WELCOME // #128 hybrid used "pending".
        IN_PROGRESS -> Destination.PROFILE
        else -> Destination.HOME
    }

    fun normalizedImportedState(state: String, importedDraft: Boolean): String {
        if (importedDraft) return COMPLETE
        return when (state) {
            SKIPPED -> SKIPPED
            IN_PROGRESS -> IN_PROGRESS
            COMPLETE -> COMPLETE
            // Legacy "pending" referred to welcome rendering, not consent to start over.
            else -> COMPLETE
        }
    }

    /**
     * LegacyLocalStorageReader always adds a legacyStorageRaw JSON entry, including
     * for an empty Chromium namespace; that synthetic entry is not user data.
     * Unknown nonempty legacy keys are conservatively treated as existing data.
     */
    fun hasLegacyEvidence(values: Map<String, String>): Boolean {
        if (values.any { (key, value) -> key != "legacyStorageRaw" && value.isNotBlank() }) return true
        val raw = values["legacyStorageRaw"] ?: return false
        return runCatching { JSONObject(raw).length() > 0 }.getOrDefault(true)
    }

    fun initialState(
        existing: String?,
        legacyEvidence: Boolean,
        existingProfileJson: String
    ): String {
        if (existing != null) return when (existing) {
            WELCOME, IN_PROGRESS, SKIPPED, COMPLETE -> existing
            "pending" -> WELCOME
            else -> COMPLETE
        }
        val hasProfile = runCatching { JSONObject(existingProfileJson).length() > 0 }.getOrDefault(true)
        return if (!legacyEvidence && !hasProfile) WELCOME else COMPLETE
    }

    fun canTransition(from: String, to: String): Boolean = when (to) {
        IN_PROGRESS -> from == WELCOME || from == "pending" || from == SKIPPED
        SKIPPED -> from == WELCOME || from == "pending" || from == IN_PROGRESS
        else -> false
    }
}
