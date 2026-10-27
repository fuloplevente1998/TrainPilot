package com.repforge.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
