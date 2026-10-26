package com.repforge.app

import android.content.Context
import com.repforge.app.data.db.ProgramWithDays
import com.repforge.app.data.db.TrainPilotDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Only the two A/B days from the main's home-basic factory are resettable.
 * The main's legacy rf153ResetDay edits the A/B exercise composition, while its
 * settings reset also overwrites the global exercise catalog. We deliberately
 * do not expose the latter operation from the program screen.
 */
internal object NativeProgramActions {
    data class RestorePreview(
        val opened: ProgramWithDays,
        val factory: ProgramWithDays,
        val dayId: String,
        val before: List<String>,
        val after: List<String>,
        val resetRecipeIds: Set<String>,
        val otherDaysUsingRecipes: List<String>,
        val continuation: NativeDataReplacement.Continuation
    )

    suspend fun preview(context: Context, db: TrainPilotDatabase, opened: ProgramWithDays,
        dayId: String): RestorePreview {
        val ticket = NativeDataReplacement.continuationForCurrentData(context)
        return withContext(Dispatchers.IO) {
            require(dayId in setOf("A", "B")) { "Only factory A/B days can be restored" }
            val fresh = requireNotNull(db.programDao().getProgramWithDaysById(opened.program.id))
            check(fresh == opened && !fresh.program.builtin &&
                fresh.program.sourceTemplateId == "home-basic") { "Program changed; reopen preview" }
            val factory = requireNotNull(db.programDao().getProgramWithDaysById("home-basic"))
            check(factory.program.builtin)
            val existingDay = requireNotNull(fresh.days.singleOrNull { it.dayId == dayId })
            val factoryDay = requireNotNull(factory.days.singleOrNull { it.dayId == dayId })
            fun entries(raw: String): List<String> {
                val array = JSONArray(raw)
                return (0 until array.length()).map { index ->
                    val item = array.get(index)
                    if (item is String) JSONObject.quote(item) else (item as JSONObject).toString()
                }
            }
            val ids = entries(factoryDay.exercisesJson).map(NativeProgramEditor::exerciseId).toSet()
            val prescriptions = JSONObject(fresh.program.rawJson).optJSONObject("prescriptions")
            val editableKeys = setOf("sets", "reps", "weight", "rest", "trial")
            val touched = ids.filter { id ->
                prescriptions?.optJSONObject(id)?.keys()?.asSequence()?.any { it in editableKeys } == true
            }.toSet()
            val affected = fresh.days.filter { it.dayId != dayId &&
                (0 until JSONArray(it.exercisesJson).length()).any { index ->
                    val arr = JSONArray(it.exercisesJson)
                    val item = arr.get(index)
                    val id = if (item is String) item else (item as JSONObject).optString("id")
                    id in touched
                }
            }.map { it.dayId }
            ticket.requireCurrent(context)
            RestorePreview(fresh, factory, dayId, entries(existingDay.exercisesJson),
                entries(factoryDay.exercisesJson), touched, affected, ticket)
        }
    }

    suspend fun restore(context: Context, db: TrainPilotDatabase, preview: RestorePreview): String =
        NativeDataReplacement.mutate(context) {
            preview.continuation.requireCurrent(context)
            val factoryDay = requireNotNull(preview.factory.days.singleOrNull { it.dayId == preview.dayId })
            // A retry with an old preview never overwrites a committed reset.
            val draft = NativeProgramEditor.from(preview.opened, "hu")
            val restored = draft.copy(days = draft.days.map {
                if (it.id == preview.dayId) it.copy(name = factoryDay.nameHu, entries = preview.after)
                else it
            })
            NativeProgramEditor.save(context, db, preview.opened, restored, "hu",
                restoreProgram = preview.factory, restoreDayId = preview.dayId,
                restorePrescriptionIds = preview.resetRecipeIds)
        }
}
