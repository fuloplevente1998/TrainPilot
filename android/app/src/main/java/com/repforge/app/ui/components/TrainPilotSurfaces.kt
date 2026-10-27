package com.repforge.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import com.repforge.app.ui.theme.LocalTrainPilotColors
import com.repforge.app.ui.theme.TrainPilotDimensions

/** A presentation surface; callers own all state, labels, callbacks and test tags. */
@Composable
fun TrainPilotSurfaceCard(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalTrainPilotColors.current
    val shape = RoundedCornerShape(TrainPilotDimensions.CardRadius)
    val border = BorderStroke(if (selected) 2.dp else 1.dp,
        if (selected) colors.selectedBorder else colors.line)
    val cardModifier = if (selected) modifier.semantics { this.selected = true } else modifier
    val fill = if (selected) colors.selectedSurface else colors.card
    val body: @Composable () -> Unit = {
        Column(Modifier.padding(TrainPilotDimensions.Page),
            verticalArrangement = Arrangement.spacedBy(TrainPilotDimensions.Small), content = content)
    }
    if (onClick == null) Surface(cardModifier, shape, color = fill,
        contentColor = colors.text, border = border, content = body)
    else Surface(onClick = onClick, modifier = cardModifier.semantics { role = Role.Button },
        enabled = enabled, shape = shape, color = fill, contentColor = colors.text,
        border = border, content = body)
}

@Composable
fun TrainPilotSectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(title, modifier = modifier, style = MaterialTheme.typography.titleLarge,
        color = LocalTrainPilotColors.current.text)
}

/** Presentation-only variants share the existing palette; no domain or timer state. */
@Composable
fun TrainPilotHeroSurface(modifier: Modifier = Modifier,
    focus: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalTrainPilotColors.current
    TrainPilotPanel(modifier, colors.card2, colors.selectedBorder, 22,
        if (focus) colors.accent else null, content)
}

@Composable
fun TrainPilotSectionSurface(modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalTrainPilotColors.current
    TrainPilotPanel(modifier, colors.card, colors.line, 18, null, content)
}

@Composable
fun TrainPilotSubtleSurface(modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalTrainPilotColors.current
    TrainPilotPanel(modifier, colors.card2, colors.line, 12, null, content)
}

@Composable
private fun TrainPilotPanel(modifier: Modifier, fill: Color, outline: Color,
    radius: Int, focus: Color?, content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalTrainPilotColors.current
    Surface(modifier, shape = RoundedCornerShape(radius.dp), color = fill,
        contentColor = colors.text, border = BorderStroke(1.dp, outline)) {
        Column(Modifier.drawWithContent {
            drawContent()
            if (focus != null) drawRect(focus, Offset(0f, 16.dp.toPx()),
                Size(3.dp.toPx(), (size.height - 32.dp.toPx()).coerceAtLeast(0f)))
        }.padding(TrainPilotDimensions.Page),
            verticalArrangement = Arrangement.spacedBy(TrainPilotDimensions.Small), content = content)
    }
}

@Composable
fun TrainPilotStatusBadge(text: String, modifier: Modifier = Modifier) {
    val colors = LocalTrainPilotColors.current
    Surface(modifier, shape = RoundedCornerShape(8.dp), color = colors.selectedSurface,
        contentColor = colors.accentText, border = BorderStroke(1.dp, colors.line)) {
        Text(text, Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"))
    }
}

@Composable
fun TrainPilotPrimaryButton(text: String, onClick: () -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true) {
    val colors = LocalTrainPilotColors.current
    Button(onClick, modifier.heightIn(min = TrainPilotDimensions.PrimaryAction), enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.onAccent)) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}
