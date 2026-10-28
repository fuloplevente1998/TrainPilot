package com.repforge.app.ui.home

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.repforge.app.ui.theme.LocalTrainPilotColors

/** Direct 18:49 source-tree port of HomeVitalCard.
 *
 * The original source used the exact same 18dp radius / 14dp inset,
 * 22dp icon, 6dp spacing, value/status/progress/footer order.
 * Instead of DEMO input we take real native TodayMetricsUiModel fields.
 * Missing status/progress is not replaced by a synthetic measurement.
 */
@Composable
internal fun NativeStudioHomeVitalCard(
    key: String,
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color,
    title: String,
    mainValue: String,
    statusText: String? = null,
    statusColor: Color,
    progressFraction: Float? = null,
    progressColor: Color = iconTint,
    footerCaption: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val palette = LocalTrainPilotColors.current
    val shape = RoundedCornerShape(18.dp)
    Surface(
        shape = shape, color = palette.card,
        modifier = modifier.heightIn(min = 48.dp)
            .border(1.dp, palette.line, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .testTag("home_metric_$key")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, contentDescription = null, tint = iconTint,
                    modifier = Modifier.size(22.dp))
                Text(title, style = MaterialTheme.typography.bodyMedium,
                    color = palette.muted, maxLines = 2,
                    overflow = TextOverflow.Ellipsis)
            }
            Text(
                text = mainValue,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold),
                color = palette.text,
                modifier = Modifier.testTag("home_metric_${key}_value")
            )
            if (statusText != null) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold),
                    color = statusColor,
                    modifier = Modifier.testTag("home_metric_${key}_source")
                )
            }
            if (progressFraction != null && progressFraction.isFinite()) {
                Spacer(Modifier.height(2.dp))
                LinearProgressIndicator(
                    progress = { progressFraction.coerceIn(0f, 1f) },
                    color = progressColor,
                    trackColor = palette.card2,
                    strokeCap = StrokeCap.Round,
                    modifier = Modifier.fillMaxWidth().height(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }
            if (footerCaption != null) {
                Text(footerCaption, style = MaterialTheme.typography.labelSmall,
                    color = palette.muted.copy(alpha = .8f), maxLines = 2,
                    overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
