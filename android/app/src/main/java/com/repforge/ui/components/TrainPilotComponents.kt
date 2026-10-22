package com.repforge.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.repforge.*
import com.repforge.ui.theme.*

/**
 * v1.3.71: Egységes tematikus haptikus jelzésű Checkbox.
 */
@Composable
fun ThemedHapticCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val view = LocalView.current
    Checkbox(
        checked = checked,
        onCheckedChange = { isChecked ->
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            onCheckedChange(isChecked)
        },
        modifier = modifier,
        enabled = enabled,
        colors = CheckboxDefaults.colors(
            checkedColor = MaterialTheme.colorScheme.secondary,
            uncheckedColor = TextSecondary,
            checkmarkColor = Color.White
        )
    )
}

/**
 * v1.3.71: Laposabb összecsukott progressziós sáv és kiterjeszthető panel.
 */
@Composable
fun CollapsibleProgressionPanel(
    isExpanded: Boolean,
    style: EngineTrainingStyle,
    progression: EngineProgressionModel,
    advice: ProgressionAdvice?,
    onToggleExpand: () -> Unit,
    onSelectStyle: (EngineTrainingStyle) -> Unit,
    onSelectProgression: (EngineProgressionModel) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onToggleExpand() },
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Lapos összefoglaló sáv (Flatter collapsed design)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "${style.name} • ${progression.name.replace("_", " ")}",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = TextSecondary
                )
            }

            // Kiterjesztett szerkesztő nézet
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Edzésmódszer:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        EngineTrainingStyle.values().forEach { s ->
                            FilterChip(
                                selected = style == s,
                                onClick = { onSelectStyle(s) },
                                label = { Text(s.name) }
                            )
                        }
                    }

                    Text("Progressziós Modell:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EngineProgressionModel.values().forEach { p ->
                            FilterChip(
                                selected = progression == p,
                                onClick = { onSelectProgression(p) },
                                label = { Text(p.name.take(6)) }
                            )
                        }
                    }

                    advice?.let { adv ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = adv.adviceMessageHu,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Pihenőidő visszaszámláló HUD.
 */
@Composable
fun RestTimerHud(
    restSecondsRemaining: Int,
    onSkip: () -> Unit,
    onAdjust: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Pihenőidő",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            val min = restSecondsRemaining / 60
            val sec = restSecondsRemaining % 60
            Text(
                text = "%02d:%02d".format(min, sec),
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(onClick = { onAdjust(-15) }) {
                    Text("-15 mp")
                }
                Button(
                    onClick = onSkip,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Kihagyás")
                }
                OutlinedButton(onClick = { onAdjust(30) }) {
                    Text("+30 mp")
                }
            }
        }
    }
}

/**
 * Sorozat szerkesztő sor (Prefill értékekkel, diszkrét RIR 0-10 és tematikus haptikus checkbox).
 */
@Composable
fun SetRowItem(
    set: EngineSetRecord,
    onValuesChanged: (Int, Double, Int) -> Unit,
    onComplete: (Int, Double, Int) -> Unit,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier
) {
    var weightInput by remember(set.weightKg) { mutableStateOf(set.weightKg.toString()) }
    var repsInput by remember(set.reps) { mutableStateOf(set.reps.toString()) }
    var rirState by remember(set.actualRir) { mutableStateOf(set.actualRir) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        color = if (set.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Sorozatszám vagy típus jelvény
            Text(
                text = "${set.setIndex}.",
                fontWeight = FontWeight.Bold,
                color = if (set.isCompleted) TextSecondary else MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(28.dp)
            )

            // Súly beviteli mező
            OutlinedTextField(
                value = weightInput,
                onValueChange = {
                    weightInput = it
                    it.toDoubleOrNull()?.let { w -> onValuesChanged(set.reps, w, rirState) }
                },
                label = { Text("kg") },
                modifier = Modifier.width(78.dp),
                singleLine = true
            )

            // Ismétlés beviteli mező
            OutlinedTextField(
                value = repsInput,
                onValueChange = {
                    repsInput = it
                    it.toIntOrNull()?.let { r -> onValuesChanged(r, set.weightKg, rirState) }
                },
                label = { Text("ism") },
                modifier = Modifier.width(68.dp),
                singleLine = true
            )

            // Diszkrét RIR gomb (0..10 ciklikus léptetéssel)
            val rirColor = when (rirState) {
                0 -> PilotError
                1, 2 -> PilotWarning
                3, 4 -> PilotSuccess
                else -> MaterialTheme.colorScheme.primary
            }

            AssistChip(
                onClick = {
                    val nextRir = (rirState + 1) % 6
                    rirState = nextRir
                    val w = weightInput.toDoubleOrNull() ?: set.weightKg
                    val r = repsInput.toIntOrNull() ?: set.reps
                    onValuesChanged(r, w, nextRir)
                },
                label = { Text("RIR $rirState", color = rirColor, fontWeight = FontWeight.Bold) }
            )

            // Tematikus haptikus Checkbox
            ThemedHapticCheckbox(
                checked = set.isCompleted,
                onCheckedChange = { isChecked ->
                    val w = weightInput.toDoubleOrNull() ?: set.weightKg
                    val r = repsInput.toIntOrNull() ?: set.reps
                    if (isChecked) {
                        onComplete(r, w, rirState)
                    } else {
                        onUndo()
                    }
                }
            )
        }
    }
}

/**
 * v1.3.71: Megjavított PR részletek Modál dialógus (Epley, Brzycki, Lombardi kalkulációk).
 */
@Composable
fun PersonalRecordDetailsDialog(
    breakdown: PersonalRecordBreakdown226,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = PilotWarning)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Rekord Részletei")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = breakdown.exerciseName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = breakdown.localizedCategoryName["hu"] ?: breakdown.category.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Divider(color = MaterialTheme.colorScheme.surfaceVariant)

                Text(
                    text = "Elért csúcs: %.1f kg (%d ismétlés)".format(breakdown.newValue, breakdown.reps),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                )

                if (breakdown.oldValue != null && breakdown.oldValue > 0.0) {
                    Text(
                        text = "Fejlődés: +%.1f kg (+%.1f%%)".format(breakdown.deltaKg, breakdown.deltaPercent),
                        color = PilotSuccess,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text("1RM Formula Összehasonlítás (PR #226):", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                breakdown.formulaEstimates.forEach { est ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(est.formulaName, style = MaterialTheme.typography.bodySmall)
                        Text("%.1f kg".format(est.estimated1RmKg), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Bezárás")
            }
        }
    )
}

/**
 * v1.3.71: Visszaállított megerősítő Cancel (Megszakítás) dialógus.
 */
@Composable
fun CancelWorkoutConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edzés Megszakítása?") },
        text = { Text("Biztosan meg akarod szakítani az aktív edzést? A nem mentett sorozatok elvesznek.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Megszakítás")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Edzés Folytatása")
            }
        }
    )
}

/**
 * GPS útvonal és beltéri mérés HUD (#193/#202).
 */
@Composable
fun GpsRouteHud(
    gpsSnapshot: PhoneGpsSnapshot193,
    onToggleIndoor: () -> Unit,
    onCompleteExplicitCardio: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    val distKm = gpsSnapshot.totalDistanceMeters / 1000.0
                    Text(
                        text = "Távolság: %.2f km".format(distKm),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    val paceMin = (gpsSnapshot.currentPaceSecondsPerKm / 60.0).toInt()
                    val paceSec = (gpsSnapshot.currentPaceSecondsPerKm % 60.0).toInt()
                    Text(
                        text = if (gpsSnapshot.isIndoor) "Beltéri Mód (GPS inaktív)" else "Tempó: %d:%02d /km".format(paceMin, paceSec),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (gpsSnapshot.isIndoor) "Beltér" else "Kültér", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.width(6.dp))
                    Switch(
                        checked = gpsSnapshot.isIndoor,
                        onCheckedChange = { onToggleIndoor() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onCompleteExplicitCardio,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Default.DirectionsRun, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Kardió/Intervallum Mérés Lezárása")
            }
        }
    }
}

/**
 * Wear OS Ownership Transition Banner (#193).
 */
@Composable
fun OwnershipHandoffBanner(
    role: PhoneOwnershipRole,
    onHandoffToWatch: () -> Unit,
    onReclaimToPhone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Watch, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (role == PhoneOwnershipRole.PRIMARY) "Telefon Vezérel (Primary)" else "Óra Vezérel (Mirror)",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                )
            }
            if (role == PhoneOwnershipRole.PRIMARY) {
                TextButton(onClick = onHandoffToWatch) {
                    Text("Átadás Órára")
                }
            } else {
                TextButton(onClick = onReclaimToPhone) {
                    Text("Visszavétel")
                }
            }
        }
    }
}
