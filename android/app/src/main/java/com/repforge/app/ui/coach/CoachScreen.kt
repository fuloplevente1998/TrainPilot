package com.repforge.app.ui.coach

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.repforge.app.ui.theme.LocalTrainPilotColors
import java.text.NumberFormat
import java.util.Locale

@Composable
internal fun CoachScreen(starting: Boolean, onStart: (String, String, String?) -> Unit,
    onCalendar: () -> Unit, onHealth: () -> Unit, onProgress: () -> Unit, model: CoachViewModel = viewModel()) {
    val state by model.state.collectAsStateWithLifecycle()
    val colors = LocalTrainPilotColors.current
    val words = coachWords(state.language)
    val number = remember(state.language) { NumberFormat.getNumberInstance(Locale.forLanguageTag(state.language)).apply { maximumFractionDigits = 1 } }
    fun value(n: Number?) = n?.let { number.format(it) } ?: "—"
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, model) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) model.reload() }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val readiness = state.readiness
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("TrainPilot Coach", color = colors.text, style = MaterialTheme.typography.headlineSmall) }
        item {
            if (state.loading) CircularProgressIndicator()
            if (state.error) Text(words[0], color = colors.dangerText)
            Card(colors = CardDefaults.cardColors(containerColor = colors.card)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(words[1], color = colors.text)
                    Text(if (readiness?.score == null) "—" else "${value(readiness.score)}/100",
                        style = MaterialTheme.typography.headlineLarge, color = colors.accent)
                    val mode = when(readiness?.mode) { "normal" -> 2; "reduced" -> 3; "light" -> 4; else -> 5 }
                    Text(words[mode], color = colors.text)
                    if (readiness?.pain == true) Text(words[6], color = colors.dangerText)
                }
            }
        }
        val signals = listOf(words[7] to readiness?.samsungScore, words[8] to readiness?.trainpilotScore,
            words[9] to readiness?.sleepMinutes, "HRV (ms)" to readiness?.hrvMs,
            words[10] to readiness?.baselineMs, words[11] to readiness?.load)
        for (pair in signals.chunked(2)) item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((label, n) in pair) Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = colors.card2)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(label, color = colors.muted)
                        Text(value(n), color = colors.text, style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        }
        item { Text(words[12], color = colors.muted) }
        item {
            val target = state.target
            Card(colors = CardDefaults.cardColors(containerColor = colors.card)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(words[13], color = colors.text)
                    Text(target?.title ?: words[14], color = colors.text)
                    if (target != null) {
                        target.schedule?.let { Text(it.date, color = colors.muted) }
                        Button(onClick = { if (target.upcoming) onCalendar() else onStart(target.programId, target.dayId, target.schedule?.id) },
                            enabled = !starting && !state.loading, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Text(if (target.upcoming) words[15] else words[16])
                        }
                    }
                }
            }
        }
        item { OutlinedButton(onClick = model::reload, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) { Text(words[17]) } }
        item { OutlinedButton(onClick = onHealth, modifier = Modifier.fillMaxWidth()) { Text(words[18]) } }
        item { OutlinedButton(onClick = onProgress, modifier = Modifier.fillMaxWidth()) { Text(words[19]) } }
        item { Text(words[20], color = colors.muted); Spacer(Modifier.height(96.dp)) }
    }
}

private fun coachWords(language: String) = when(language) {
    "en" -> listOf("Data could not be loaded. Try again.", "Today readiness", "Normal planned load is supported by the available signals.", "Keep the load moderate today; avoid forcing progression.", "Start lighter; recovery or recent load limits the recommendation.", "Not enough recent data. Consider your previous performance and how you feel today.", "Recent pain feedback: do not increase load for the affected exercise.", "Samsung energy /100", "TrainPilot estimate /100", "Sleep (minutes)", "HRV baseline (ms)", "Recent load /100", "Samsung energy and TrainPilot's estimate are separate. Pain and heavy effort can limit readiness. Only recent, identifiable Samsung readings are used; missing values stay empty.", "Workout suggestion", "No available program day.", "Open calendar", "Start workout", "Reload stored data", "Open Health / refresh", "Progress", "Informational estimate, not a medical assessment.")
    "de" -> listOf("Daten konnten nicht geladen werden. Erneut versuchen.", "Heutige Bereitschaft", "Die verfügbaren Signale unterstützen die geplante normale Belastung.", "Heute moderat trainieren und keine Steigerung erzwingen.", "Leichter beginnen; Regeneration oder letzte Belastung begrenzen die Empfehlung.", "Zu wenig aktuelle Daten. Bisherige Leistung und heutiges Befinden berücksichtigen.", "Aktuelle Schmerzmeldung: bei der betroffenen Übung nicht steigern.", "Samsung Energie /100", "TrainPilot Schätzung /100", "Schlaf (Minuten)", "HRV-Basis (ms)", "Letzte Belastung /100", "Samsung Energie und TrainPilot-Schätzung sind getrennt. Schmerz und hohe Belastung können die Bereitschaft begrenzen. Nur aktuelle, zuordenbare Samsung-Werte werden verwendet; fehlende Werte bleiben leer.", "Trainingsvorschlag", "Kein verfügbarer Programmtag.", "Kalender öffnen", "Training starten", "Gespeicherte Daten neu laden", "Gesundheit / Aktualisierung", "Fortschritt", "Orientierende Schätzung, keine medizinische Bewertung.")
    "ro" -> listOf("Datele nu au putut fi încărcate. Încearcă din nou.", "Pregătire azi", "Semnalele disponibile permit încărcarea normală planificată.", "Menține încărcarea moderată; nu forța progresia.", "Începe mai ușor; recuperarea sau încărcarea recentă limitează recomandarea.", "Nu există suficiente date recente. Ține cont de performanța anterioară și de cum te simți azi.", "Durere raportată recent: nu crește încărcarea exercițiului afectat.", "Energie Samsung /100", "Estimare TrainPilot /100", "Somn (minute)", "Bază HRV (ms)", "Încărcare recentă /100", "Energia Samsung și estimarea TrainPilot sunt separate. Durerea și efortul mare pot limita pregătirea. Sunt folosite doar valori Samsung recente, cu sursă identificabilă; valorile lipsă rămân goale.", "Antrenament sugerat", "Nicio zi de program disponibilă.", "Deschide calendarul", "Începe antrenamentul", "Reîncarcă datele salvate", "Sănătate / actualizare", "Progres", "Estimare orientativă, nu o evaluare medicală.")
    else -> listOf("Az adatok nem tölthetők be. Próbáld újra.", "Mai készenlét", "A rendelkezésre álló jelek alapján mehet a tervezett normál terhelés.", "Ma mérsékelt terhelést tarts; ne erőltesd a progressziót.", "Kezdj könnyebben; a regeneráció vagy a legutóbbi terhelés korlátozza a javaslatot.", "Nincs elég friss adat. A korábbi teljesítményt és a mai közérzetedet vedd alapul.", "Friss fájdalomjelzés: az érintett gyakorlatnál ne emelj terhelést.", "Samsung energia /100", "TrainPilot becslés /100", "Alvás (perc)", "HRV alapérték (ms)", "Legutóbbi terhelés /100", "A Samsung-pontszám és a TrainPilot becslése külön számítás. Fájdalom és nagy terhelés korlátozhatja a készenlétet. Csak friss, azonosítható Samsung-mérést használunk; a hiányzó értékek üresen maradnak.", "Edzésjavaslat", "Nincs elérhető programnap.", "Naptár megnyitása", "Edzés indítása", "Tárolt adatok újraolvasása", "Egészség / frissítés megnyitása", "Fejlődés", "Tájékoztató becslés; nem orvosi értékelés.")
}
