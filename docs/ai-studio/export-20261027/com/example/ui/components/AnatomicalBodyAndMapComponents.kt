package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalTrainPilotPalette

data class MuscleLoadItem(
    val id: String,
    val nameHu: String,
    val setsText: String,
    val percentage: Int,
    val ratio: Float,
    val levelLabelHu: String,
    val topExercisesHu: List<Pair<String, String>>, // ExerciseName to sets/load
    val exerciseIdToOpen: String
)

/**
 * Compact Side-by-Side Front & Back Anatomical Muscle Heatmap ("Izomcsoport-terhelés")
 * Matches the `main` app screenshot (`Screenshot_2026-10-27-15-00-06-481_com.repforge.app.jpg`):
 * - Left: Dual Front & Back Anatomical Muscular Figures
 * - Right: 6 horizontal progress bars (Mell 45%, Hát 66%, Váll 59%, Láb 75%, Kar 100%, Core 61%)
 * - Bottom: Kisebb / Közepes / Nagyobb legend + explanatory caption
 * - Tapping any muscle bar or period pill expands/updates the details inline!
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnatomicalMuscleHeatmapCard(
    title: String = "Izomcsoport-terhelés",
    subtitle: String = "A színek és arányok az időszak legterheltebb izomcsoportjához viszonyítanak, nem fáradtságot mérnek.",
    highlightedMuscleId: String? = null,
    onOpenExerciseDetail: ((String) -> Unit)? = null
) {
    val palette = LocalTrainPilotPalette.current
    val periodOptions = remember { listOf("3 hó", "30 nap", "7 nap", "1 nap") }
    var selectedPeriodIdx by remember { mutableIntStateOf(0) }

    val muscleLoads = remember(selectedPeriodIdx) {
        val factor = when (selectedPeriodIdx) {
            1 -> 0.85f
            2 -> 0.65f
            3 -> 0.45f
            else -> 1.0f
        }
        listOf(
            MuscleLoadItem(
                id = "chest",
                nameHu = "Mell",
                setsText = "31,2 sorozat",
                percentage = (45 * factor).toInt().coerceAtLeast(15),
                ratio = (0.45f * factor).coerceIn(0.15f, 1f),
                levelLabelHu = "Közepes terhelés",
                topExercisesHu = listOf(
                    "Fekvenyomás" to "19,2 sorozat • PR 100 kg",
                    "Fekvőtámasz" to "12 sorozat • PR 15 ism."
                ),
                exerciseIdToOpen = "bench_press"
            ),
            MuscleLoadItem(
                id = "back",
                nameHu = "Hát",
                setsText = "92,2 sorozat",
                percentage = (66 * factor).toInt().coerceAtLeast(25),
                ratio = (0.66f * factor).coerceIn(0.25f, 1f),
                levelLabelHu = "Közepes–magas terhelés",
                topExercisesHu = listOf(
                    "Döntött törzsű evezés – kétkezes rúddal" to "42 sorozat • PR 65 kg",
                    "Húzódzkodás" to "28 sorozat • Saját testsúly",
                    "Evezés gumiszalaggal" to "22,2 sorozat • 14 kg"
                ),
                exerciseIdToOpen = "barbell_row"
            ),
            MuscleLoadItem(
                id = "shoulders",
                nameHu = "Váll",
                setsText = "46,5 sorozat",
                percentage = (59 * factor).toInt().coerceAtLeast(20),
                ratio = (0.59f * factor).coerceIn(0.20f, 1f),
                levelLabelHu = "Közepes terhelés",
                topExercisesHu = listOf(
                    "Vállból nyomás" to "28,5 sorozat • 42,5 kg",
                    "Oldalemelés" to "18 sorozat • 10 kg"
                ),
                exerciseIdToOpen = "bench_press"
            ),
            MuscleLoadItem(
                id = "legs",
                nameHu = "Láb",
                setsText = "82,9 sorozat",
                percentage = (75 * factor).toInt().coerceAtLeast(35),
                ratio = (0.75f * factor).coerceIn(0.35f, 1f),
                levelLabelHu = "Nagyobb terhelés",
                topExercisesHu = listOf(
                    "Goblet guggolás" to "38 sorozat • PR 16 kg × 12",
                    "Bolgár guggolás" to "26,9 sorozat • PR 15 ism.",
                    "Kitörés" to "18 sorozat • Saját testsúly / Súlyzó"
                ),
                exerciseIdToOpen = "goblet_squat"
            ),
            MuscleLoadItem(
                id = "arms",
                nameHu = "Kar",
                setsText = "45,7 sorozat",
                percentage = if (selectedPeriodIdx == 0) 100 else (90 * factor).toInt().coerceAtLeast(45),
                ratio = if (selectedPeriodIdx == 0) 1.0f else (0.90f * factor).coerceIn(0.45f, 1f),
                levelLabelHu = "Legterheltebb izomcsoport (100%)",
                topExercisesHu = listOf(
                    "Tricepsz fej fölött" to "24,7 sorozat • PR 5,5 kg",
                    "Bicepsz hajlítás" to "21 sorozat • 12 kg"
                ),
                exerciseIdToOpen = "push_up"
            ),
            MuscleLoadItem(
                id = "core",
                nameHu = "Core",
                setsText = "18,5 sorozat",
                percentage = (61 * factor).toInt().coerceAtLeast(20),
                ratio = (0.61f * factor).coerceIn(0.20f, 1f),
                levelLabelHu = "Közepes terhelés",
                topExercisesHu = listOf(
                    "Plank" to "11,5 sorozat • 45 mp",
                    "Oldalsó plank" to "7 sorozat • 25 mp / oldal"
                ),
                exerciseIdToOpen = "plank"
            )
        )
    }

    var selectedMuscleId by remember(highlightedMuscleId) {
        mutableStateOf(highlightedMuscleId ?: "all")
    }

    val selectedMuscleObj = muscleLoads.find { it.id == selectedMuscleId }

    val smallHeatColor = Color(0xFFE5B842)   // Yellow-Gold (Kisebb)
    val mediumHeatColor = Color(0xFFE68A2E)  // Warm Orange (Közepes)
    val highHeatColor = Color(0xFFE25555)    // Coral Red (Nagyobb)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = palette.surfaceCard,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, palette.cardBorder, RoundedCornerShape(20.dp))
            .testTag("anatomical_muscle_heatmap_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header row: "Izomcsoport-terhelés" + "3 hó" pill button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = palette.surfaceElevated,
                    modifier = Modifier
                        .border(1.dp, palette.cardBorder, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            selectedPeriodIdx = (selectedPeriodIdx + 1) % periodOptions.size
                        }
                        .testTag("muscle_heatmap_period_pill")
                ) {
                    Text(
                        text = periodOptions[selectedPeriodIdx],
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Side-by-Side Row: Left = Front & Back Anatomical Figures, Right = 6 Muscle Progress Bars
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Left half: Dual Front & Back Anatomical Muscular Figures
                val baseSilhouette = Color(0xFF434A56)
                val outlineCol = Color(0xFF9EA7B8).copy(alpha = 0.55f)

                fun colorForMuscle(id: String, defaultHeat: Color): Color {
                    return if (selectedMuscleId == "all") {
                        defaultHeat
                    } else if (selectedMuscleId == id) {
                        highHeatColor
                    } else {
                        baseSilhouette.copy(alpha = 0.55f)
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(0.45f)
                        .height(156.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            // Cycle through muscles or reset to all on body tap
                            val ids = listOf("all", "arms", "legs", "back", "shoulders", "core", "chest")
                            val nextIdx = (ids.indexOf(selectedMuscleId) + 1) % ids.size
                            selectedMuscleId = ids[nextIdx]
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        drawAnatomicalFigure(
                            cx = w * 0.27f,
                            h = h,
                            isFront = true,
                            baseColor = baseSilhouette,
                            outlineColor = outlineCol,
                            chestColor = colorForMuscle("chest", mediumHeatColor),
                            shoulderColor = colorForMuscle("shoulders", mediumHeatColor),
                            armColor = colorForMuscle("arms", highHeatColor),
                            coreColor = colorForMuscle("core", mediumHeatColor),
                            legColor = colorForMuscle("legs", highHeatColor),
                            backColor = colorForMuscle("back", mediumHeatColor)
                        )

                        drawAnatomicalFigure(
                            cx = w * 0.75f,
                            h = h,
                            isFront = false,
                            baseColor = baseSilhouette,
                            outlineColor = outlineCol,
                            chestColor = colorForMuscle("chest", mediumHeatColor),
                            shoulderColor = colorForMuscle("shoulders", mediumHeatColor),
                            armColor = colorForMuscle("arms", highHeatColor),
                            coreColor = colorForMuscle("core", mediumHeatColor),
                            legColor = colorForMuscle("legs", highHeatColor),
                            backColor = colorForMuscle("back", mediumHeatColor)
                        )
                    }
                }

                // Right half: 6 Compact Muscle Progress Bars (Mell, Hát, Váll, Láb, Kar, Core)
                Column(
                    modifier = Modifier.weight(0.55f),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    muscleLoads.forEach { m ->
                        val isSel = selectedMuscleId == m.id
                        val barColor = when {
                            m.percentage >= 70 -> highHeatColor
                            m.percentage >= 40 -> mediumHeatColor
                            else -> smallHeatColor
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    selectedMuscleId = if (selectedMuscleId == m.id) "all" else m.id
                                }
                                .padding(vertical = 1.dp)
                                .testTag("muscle_chip_${m.id}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = m.nameHu,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) palette.primary else palette.textPrimary,
                                modifier = Modifier.width(34.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(palette.surfaceElevated)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(m.ratio)
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(barColor)
                                )
                            }
                            Text(
                                text = "${m.percentage}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) palette.primary else palette.textSecondary,
                                modifier = Modifier.width(34.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.End
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Legend Row: 🟡 Kisebb   🟠 Közepes   🔴 Nagyobb
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendDot(color = smallHeatColor, label = "Kisebb")
                Spacer(modifier = Modifier.width(14.dp))
                LegendDot(color = mediumHeatColor, label = "Közepes")
                Spacer(modifier = Modifier.width(14.dp))
                LegendDot(color = highHeatColor, label = "Nagyobb")
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary
            )

            // Expandable Muscle Exercise Breakdown when user taps any muscle bar or body part
            if (selectedMuscleObj != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = palette.surfaceElevated,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, palette.primary.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .clickable { selectedMuscleId = "all" }
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${selectedMuscleObj.nameHu} • ${selectedMuscleObj.setsText} (${selectedMuscleObj.percentage}%)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = palette.primary
                            )
                            if (onOpenExerciseDetail != null) {
                                Text(
                                    text = "Gyakorlat >",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.primary,
                                    modifier = Modifier.clickable {
                                        onOpenExerciseDetail(selectedMuscleObj.exerciseIdToOpen)
                                    }
                                )
                            } else {
                                Text(
                                    text = "Bezárás ✕",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                            }
                        }
                        HorizontalDivider(color = palette.cardBorder)
                        selectedMuscleObj.topExercisesHu.forEach { (exName, exLoad) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "• $exName",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = exLoad,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    val palette = LocalTrainPilotPalette.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = palette.textSecondary
        )
    }
}

private fun DrawScope.drawAnatomicalFigure(
    cx: Float,
    h: Float,
    isFront: Boolean,
    baseColor: Color,
    outlineColor: Color,
    chestColor: Color,
    shoulderColor: Color,
    armColor: Color,
    coreColor: Color,
    legColor: Color,
    backColor: Color
) {
    val stroke = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

    // 1. Head & Neck
    drawCircle(
        color = baseColor,
        radius = h * 0.065f,
        center = Offset(cx, h * 0.10f)
    )
    drawCircle(
        color = outlineColor,
        radius = h * 0.065f,
        center = Offset(cx, h * 0.10f),
        style = stroke
    )
    // Neck / Upper Traps
    drawRoundRect(
        color = if (isFront) baseColor else backColor,
        topLeft = Offset(cx - h * 0.035f, h * 0.16f),
        size = Size(h * 0.07f, h * 0.04f),
        cornerRadius = CornerRadius(6f, 6f)
    )

    // 2. Shoulders (Deltoids Left & Right)
    drawOval(
        color = shoulderColor,
        topLeft = Offset(cx - h * 0.175f, h * 0.19f),
        size = Size(h * 0.075f, h * 0.085f)
    )
    drawOval(
        color = shoulderColor,
        topLeft = Offset(cx + h * 0.10f, h * 0.19f),
        size = Size(h * 0.075f, h * 0.085f)
    )

    if (isFront) {
        // 3A. Front Chest (Pectorals)
        drawRoundRect(
            color = chestColor,
            topLeft = Offset(cx - h * 0.11f, h * 0.20f),
            size = Size(h * 0.102f, h * 0.095f),
            cornerRadius = CornerRadius(12f, 12f)
        )
        drawRoundRect(
            color = chestColor,
            topLeft = Offset(cx + h * 0.008f, h * 0.20f),
            size = Size(h * 0.102f, h * 0.095f),
            cornerRadius = CornerRadius(12f, 12f)
        )

        // 4A. Front Core / Abs (6-pack blocks)
        drawRoundRect(
            color = coreColor,
            topLeft = Offset(cx - h * 0.085f, h * 0.305f),
            size = Size(h * 0.17f, h * 0.165f),
            cornerRadius = CornerRadius(10f, 10f)
        )
        // Center linea alba & transverse lines
        drawLine(
            color = outlineColor,
            start = Offset(cx, h * 0.31f),
            end = Offset(cx, h * 0.46f),
            strokeWidth = 1.5.dp.toPx()
        )
    } else {
        // 3B. Back Lats & Rhomboids (V-Taper)
        val backPath = Path().apply {
            moveTo(cx - h * 0.115f, h * 0.20f)
            lineTo(cx + h * 0.115f, h * 0.20f)
            lineTo(cx + h * 0.08f, h * 0.42f)
            lineTo(cx - h * 0.08f, h * 0.42f)
            close()
        }
        drawPath(path = backPath, color = backColor)
        drawPath(path = backPath, color = outlineColor, style = stroke)

        // 4B. Glutes (Farizom - part of Legs/Posterior chain)
        drawRoundRect(
            color = legColor,
            topLeft = Offset(cx - h * 0.095f, h * 0.43f),
            size = Size(h * 0.09f, h * 0.085f),
            cornerRadius = CornerRadius(14f, 14f)
        )
        drawRoundRect(
            color = legColor,
            topLeft = Offset(cx + h * 0.005f, h * 0.43f),
            size = Size(h * 0.09f, h * 0.085f),
            cornerRadius = CornerRadius(14f, 14f)
        )
    }

    // 5. Arms (Biceps/Triceps + Forearms)
    // Left Upper Arm
    drawRoundRect(
        color = armColor,
        topLeft = Offset(cx - h * 0.185f, h * 0.275f),
        size = Size(h * 0.06f, h * 0.11f),
        cornerRadius = CornerRadius(10f, 10f)
    )
    // Right Upper Arm
    drawRoundRect(
        color = armColor,
        topLeft = Offset(cx + h * 0.125f, h * 0.275f),
        size = Size(h * 0.06f, h * 0.11f),
        cornerRadius = CornerRadius(10f, 10f)
    )
    // Left Forearm
    drawRoundRect(
        color = armColor.copy(alpha = 0.8f),
        topLeft = Offset(cx - h * 0.20f, h * 0.395f),
        size = Size(h * 0.052f, h * 0.11f),
        cornerRadius = CornerRadius(8f, 8f)
    )
    // Right Forearm
    drawRoundRect(
        color = armColor.copy(alpha = 0.8f),
        topLeft = Offset(cx + h * 0.148f, h * 0.395f),
        size = Size(h * 0.052f, h * 0.11f),
        cornerRadius = CornerRadius(8f, 8f)
    )

    // 6. Upper Legs (Quads / Hamstrings)
    drawRoundRect(
        color = legColor,
        topLeft = Offset(cx - h * 0.098f, h * 0.515f),
        size = Size(h * 0.086f, h * 0.205f),
        cornerRadius = CornerRadius(14f, 14f)
    )
    drawRoundRect(
        color = legColor,
        topLeft = Offset(cx + h * 0.012f, h * 0.515f),
        size = Size(h * 0.086f, h * 0.205f),
        cornerRadius = CornerRadius(14f, 14f)
    )

    // 7. Lower Legs (Calves / Vádli)
    drawRoundRect(
        color = legColor.copy(alpha = 0.85f),
        topLeft = Offset(cx - h * 0.09f, h * 0.735f),
        size = Size(h * 0.072f, h * 0.19f),
        cornerRadius = CornerRadius(12f, 12f)
    )
    drawRoundRect(
        color = legColor.copy(alpha = 0.85f),
        topLeft = Offset(cx + h * 0.018f, h * 0.735f),
        size = Size(h * 0.072f, h * 0.19f),
        cornerRadius = CornerRadius(12f, 12f)
    )
}

/**
 * High-detail Margitsziget / Danube / Margit híd GPS Route Map Canvas
 * Matching Mockup 3/4 #3 (`Edzésútvonal`) & Mockup 2/4 #4 (`Kültéri mérés`).
 */
@Composable
fun MargitszigetDetailedMapBox(
    modifier: Modifier = Modifier,
    interactiveControls: Boolean = true,
    selectedKmSplit: Int = 0,
    onSelectKmSplit: ((Int) -> Unit)? = null
) {
    val palette = LocalTrainPilotPalette.current
    var zoomScale by remember { mutableFloatStateOf(1.0f) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF0E1A24))
            .border(1.dp, palette.cardBorder, RoundedCornerShape(24.dp))
    ) {
        val routeGreen = Color(0xFF22C55E)
        val bridgeYellow = Color(0xFFEAB308)

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Base urban land (Vizafogó on top-left, Újlipótváros on right)
            drawRect(color = Color(0xFF1A242D), size = size)

            // 2. Danube River branches around Margaret Island (Duna)
            val leftRiver = Path().apply {
                moveTo(w * 0.42f, 0f)
                lineTo(w * 0.68f, 0f)
                lineTo(w * 0.24f, h)
                lineTo(0f, h)
                lineTo(0f, h * 0.62f)
                close()
            }
            drawPath(leftRiver, Color(0xFF1B3B52))

            val rightRiver = Path().apply {
                moveTo(w * 0.82f, 0f)
                lineTo(w, 0f)
                lineTo(w, h * 0.35f)
                lineTo(w * 0.56f, h)
                lineTo(w * 0.30f, h)
                close()
            }
            drawPath(rightRiver, Color(0xFF1B3B52))

            // 3. Margaret Island (Margitsziget) diagonal park landmass
            val island = Path().apply {
                moveTo(w * 0.72f, h * 0.08f)
                cubicTo(w * 0.84f, h * 0.14f, w * 0.80f, h * 0.35f, w * 0.62f, h * 0.65f)
                cubicTo(w * 0.48f, h * 0.85f, w * 0.36f, h * 0.88f, w * 0.27f, h * 0.82f)
                cubicTo(w * 0.22f, h * 0.72f, w * 0.34f, h * 0.45f, w * 0.52f, h * 0.22f)
                close()
            }
            drawPath(island, Color(0xFF233931))

            // Inner park lawns & paths
            val innerPark = Path().apply {
                moveTo(w * 0.66f, h * 0.16f)
                lineTo(w * 0.72f, h * 0.26f)
                lineTo(w * 0.46f, h * 0.72f)
                lineTo(w * 0.34f, h * 0.66f)
                close()
            }
            drawPath(innerPark, Color(0xFF2B473C))

            // 4. Urban street grids (Vizafogó top-left & Újlipótváros right)
            val streetStroke = Stroke(width = 1.2.dp.toPx())
            for (i in 1..6) {
                // Top-left streets
                drawLine(
                    color = Color.White.copy(alpha = 0.09f),
                    start = Offset(0f, h * 0.06f * i),
                    end = Offset(w * 0.42f, h * 0.11f * i),
                    strokeWidth = streetStroke.width
                )
                // Right-side Újlipótváros grid
                drawLine(
                    color = Color.White.copy(alpha = 0.10f),
                    start = Offset(w * 0.66f, h * (0.28f + i * 0.10f)),
                    end = Offset(w, h * (0.38f + i * 0.10f)),
                    strokeWidth = streetStroke.width
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.08f),
                    start = Offset(w * (0.62f + i * 0.06f), h * 0.25f),
                    end = Offset(w * (0.48f + i * 0.07f), h),
                    strokeWidth = streetStroke.width
                )
            }

            // 5. Margit híd (Margaret Bridge at bottom, angled with bend at island tip)
            val bridgePath = Path().apply {
                moveTo(0f, h * 0.78f)
                lineTo(w * 0.32f, h * 0.85f)
                lineTo(w * 0.68f, h * 0.96f)
            }
            drawPath(
                path = bridgePath,
                color = bridgeYellow.copy(alpha = 0.8f),
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // 6. Glowing GPS Running Track on Margitsziget (matching Mockup 3/4 #3)
            val gpsRoute = Path().apply {
                moveTo(w * 0.37f, h * 0.73f)
                cubicTo(
                    w * 0.42f, h * 0.68f,
                    w * 0.50f, h * 0.71f,
                    w * 0.56f, h * 0.62f
                )
                cubicTo(
                    w * 0.63f, h * 0.50f,
                    w * 0.68f, h * 0.36f,
                    w * 0.74f, h * 0.22f
                )
            }
            // Outer glow
            drawPath(
                path = gpsRoute,
                color = routeGreen.copy(alpha = 0.28f),
                style = Stroke(width = (11.dp * zoomScale).toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            // Main GPS line
            drawPath(
                path = gpsRoute,
                color = routeGreen,
                style = Stroke(width = (4.5.dp * zoomScale).toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Intermediate 1 km marker dot
            drawCircle(
                color = Color.White,
                radius = 5.dp.toPx(),
                center = Offset(w * 0.60f, h * 0.54f)
            )
            drawCircle(
                color = routeGreen,
                radius = 3.dp.toPx(),
                center = Offset(w * 0.60f, h * 0.54f)
            )

            // Start Pin (South-west green circle with white ring)
            val startCenter = Offset(w * 0.37f, h * 0.73f)
            drawCircle(color = Color.White, radius = 9.dp.toPx(), center = startCenter)
            drawCircle(color = routeGreen, radius = 6.dp.toPx(), center = startCenter)

            // Finish Pin (North-east checkered target circle)
            val endCenter = Offset(w * 0.74f, h * 0.22f)
            drawCircle(color = Color.White, radius = 10.dp.toPx(), center = endCenter)
            drawCircle(color = Color(0xFF111827), radius = 7.dp.toPx(), center = endCenter)
            drawCircle(color = Color.White, radius = 3.5.dp.toPx(), center = endCenter)
        }

        // Geographic Labels matching Mockup 3/4 #3
        Text(
            text = "Vizafogó",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.75f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 18.dp, top = 22.dp)
        )
        Text(
            text = "Újlipótváros",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.80f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 18.dp, top = 46.dp)
        )
        Text(
            text = "Margitsziget",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(end = 28.dp, bottom = 24.dp)
        )
        Text(
            text = "Duna",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF93C5FD),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 68.dp, top = 24.dp)
        )
        Text(
            text = "Margit\nhíd",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.88f),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 76.dp, bottom = 22.dp)
        )

        if (interactiveControls) {
            // Zoom & Recenter floating controls (bottom-right matching Mockup 3/4 #3)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1F2937).copy(alpha = 0.9f),
                    modifier = Modifier
                        .size(40.dp)
                        .border(1.dp, palette.cardBorder, CircleShape)
                        .clip(CircleShape)
                        .clickable { zoomScale = (zoomScale + 0.2f).coerceAtMost(1.6f) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Zoom in", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1F2937).copy(alpha = 0.9f),
                    modifier = Modifier
                        .size(40.dp)
                        .border(1.dp, palette.cardBorder, CircleShape)
                        .clip(CircleShape)
                        .clickable { zoomScale = (zoomScale - 0.2f).coerceAtLeast(0.8f) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "Zoom out", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1F2937).copy(alpha = 0.9f),
                    modifier = Modifier
                        .size(44.dp)
                        .border(1.dp, palette.primary.copy(alpha = 0.6f), CircleShape)
                        .clip(CircleShape)
                        .clickable { zoomScale = 1.0f }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.MyLocation, contentDescription = "Recenter", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

/**
 * Custom Exercise Posture & Target Muscle Mini-Illustration (Mockup 1/4 #5 & #6, Mockup 2/4 #1..#3, Mockup 3/4 #2).
 * Depicts the exact exercise body position (Squat, Push-up/Bench, Row/Pull, Lunge, Plank, Overhead Press, Arm Curl)
 * with highlighted active muscles and motion direction arrow.
 */
@Composable
fun ExercisePostureMiniIllustration(
    exerciseNameOrId: String,
    modifier: Modifier = Modifier.size(54.dp)
) {
    val palette = LocalTrainPilotPalette.current
    val lower = exerciseNameOrId.lowercase()

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = palette.surfaceElevated,
        modifier = modifier.border(1.dp, palette.cardBorder, RoundedCornerShape(14.dp))
    ) {
        val primaryGold = palette.primary
        val muscleOrange = Color(0xFFF97316)
        val bodySlate = Color(0xFFCBD5E1)
        val headSlate = Color(0xFF94A3B8)

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val strokeBody = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            val strokeMuscle = Stroke(width = 4.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

            when {
                // 1. SQUAT / GOBLET SQUAT / GUGGOLÁS (Squatting posture + highlighted quads/glutes)
                lower.contains("guggol") || lower.contains("squat") || lower.contains("lábnyom") -> {
                    // Head
                    drawCircle(color = headSlate, radius = w * 0.09f, center = Offset(w * 0.48f, h * 0.22f))
                    // Torso leaning slightly forward
                    drawLine(
                        color = bodySlate,
                        start = Offset(w * 0.48f, h * 0.31f),
                        end = Offset(w * 0.36f, h * 0.56f),
                        strokeWidth = strokeBody.width,
                        cap = StrokeCap.Round
                    )
                    // Highlighted Quads & Glutes (horizontal thigh in squat position)
                    drawLine(
                        color = muscleOrange,
                        start = Offset(w * 0.36f, h * 0.56f),
                        end = Offset(w * 0.62f, h * 0.58f),
                        strokeWidth = strokeMuscle.width,
                        cap = StrokeCap.Round
                    )
                    // Lower leg (shin)
                    drawLine(
                        color = bodySlate,
                        start = Offset(w * 0.62f, h * 0.58f),
                        end = Offset(w * 0.56f, h * 0.84f),
                        strokeWidth = strokeBody.width,
                        cap = StrokeCap.Round
                    )
                    // Arms holding Kettlebell / Dumbbell at chest (Goblet)
                    drawLine(
                        color = bodySlate,
                        start = Offset(w * 0.46f, h * 0.36f),
                        end = Offset(w * 0.64f, h * 0.42f),
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawCircle(color = primaryGold, radius = w * 0.08f, center = Offset(w * 0.68f, h * 0.42f))
                    // Up/down motion arrow on left
                    drawLine(
                        color = primaryGold,
                        start = Offset(w * 0.18f, h * 0.28f),
                        end = Offset(w * 0.18f, h * 0.72f),
                        strokeWidth = 1.6.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // 2. PUSH-UP / BENCH PRESS / FEKVŐTÁMASZ / FEKVENYOMÁS (Horizontal push posture + chest/triceps highlight)
                lower.contains("fekvő") || lower.contains("fekvenyom") || lower.contains("push") || lower.contains("bench") || lower.contains("mell") -> {
                    // Floor / Bench reference line
                    drawLine(
                        color = headSlate.copy(alpha = 0.35f),
                        start = Offset(w * 0.14f, h * 0.76f),
                        end = Offset(w * 0.86f, h * 0.76f),
                        strokeWidth = 1.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    // Head
                    drawCircle(color = headSlate, radius = w * 0.085f, center = Offset(w * 0.76f, h * 0.40f))
                    // Inclined Push-up Body line (shoulders to feet)
                    drawLine(
                        color = bodySlate,
                        start = Offset(w * 0.68f, h * 0.44f),
                        end = Offset(w * 0.18f, h * 0.72f),
                        strokeWidth = strokeBody.width,
                        cap = StrokeCap.Round
                    )
                    // Highlighted Chest & Anterior Shoulder
                    drawCircle(color = muscleOrange, radius = w * 0.095f, center = Offset(w * 0.63f, h * 0.47f))
                    // Pushing arm to floor
                    drawLine(
                        color = muscleOrange,
                        start = Offset(w * 0.65f, h * 0.47f),
                        end = Offset(w * 0.66f, h * 0.75f),
                        strokeWidth = strokeBody.width,
                        cap = StrokeCap.Round
                    )
                }

                // 3. ROW / PULL / EVEZÉS / HÚZÓDZKODÁS / FELHÚZÁS (Hinged back posture + lats/upper back highlight)
                lower.contains("evez") || lower.contains("row") || lower.contains("hát") || lower.contains("húzó") || lower.contains("felhúz") || lower.contains("deadlift") -> {
                    // Head
                    drawCircle(color = headSlate, radius = w * 0.085f, center = Offset(w * 0.66f, h * 0.26f))
                    // Hinged Torso (Highlighted Back / Lats)
                    drawLine(
                        color = muscleOrange,
                        start = Offset(w * 0.60f, h * 0.33f),
                        end = Offset(w * 0.32f, h * 0.54f),
                        strokeWidth = strokeMuscle.width,
                        cap = StrokeCap.Round
                    )
                    // Bent Legs
                    drawLine(
                        color = bodySlate,
                        start = Offset(w * 0.32f, h * 0.54f),
                        end = Offset(w * 0.42f, h * 0.82f),
                        strokeWidth = strokeBody.width,
                        cap = StrokeCap.Round
                    )
                    // Pulling arm + band/dumbbell
                    drawLine(
                        color = primaryGold,
                        start = Offset(w * 0.54f, h * 0.38f),
                        end = Offset(w * 0.76f, h * 0.56f),
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawCircle(color = primaryGold, radius = w * 0.065f, center = Offset(w * 0.76f, h * 0.58f))
                }

                // 4. LUNGE / KITÖRÉS / SPLIT SQUAT (Split-stance lunge posture)
                lower.contains("kitör") || lower.contains("lunge") || lower.contains("bolgár") -> {
                    // Head
                    drawCircle(color = headSlate, radius = w * 0.085f, center = Offset(w * 0.50f, h * 0.20f))
                    // Upright Torso
                    drawLine(
                        color = bodySlate,
                        start = Offset(w * 0.50f, h * 0.29f),
                        end = Offset(w * 0.50f, h * 0.54f),
                        strokeWidth = strokeBody.width,
                        cap = StrokeCap.Round
                    )
                    // Front Lunge Leg (Highlighted Quad/Glute)
                    drawLine(
                        color = muscleOrange,
                        start = Offset(w * 0.50f, h * 0.54f),
                        end = Offset(w * 0.72f, h * 0.56f),
                        strokeWidth = strokeMuscle.width,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = bodySlate,
                        start = Offset(w * 0.72f, h * 0.56f),
                        end = Offset(w * 0.72f, h * 0.82f),
                        strokeWidth = strokeBody.width,
                        cap = StrokeCap.Round
                    )
                    // Rear Knee-down Leg
                    drawLine(
                        color = muscleOrange.copy(alpha = 0.85f),
                        start = Offset(w * 0.50f, h * 0.54f),
                        end = Offset(w * 0.34f, h * 0.78f),
                        strokeWidth = strokeBody.width,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = bodySlate,
                        start = Offset(w * 0.34f, h * 0.78f),
                        end = Offset(w * 0.18f, h * 0.80f),
                        strokeWidth = strokeBody.width,
                        cap = StrokeCap.Round
                    )
                }

                // 5. PLANK / OLDALSÓ PLANK / CORE / HASPRÉS (Core stability posture + glowing abs/obliques)
                lower.contains("plank") || lower.contains("core") || lower.contains("has") || lower.contains("dead bug") -> {
                    // Floor line
                    drawLine(
                        color = headSlate.copy(alpha = 0.35f),
                        start = Offset(w * 0.12f, h * 0.76f),
                        end = Offset(w * 0.88f, h * 0.76f),
                        strokeWidth = 1.5.dp.toPx()
                    )
                    // Head
                    drawCircle(color = headSlate, radius = w * 0.08f, center = Offset(w * 0.78f, h * 0.48f))
                    // Forearm support on floor
                    drawLine(
                        color = bodySlate,
                        start = Offset(w * 0.68f, h * 0.52f),
                        end = Offset(w * 0.68f, h * 0.74f),
                        strokeWidth = 2.8.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = bodySlate,
                        start = Offset(w * 0.68f, h * 0.74f),
                        end = Offset(w * 0.80f, h * 0.74f),
                        strokeWidth = 2.8.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    // Highlighted Core / Torso bridge
                    drawLine(
                        color = muscleOrange,
                        start = Offset(w * 0.68f, h * 0.52f),
                        end = Offset(w * 0.42f, h * 0.58f),
                        strokeWidth = strokeMuscle.width,
                        cap = StrokeCap.Round
                    )
                    // Legs to toes
                    drawLine(
                        color = bodySlate,
                        start = Offset(w * 0.42f, h * 0.58f),
                        end = Offset(w * 0.16f, h * 0.74f),
                        strokeWidth = strokeBody.width,
                        cap = StrokeCap.Round
                    )
                }

                // 6. OVERHEAD / SHOULDER / ARM PRESS (Standing overhead press / curl posture)
                else -> {
                    // Head
                    drawCircle(color = headSlate, radius = w * 0.085f, center = Offset(w * 0.50f, h * 0.30f))
                    // Upright Torso
                    drawLine(
                        color = bodySlate,
                        start = Offset(w * 0.50f, h * 0.39f),
                        end = Offset(w * 0.50f, h * 0.64f),
                        strokeWidth = strokeBody.width,
                        cap = StrokeCap.Round
                    )
                    // Highlighted Shoulders & Raised Arms
                    drawCircle(color = muscleOrange, radius = w * 0.075f, center = Offset(w * 0.36f, h * 0.42f))
                    drawCircle(color = muscleOrange, radius = w * 0.075f, center = Offset(w * 0.64f, h * 0.42f))
                    drawLine(
                        color = muscleOrange,
                        start = Offset(w * 0.34f, h * 0.42f),
                        end = Offset(w * 0.28f, h * 0.20f),
                        strokeWidth = 2.8.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = muscleOrange,
                        start = Offset(w * 0.66f, h * 0.42f),
                        end = Offset(w * 0.72f, h * 0.20f),
                        strokeWidth = 2.8.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    // Overhead barbell / dumbbells
                    drawLine(
                        color = primaryGold,
                        start = Offset(w * 0.18f, h * 0.18f),
                        end = Offset(w * 0.82f, h * 0.18f),
                        strokeWidth = 2.6.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    // Legs
                    drawLine(
                        color = bodySlate,
                        start = Offset(w * 0.46f, h * 0.64f),
                        end = Offset(w * 0.40f, h * 0.86f),
                        strokeWidth = 2.8.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = bodySlate,
                        start = Offset(w * 0.54f, h * 0.64f),
                        end = Offset(w * 0.60f, h * 0.86f),
                        strokeWidth = 2.8.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

/**
 * Custom Program Category Emblem Icon matching Mockup 1/4 #3 (`Programok`):
 * - Home + Dumbbell for "Otthoni" programs
 * - Bodyweight / Calisthenics athlete for "Saját testsúlyos" / "Kezdő" programs
 * - Heavy Barbell with plates for "Felső / Alsó" / "Súlyzós" / "Erő" programs
 */
@Composable
fun ProgramTypeEmblemIcon(
    programNameOrId: String,
    isHighlighted: Boolean = false,
    modifier: Modifier = Modifier.size(50.dp)
) {
    val palette = LocalTrainPilotPalette.current
    val lower = programNameOrId.lowercase()
    val accent = if (isHighlighted) palette.primary else palette.textPrimary

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isHighlighted) palette.primary.copy(alpha = 0.16f) else palette.surfaceElevated,
        modifier = modifier.border(
            1.dp,
            if (isHighlighted) palette.primary.copy(alpha = 0.5f) else palette.cardBorder,
            RoundedCornerShape(14.dp)
        )
    ) {
        val goldColor = palette.primary
        val secondaryColor = palette.secondary
        val greenColor = palette.success
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            when {
                // 1. OTTHONI (House Roof + Dumbbell inside)
                lower.contains("otthon") || lower.contains("home") || lower.contains("alap") -> {
                    val roof = Path().apply {
                        moveTo(w * 0.18f, h * 0.46f)
                        lineTo(w * 0.50f, h * 0.20f)
                        lineTo(w * 0.82f, h * 0.46f)
                    }
                    drawPath(
                        path = roof,
                        color = goldColor,
                        style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    // Dumbbell bar & plates inside house
                    drawLine(
                        color = accent,
                        start = Offset(w * 0.30f, h * 0.64f),
                        end = Offset(w * 0.70f, h * 0.64f),
                        strokeWidth = 2.6.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawRoundRect(
                        color = goldColor,
                        topLeft = Offset(w * 0.24f, h * 0.52f),
                        size = Size(w * 0.10f, h * 0.24f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                    drawRoundRect(
                        color = goldColor,
                        topLeft = Offset(w * 0.66f, h * 0.52f),
                        size = Size(w * 0.10f, h * 0.24f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                }

                // 2. SAJÁT TESTSÚLYOS (Calisthenics / Pull-up / Bodyweight figure)
                lower.contains("testsúly") || lower.contains("body") || lower.contains("kezdő") || lower.contains("calisthen") -> {
                    // Pull-up bar at top
                    drawLine(
                        color = goldColor,
                        start = Offset(w * 0.20f, h * 0.22f),
                        end = Offset(w * 0.80f, h * 0.22f),
                        strokeWidth = 2.4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    // Athlete head
                    drawCircle(color = greenColor, radius = w * 0.085f, center = Offset(w * 0.50f, h * 0.38f))
                    // Arms reaching up to bar
                    drawLine(
                        color = greenColor,
                        start = Offset(w * 0.42f, h * 0.46f),
                        end = Offset(w * 0.30f, h * 0.24f),
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = greenColor,
                        start = Offset(w * 0.58f, h * 0.46f),
                        end = Offset(w * 0.70f, h * 0.24f),
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    // V-torso & legs
                    drawLine(
                        color = accent,
                        start = Offset(w * 0.50f, h * 0.46f),
                        end = Offset(w * 0.50f, h * 0.76f),
                        strokeWidth = 3.2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // 3. SÚLYZÓS / FELSŐ-ALSÓ / ERŐ (Heavy Olympic Barbell with dual plates)
                else -> {
                    drawLine(
                        color = accent,
                        start = Offset(w * 0.14f, h * 0.50f),
                        end = Offset(w * 0.86f, h * 0.50f),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    // Inner heavy plates
                    drawRoundRect(
                        color = goldColor,
                        topLeft = Offset(w * 0.24f, h * 0.28f),
                        size = Size(w * 0.10f, h * 0.44f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                    drawRoundRect(
                        color = goldColor,
                        topLeft = Offset(w * 0.66f, h * 0.28f),
                        size = Size(w * 0.10f, h * 0.44f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                    // Outer smaller plates
                    drawRoundRect(
                        color = secondaryColor,
                        topLeft = Offset(w * 0.15f, h * 0.35f),
                        size = Size(w * 0.07f, h * 0.30f),
                        cornerRadius = CornerRadius(3f, 3f)
                    )
                    drawRoundRect(
                        color = secondaryColor,
                        topLeft = Offset(w * 0.78f, h * 0.35f),
                        size = Size(w * 0.07f, h * 0.30f),
                        cornerRadius = CornerRadius(3f, 3f)
                    )
                }
            }
        }
    }
}

fun programEquipmentCategoryLabel(programNameOrId: String): String {
    val lower = programNameOrId.lowercase()
    return when {
        lower.contains("otthon") || lower.contains("alap") -> "Otthoni • Kézi súlyzós & gumiszalag"
        lower.contains("testsúly") || lower.contains("kezdő") -> "Saját testsúlyos • Eszköz nélkül"
        else -> "Súlyzós • Konditermi rúd & kézi súlyzó"
    }
}

/**
 * Custom Smartwatch (Galaxy Watch7 / TrainPilot óra) Icon Badge matching Mockup 3/4 #2 & Mockup 4/4 #2.
 */
@Composable
fun MockupWatchGraphicBadge(
    modifier: Modifier = Modifier.size(44.dp)
) {
    val palette = LocalTrainPilotPalette.current
    val gold = palette.primary
    val green = palette.success
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // Top & bottom watch straps
        drawRoundRect(
            color = Color(0xFF334155),
            topLeft = Offset(w * 0.34f, h * 0.06f),
            size = Size(w * 0.32f, h * 0.88f),
            cornerRadius = CornerRadius(8f, 8f)
        )
        // Watch crown on right
        drawRoundRect(
            color = gold,
            topLeft = Offset(w * 0.82f, h * 0.36f),
            size = Size(w * 0.10f, h * 0.16f),
            cornerRadius = CornerRadius(4f, 4f)
        )
        // Watch case & screen
        drawCircle(color = Color(0xFF1E293B), radius = w * 0.36f, center = Offset(w * 0.50f, h * 0.50f))
        drawCircle(
            color = gold,
            radius = w * 0.36f,
            center = Offset(w * 0.50f, h * 0.50f),
            style = Stroke(width = 2.dp.toPx())
        )
        // Mini ECG pulse line on watch face
        val pulsePath = Path().apply {
            moveTo(w * 0.28f, h * 0.52f)
            lineTo(w * 0.40f, h * 0.52f)
            lineTo(w * 0.46f, h * 0.36f)
            lineTo(w * 0.54f, h * 0.64f)
            lineTo(w * 0.60f, h * 0.48f)
            lineTo(w * 0.72f, h * 0.48f)
        }
        drawPath(pulsePath, color = gold, style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        // Green connected dot at top-right of bezel
        drawCircle(color = green, radius = w * 0.08f, center = Offset(w * 0.74f, h * 0.24f))
    }
}

/**
 * Custom Health Connect Dual-Link Heart Emblem matching Mockup 3/4 #2 & Mockup 4/4 #2.
 */
@Composable
fun MockupHealthConnectBadge(
    modifier: Modifier = Modifier.size(36.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // Soft rounded background badge
        drawRoundRect(
            color = Color(0xFF0EA5E9).copy(alpha = 0.16f),
            size = size,
            cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
        )
        // Left interlocking cyan loop
        drawCircle(
            color = Color(0xFF38BDF8),
            radius = w * 0.22f,
            center = Offset(w * 0.40f, h * 0.50f),
            style = Stroke(width = 2.6.dp.toPx())
        )
        // Right interlocking emerald/gold loop
        drawCircle(
            color = Color(0xFF34D399),
            radius = w * 0.22f,
            center = Offset(w * 0.60f, h * 0.50f),
            style = Stroke(width = 2.6.dp.toPx())
        )
    }
}

@Composable
fun ExercisePostureMiniIllustration(
    exerciseName: String,
    size: androidx.compose.ui.unit.Dp
) {
    ExercisePostureMiniIllustration(
        exerciseNameOrId = exerciseName,
        modifier = Modifier.size(size)
    )
}

@Composable
fun ProgramTypeEmblemIcon(
    programTitle: String,
    isHighlighted: Boolean = false,
    size: androidx.compose.ui.unit.Dp
) {
    ProgramTypeEmblemIcon(
        programNameOrId = programTitle,
        isHighlighted = isHighlighted,
        modifier = Modifier.size(size)
    )
}

@Composable
fun MockupWatchGraphicBadge(
    size: androidx.compose.ui.unit.Dp
) {
    MockupWatchGraphicBadge(
        modifier = Modifier.size(size)
    )
}

@Composable
fun MockupHealthConnectBadge(
    size: androidx.compose.ui.unit.Dp
) {
    MockupHealthConnectBadge(
        modifier = Modifier.size(size)
    )
}


