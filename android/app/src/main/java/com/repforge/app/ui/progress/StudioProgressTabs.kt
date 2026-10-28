package com.repforge.app.ui.progress

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.repforge.app.R
import com.repforge.app.ui.text.nativeResources
import com.repforge.app.ui.theme.LocalTrainPilotColors

/** Approved 18:49 AI Studio three-tab Progress header.
 * This presentation state is local: changing a tab never resets native progress
 * filters, strength queries, photo privacy or actual cardio data.
 */
@Composable
internal fun StudioProgressTabs(
    selected: Int, language: String, onSelect: (Int) -> Unit
) {
    val colors = LocalTrainPilotColors.current
    val context = LocalContext.current
    val labels = listOf(
        R.string.studio_progress_tab_progress,
        R.string.studio_progress_tab_statistics,
        R.string.studio_progress_tab_strength
    ).map { nativeResources(context, language).getString(it) }
    val scroll = LocalConfiguration.current.screenWidthDp < 355 ||
        LocalDensity.current.fontScale > 1.3f
    val shape = RoundedCornerShape(14.dp)
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("progress_studio_sections"),
        shape = shape, color = colors.card, border = BorderStroke(1.dp, colors.line)
    ) {
        Row(Modifier.fillMaxWidth()
            .then(if (scroll) Modifier.horizontalScroll(rememberScrollState()) else Modifier)
            .padding(4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            labels.forEachIndexed { index, label ->
                val active = selected == index
                val pill = RoundedCornerShape(10.dp)
                val fill = if (active)
                    colors.accent.copy(alpha = .22f).compositeOver(colors.card)
                    else colors.card
                Box(
                    modifier = (if (scroll) Modifier.widthIn(min = 125.dp)
                        else Modifier.weight(1f)).heightIn(min = 48.dp)
                        .clip(pill)
                        .background(fill)
                        .then(if (active) Modifier.border(1.dp, colors.accent, pill) else Modifier)
                        .clickable(role = Role.Tab) { onSelect(index) }
                        .semantics { this.selected = active }
                        .testTag("progress_section_tab_$index")
                        .padding(horizontal = 4.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        color = if (active) colors.accentText else colors.muted,
                        maxLines = 2)
                }
            }
        }
    }
}

/** Accepted Studio's nested personal-records / cardio-statistics segmented bar. */
@Composable
internal fun StudioProgressStatisticsTabs(
    selected: Int, language: String, onSelect: (Int) -> Unit
) {
    val colors = LocalTrainPilotColors.current
    val context = LocalContext.current
    val strings = nativeResources(context, language)
    val labels = listOf(
        strings.getString(R.string.studio_progress_stats_records),
        strings.getString(R.string.studio_progress_stats_cardio)
    )
    val stack = LocalConfiguration.current.screenWidthDp < 330 ||
        LocalDensity.current.fontScale > 1.5f
    val content: @Composable (Int, String, Modifier) -> Unit = { index, label, modifier ->
        val active = selected == index
        val shape = RoundedCornerShape(14.dp)
        val fill = if (active)
            colors.accent.copy(alpha = .22f).compositeOver(colors.card)
            else colors.card
        Surface(
            modifier = modifier.heightIn(min = 48.dp)
                .testTag("progress_stats_subtab_$index")
                .semantics { this.selected = active },
            onClick = { onSelect(index) }, shape = shape, color = fill,
            border = BorderStroke(1.dp, if (active) colors.accent else colors.line)
        ) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center) {
                Text(label, style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold, maxLines = 2,
                    color = if (active) colors.accentText else colors.text)
            }
        }
    }
    if (stack) Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        labels.forEachIndexed { index, label -> content(index, label, Modifier.fillMaxWidth()) }
    } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        labels.forEachIndexed { index, label -> content(index, label, Modifier.weight(1f)) }
    }
}
