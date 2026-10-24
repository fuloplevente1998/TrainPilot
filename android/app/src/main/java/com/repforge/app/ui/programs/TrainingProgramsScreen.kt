package com.repforge.app.ui.programs
import androidx.compose.foundation.layout.size

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.repforge.app.data.db.ProgramWithDays
import com.repforge.app.ui.theme.LocalTrainPilotColors
import org.json.JSONArray

@Composable
fun TrainingProgramsScreen(
    navController: NavController,
    language: String = "hu",
    viewModel: TrainingProgramsViewModel = viewModel(),
    onStartProgramDay: (String, String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalTrainPilotColors.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("program_list")
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (state.writeFailed) item {
            Text(com.repforge.app.ui.text.nativeResources(androidx.compose.ui.platform.LocalContext.current, state.language)
                .getString(com.repforge.app.R.string.metadata_write_failed), color = colors.danger)
        }

        // 1. #37 Személyes Edzéstervező nyitó kártya (Personal Planner Banner)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.openPersonalPlanner() },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colors.card2),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.emphasisBorder)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = colors.accentText,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Személyes Edzéstervező",
                            color = colors.text,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Készíts egyéni tervet szinted és céljaid alapján",
                            color = colors.muted,
                            fontSize = 12.sp
                        )
                    }
                    Text("Megnyitás ›", color = colors.accentText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Edzésprogramok",
                    color = colors.text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${state.programs.size} program elérhető",
                    color = colors.muted,
                    fontSize = 12.sp
                )
            }
        }

        // 2. Programkártyák listája
        items(state.programs, key = { it.program.id }) { programWithDays ->
            val isActive = programWithDays.program.id == state.activeProgramId
            ProgramCardItem(
                programWithDays = programWithDays,
                isActive = isActive,
                language = language,
                onActivate = { viewModel.activateProgram(programWithDays.program.id) },
                onStartDay = { dayId -> onStartProgramDay(programWithDays.program.id, dayId) }
            )
        }
    }
}

@Composable
fun ProgramCardItem(
    programWithDays: ProgramWithDays,
    isActive: Boolean,
    language: String = "hu",
    onActivate: () -> Unit,
    onStartDay: (String) -> Unit
) {
    val colors = LocalTrainPilotColors.current
    var expanded by remember { mutableStateOf(isActive) }
    val prog = programWithDays.program
    fun name(hu: String, en: String, de: String, ro: String) = when (language) {
        "en" -> en; "de" -> de; "ro" -> ro; else -> hu
    }.ifBlank { hu }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("program_${prog.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isActive) colors.selectedBorder else colors.line
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.card2)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(com.repforge.app.data.db.NativeBuiltinCatalog.programMetadata(prog, "location", language), color = colors.accent2, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.card2)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(com.repforge.app.data.db.NativeBuiltinCatalog.programMetadata(prog, "level", language), color = colors.muted, fontSize = 10.sp)
                    }
                }
                if (isActive) {
                    Text("Aktív", color = colors.accentText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                } else {
                    OutlinedButton(
                        onClick = onActivate,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.text)
                    ) {
                        Text("Kiválasztás", fontSize = 11.sp)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(name(prog.nameHu, prog.nameEn, prog.nameDe, prog.nameRo), color = colors.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(prog.description, color = colors.muted, fontSize = 12.sp)

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${programWithDays.days.size} edzésnap megtekintése",
                    color = colors.accentText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = colors.accent
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    programWithDays.days.sortedBy { it.sortOrder }.forEach { day ->
                        val exCount = runCatching { JSONArray(day.exercisesJson).length() }.getOrDefault(0)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.card2)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(name(day.nameHu, day.nameEn, day.nameDe, day.nameRo), color = colors.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text("$exCount gyakorlat", color = colors.muted, fontSize = 11.sp)
                            }
                            Button(
                                onClick = { onStartDay(day.dayId) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = colors.onAccent)
                                Spacer(Modifier.width(4.dp))
                                Text("Indítás", color = colors.onAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
