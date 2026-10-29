package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.LocalizedStrings
import com.example.model.HomeDemoMode
import com.example.ui.components.TrainPilotMountainLogo
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.viewmodel.TrainPilotUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    state: TrainPilotUiState,
    strings: LocalizedStrings,
    onOpenPalettePicker: () -> Unit,
    onOpenSettings: () -> Unit,
    onCycleDemoMode: (HomeDemoMode) -> Unit,
    onStartOrContinueWorkout: () -> Unit,
    onOpenProgramDayDetails: () -> Unit,
    onOpenQuickWorkout: () -> Unit,
    onOpenWatchAndSync: () -> Unit,
    onOpenHealthSleep: () -> Unit,
    onOpenHealthHeart: () -> Unit,
    onOpenHealthSteps: () -> Unit,
    onOpenCoachReadiness: () -> Unit,
    onOpenProgramsList: () -> Unit,
    onOpenCalendarPlanner: () -> Unit,
    onOpenGoalsDialog: () -> Unit,
    onOpenPersonalPlanner: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    val activeProgram = state.programs.firstOrNull { it.isActive } ?: state.programs.first()
    val currentDay = activeProgram.days.firstOrNull()
    var showExtendedMainCards by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // Interactive DEMO State Switcher Pill Bar so reviewer can test all Home states
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val modes = listOf(
                HomeDemoMode.PAUSED_WORKOUT to "⏸ Szüneteltetett (Mockup)",
                HomeDemoMode.PLANNED_NEXT to "📅 Tervezett (Main)",
                HomeDemoMode.COACH_LIGHT_DAY to "⚠️ Könnyített nap (54/100)"
            )
            modes.forEach { (mode, label) ->
                val selected = state.homeDemoMode == mode
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (selected) palette.primary.copy(alpha = 0.22f) else palette.surface,
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            color = if (selected) palette.primary else palette.cardBorder,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable { onCycleDemoMode(mode) }
                        .testTag("home_demo_mode_${mode.name.lowercase()}")
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (selected) palette.primary else palette.textSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // 2. GREETING SECTION ("Szia, Levente!" + "Folytasd a megkezdett edzést!") + "Profil és új terv" at top
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = strings.greetingTitle,
                    style = MaterialTheme.typography.headlineLarge,
                    color = palette.textPrimary
                )
                Text(
                    text = when (state.homeDemoMode) {
                        HomeDemoMode.PAUSED_WORKOUT -> strings.greetingPausedSub
                        HomeDemoMode.PLANNED_NEXT -> strings.greetingPlannedSub
                        HomeDemoMode.COACH_LIGHT_DAY -> strings.greetingLightSub
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.textSecondary
                )
            }
        }

        // Személyes edzéstervező ("Profil és új terv") card at the top of Kezdőlap
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = palette.surface,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
                .testTag("home_personal_planner_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.personalPlannerTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = palette.textPrimary
                        )
                        Text(
                            text = "${strings.personalPlannerSub} (${state.trainingStyleHu} • ${state.progressionRuleHu})",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    }
                }
                Button(
                    onClick = onOpenPersonalPlanner,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primary,
                        contentColor = palette.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("home_profile_new_plan_button")
                ) {
                    Text(
                        text = strings.profileAndNewPlanBtn,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        // 3. HERO WORKOUT CARD (Mockup 1/4 #1: "Szüneteltetve • Teljes test A • 8 gyakorlat • kb. 60 perc • ▶ Folytatás")
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = palette.surface,
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            palette.primary.copy(alpha = 0.55f),
                            palette.cardBorder
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .clickable { onOpenProgramDayDetails() }
                .testTag("home_hero_workout_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                palette.surfaceElevated.copy(alpha = 0.65f),
                                palette.surface
                            )
                        )
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Dumbbell Icon in warm dark gold container
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = palette.primary.copy(alpha = 0.14f),
                        modifier = Modifier
                            .size(56.dp)
                            .border(
                                width = 1.dp,
                                color = palette.primary.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(14.dp)
                            )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = palette.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (state.homeDemoMode == HomeDemoMode.PAUSED_WORKOUT) {
                                Icon(
                                    imageVector = Icons.Default.Pause,
                                    contentDescription = null,
                                    tint = palette.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = strings.pausedBadge,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = palette.primary
                                )
                            } else {
                                Text(
                                    text = "${strings.activeProgramBadge} • ${activeProgram.localizedName(state.language)}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = palette.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Text(
                            text = currentDay?.localizedTitle(state.language) ?: "Teljes test A",
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 21.sp),
                            color = palette.textPrimary
                        )

                        Text(
                            text = "${currentDay?.exercises?.size ?: 8} gyakorlat • kb. ${currentDay?.durationMinutes ?: 60} perc",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Részletek és szerkesztés",
                        tint = palette.textSecondary
                    )
                }

                // Full-width gold primary CTA Button ("▶ Folytatás" / "▶ Tervezett edzés indítása")
                Button(
                    onClick = onStartOrContinueWorkout,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primary,
                        contentColor = palette.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("home_continue_workout_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (state.homeDemoMode == HomeDemoMode.PAUSED_WORKOUT) {
                            strings.continueBtn
                        } else {
                            strings.startPlannedBtn
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        // 4. QUICK WORKOUT CARD (Mockup 1/4 #1: "Gyors edzés • Indíts egy rövid edzést most >")
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = palette.surface,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
                .clickable { onOpenQuickWorkout() }
                .testTag("home_quick_workout_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = palette.surfaceElevated,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                            contentDescription = null,
                            tint = palette.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.quickWorkoutTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary
                    )
                    Text(
                        text = strings.quickWorkoutSub,
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = palette.textSecondary
                )
            }
        }

        // 5. WEAR OS WATCH CARD (Mockup 1/4 #1: "Galaxy Watch7 • Csatlakoztatva • 92% • ●")
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = palette.surface,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
                .clickable { onOpenWatchAndSync() }
                .testTag("home_watch_status_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = palette.surfaceElevated,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Watch,
                            contentDescription = null,
                            tint = palette.textPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.watchName,
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary
                    )
                    Text(
                        text = if (state.watchConnected) {
                            "Csatlakoztatva • ${state.watchBatteryPercent}%"
                        } else {
                            "Offline mód • Szinkronra vár"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                }
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(if (state.watchConnected) palette.success else palette.warning)
                )
            }
        }

        // 6. 2x2 VITALS & READINESS BENTO GRID (Mockup 1/4 #1: Alvás, Nyugalmi pulzus, Lépések, Samsung energia)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top-Left: Alvás (7 ó 24 p • Jó minőség)
            HomeVitalCard(
                modifier = Modifier
                    .weight(1f)
                    .testTag("home_vital_sleep_card"),
                icon = Icons.Default.Bedtime,
                iconTint = palette.sleepAccent,
                title = strings.sleepTitle,
                mainValue = state.sleepDurationLabel,
                statusText = strings.sleepQualityGood,
                statusColor = palette.success,
                onClick = onOpenHealthSleep
            )

            // Top-Right: Nyugalmi pulzus (62 bpm • Normális)
            HomeVitalCard(
                modifier = Modifier
                    .weight(1f)
                    .testTag("home_vital_hr_card"),
                icon = Icons.Default.Favorite,
                iconTint = palette.heartAccent,
                title = strings.restingHrTitle,
                mainValue = "${state.restingHrBpm} bpm",
                statusText = strings.restingHrNormal,
                statusColor = palette.success,
                onClick = onOpenHealthHeart
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Bottom-Left: Lépések (8 421 • Cél: 10 000 + cyan progress bar)
            val stepsProgress = (state.dailySteps.toFloat() / state.stepsGoal.toFloat()).coerceIn(0f, 1f)
            HomeVitalCard(
                modifier = Modifier
                    .weight(1f)
                    .testTag("home_vital_steps_card"),
                icon = Icons.AutoMirrored.Filled.DirectionsRun,
                iconTint = palette.stepsAccent,
                title = strings.stepsTitle,
                mainValue = formatThousand(state.dailySteps),
                statusText = "Cél: ${formatThousand(state.stepsGoal)}",
                statusColor = palette.textSecondary,
                progressFraction = stepsProgress,
                progressColor = palette.stepsAccent,
                onClick = onOpenHealthSteps
            )

            // Bottom-Right: Samsung energia (87 / 100 • Jó állapot + green progress bar + Forrás: Samsung Health)
            val energyProgress = (state.samsungEnergyScore / 100f).coerceIn(0f, 1f)
            HomeVitalCard(
                modifier = Modifier
                    .weight(1f)
                    .testTag("home_vital_energy_card"),
                icon = Icons.Default.Eco,
                iconTint = palette.energyAccent,
                title = strings.samsungEnergyTitle,
                mainValue = "${state.samsungEnergyScore} / 100",
                statusText = if (state.samsungEnergyScore >= 75) strings.energyGoodState else "Mérsékelt",
                statusColor = if (state.samsungEnergyScore >= 75) palette.success else palette.warning,
                progressFraction = energyProgress,
                progressColor = palette.energyAccent,
                footerCaption = strings.sourceSamsungHealth,
                onClick = onOpenCoachReadiness
            )
        }

        // 7. QUICK NAVIGATION ROW (Programok / Edzéstervek, Naptár / Tervezés, Coach / Felkészültség)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HomeQuickShortcutTile(
                modifier = Modifier
                    .weight(1f)
                    .testTag("home_shortcut_programs"),
                icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                title = "Programok",
                subtitle = "Edzéstervek",
                onClick = onOpenProgramsList
            )
            HomeQuickShortcutTile(
                modifier = Modifier
                    .weight(1f)
                    .testTag("home_shortcut_calendar"),
                icon = Icons.Default.CalendarMonth,
                title = "Naptár",
                subtitle = "Tervezés",
                onClick = onOpenCalendarPlanner
            )
            HomeQuickShortcutTile(
                modifier = Modifier
                    .weight(1f)
                    .testTag("home_shortcut_coach"),
                icon = Icons.Default.Psychology,
                title = "Coach",
                subtitle = "${state.coachReadinessScore}/100 kész.",
                onClick = onOpenCoachReadiness
            )
        }

        // 8. MAIN BRANCH HOME CARDS (Célok beállítása, Mai javaslat / Coach, Személyes edzéstervező, Aktív kalória)
        AnimatedVisibility(visible = showExtendedMainCards) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Célok beállítása (1/15 edzés >) from main Home screenshot
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
                        .clickable { onOpenGoalsDialog() }
                        .testTag("home_goals_row_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.goalsSetupTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.textPrimary
                            )
                            Text(
                                text = "Aktív kalória ma: ${state.activeCaloriesKcal} kcal • Mai edzés: ${state.weeklyGoalCompleted}",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${state.weeklyGoalCompleted}/${state.weeklyGoalTarget} edzés",
                                style = MaterialTheme.typography.labelLarge,
                                color = palette.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = palette.primary
                            )
                        }
                    }
                }

                // MAI JAVASLAT / TrainPilot Coach Card from main Home screenshot
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = palette.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = palette.primary.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .clickable { onOpenCoachReadiness() }
                        .testTag("home_coach_advice_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = palette.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = strings.coachSuggestionHeader,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = palette.primary
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = palette.primary
                            )
                        }

                        val isLightDay = state.homeDemoMode == HomeDemoMode.COACH_LIGHT_DAY
                        Text(
                            text = if (isLightDay) strings.coachLightDayTitle else strings.coachNormalDayTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.textPrimary
                        )
                        Text(
                            text = if (isLightDay) strings.coachLightDayBody else strings.coachNormalDayBody,
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = palette.surfaceElevated,
                                modifier = Modifier.border(1.dp, palette.cardBorder, RoundedCornerShape(12.dp))
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                    Text(
                                        text = strings.readinessTodayLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.textSecondary
                                    )
                                    Text(
                                        text = "${state.coachReadinessScore}/100",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = palette.textPrimary
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = palette.surfaceElevated,
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, palette.cardBorder, RoundedCornerShape(12.dp))
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                    Text(
                                        text = "Következő edzés",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.textSecondary
                                    )
                                    Text(
                                        text = "${activeProgram.localizedName(state.language)} • ${currentDay?.dayCode ?: "A"}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = palette.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeVitalCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    mainValue: String,
    statusText: String,
    statusColor: Color,
    progressFraction: Float? = null,
    progressColor: Color = iconTint,
    footerCaption: String? = null,
    onClick: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = palette.surface,
        modifier = modifier
            .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = mainValue,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = palette.textPrimary
            )

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = statusColor
            )

            if (progressFraction != null) {
                Spacer(modifier = Modifier.height(2.dp))
                LinearProgressIndicator(
                    progress = { progressFraction },
                    color = progressColor,
                    trackColor = palette.surfaceElevated,
                    strokeCap = StrokeCap.Round,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }

            if (footerCaption != null) {
                Text(
                    text = footerCaption,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun HomeQuickShortcutTile(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = palette.surface,
        modifier = modifier
            .border(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = palette.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatThousand(value: Int): String {
    val s = value.toString()
    return if (s.length > 3) {
        "${s.dropLast(3)} ${s.takeLast(3)}"
    } else s
}
