package com.repforge.app.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable

class MainActivity : ComponentActivity(), DataClient.OnDataChangedListener {
    private val workout = mutableStateOf<WearWorkout?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        workout.value = WorkoutSnapshotStore.load(this)
        setContent {
            TrainPilotWearApp(workout)
        }
    }

    override fun onResume() {
        super.onResume()
        workout.value = WorkoutSnapshotStore.load(this)
        Wearable.getDataClient(this).addListener(this)
    }

    override fun onPause() {
        Wearable.getDataClient(this).removeListener(this)
        super.onPause()
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type != DataEvent.TYPE_CHANGED) continue
            val item = event.dataItem
            if (item.uri.path != WearDataListenerService.ACTIVE_WORKOUT_PATH) continue
            val raw = DataMapItem.fromDataItem(item).dataMap.getString("snapshot")
            val parsed = WorkoutSnapshotStore.save(this, raw)
            runOnUiThread { workout.value = parsed }
        }
    }
}

@Composable
private fun TrainPilotWearApp(workout: State<WearWorkout?>) {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            WorkoutScreen(workout.value)
        }
    }
}

@Composable
private fun WorkoutScreen(workout: WearWorkout?) {
    if (workout == null) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "TrainPilot",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Nincs aktív edzés",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Indíts edzést a telefonon.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val exercise = workout.exercise
    val set = workout.currentSet
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = workout.programName.ifBlank { "TrainPilot" },
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = exercise?.name.orEmpty(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = formatSet(set),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Text(
            text = if (exercise == null || exercise.sets.isEmpty()) {
                "Nincs sorozat"
            } else {
                "Sorozat " + (workout.currentSetIndex + 1) + " / " + exercise.sets.size
            },
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun formatSet(set: WearSet?): String {
    if (set == null) return ""
    val reps = set.reps.ifBlank { "–" }
    val weight = set.weight.trim()
    return if (weight.isBlank() || weight == "0" || weight == "0.0") {
        reps + " ism."
    } else {
        weight + " kg × " + reps
    }
}
