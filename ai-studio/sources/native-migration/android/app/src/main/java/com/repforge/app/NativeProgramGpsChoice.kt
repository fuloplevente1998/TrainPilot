package com.repforge.app

import android.content.Context
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Metadata for an explicit start dialog; reading it never creates a session or starts GPS. */
internal object NativeProgramGpsChoice {
    data class Option(val programId: String, val activityId: String, val name: String, val remembered: Boolean)
    suspend fun read(context: Context, requestedProgramId: String?, dayId: String,
        database: TrainPilotDatabase = TrainPilotDatabase.getInstance(context)): Option? = withContext(Dispatchers.IO) {
        val settings = TrainPilotSettingsStore(context).current()
        val programs = database.programDao().getProgramsWithDays()
        val program = if (requestedProgramId != null) programs.firstOrNull { it.program.id == requestedProgramId }
            else programs.firstOrNull { it.program.id == settings.activeProgramId } ?: programs.firstOrNull()
        program ?: return@withContext null
        val day = program.days.firstOrNull { it.dayId == dayId } ?: return@withContext null
        val first = JSONArray(day.exercisesJson).opt(0)
        val id = if (first is JSONObject) first.optString("id") else first as? String ?: return@withContext null
        val exercise = database.exerciseLibraryDao().getExerciseById(id) ?: return@withContext null
        if (!NativeOutdoorStart.eligible(id, exercise.measurementType, exercise.repUnit)) return@withContext null
        val name = when (settings.language) { "en" -> exercise.nameEn; "de" -> exercise.nameDe; "ro" -> exercise.nameRo; else -> exercise.nameHu }
        Option(program.program.id, id, name.ifBlank { exercise.nameHu }, settings.gpsStartChoices216[id] == true)
    }
}
