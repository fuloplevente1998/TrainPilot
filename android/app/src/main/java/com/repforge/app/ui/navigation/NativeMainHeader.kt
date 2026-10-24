package com.repforge.app.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.repforge.app.ui.theme.LocalTrainPilotColors

/** Main's familiar two rows of four buttons; no replacement five-tab bottom navigation. */
@Composable
internal fun NativeMainHeader(route: String, language: String, onSelect: (String) -> Unit) {
    val colors = LocalTrainPilotColors.current
    val selected = NativeMainMenu.selected(route)
    val labels = NativeMainMenu.labels(language)
    Surface(color = colors.bg) {
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (row in NativeMainMenu.routes.withIndex().toList().chunked(4)) {
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for ((index, destination) in row) {
                        val current = selected == destination
                        val icon = when(destination) {
                            "home" -> Icons.Default.Home; "training" -> Icons.Default.FitnessCenter
                            "health" -> Icons.Default.FavoriteBorder; "programs" -> Icons.Default.ViewAgenda
                            "journal" -> Icons.Default.MenuBook; "calendar" -> Icons.Default.DateRange
                            "coach" -> Icons.Default.AutoAwesome; else -> Icons.Default.Settings
                        }
                        Card(modifier = Modifier.weight(1f).fillMaxHeight()
                            .semantics { this.selected = current }
                            .clickable(role = Role.Tab, onClick = { onSelect(destination) }),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, if (current) colors.accent else colors.line),
                            colors = CardDefaults.cardColors(containerColor = if (current) colors.accent.copy(alpha = .25f) else colors.card2)) {
                            Column(Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(horizontal = 4.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(icon, contentDescription = null, tint = if (current) colors.text else colors.muted, modifier = Modifier.size(26.dp))
                                Text(labels[index], color = if (current) colors.text else colors.muted, fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }
        }
    }
}
