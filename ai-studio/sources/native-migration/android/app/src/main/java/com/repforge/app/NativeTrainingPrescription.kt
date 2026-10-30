package com.repforge.app

import org.json.JSONObject

/** Canonical recipe from main 7473b30 tp128Rx and generatePersonalProgram Level2 overrides. */
internal object NativeTrainingPrescription {
    fun create(library: JSONObject, profile: JSONObject, level2: Boolean = false): JSONObject {
        val id = library.getString("id")
        val compound = library.optBoolean("compound")
        var reps = if (library.optString("repUnit").contains("mp")) "20–40 mp"
            else if (compound) if (profile.optString("goal") == "strength" || level2) "6–10" else "8–12" else "10–15"
        if (id in setOf("pushup", "decline-pushup", "modified-pushup", "incline-pushup")) reps = if (level2) "6–12" else "8–15"
        if (id == "lateral-raise") reps = "12–20"
        if (id == "crunch") reps = "10–20"
        if (id == "db-ohp") reps = "6–10"
        return JSONObject().put("sets", if (profile.optString("experience") == "intermediate" &&
            profile.optString("goal") == "muscle" && profile.optInt("minutes") >= 60 && profile.optString("activity") != "physical") 3 else 2)
            .put("reps", reps).put("weight", 0).put("trial", true)
            .put("rest", if (compound) if (profile.optString("goal") == "strength") 150 else 120 else 75)
            .put("rir", 2).put("rirMin", 1).put("rirMax", 2).put("progressionLevel", 0)
            .put("loadType", library.getString("loadType"))
    }

    fun level2(library: JSONObject, profile: JSONObject): JSONObject {
        val rx = create(library, profile, true)
        val gear = profile.optJSONArray("gear")
        val dumbbell = (0 until (gear?.length() ?: 0)).any { gear?.optString(it) in setOf("dumbbell", "dumbbells") }
        if (library.getString("id") in setOf("side-plank", "crunch") && dumbbell)
            rx.put("variation", "weighted").put("loadType", "single_dumbbell").put("progressionLevel", 2)
        if (library.getString("id") == "pushup") rx.put("variation", "paused").put("progressionLevel", 1)
        return rx
    }

    fun needsLoad(rx: JSONObject): Boolean = rx.optBoolean("trial") && rx.optDouble("weight", Double.NaN) == 0.0 &&
        rx.optString("loadType") in setOf("total", "per_hand", "single_dumbbell")
}
