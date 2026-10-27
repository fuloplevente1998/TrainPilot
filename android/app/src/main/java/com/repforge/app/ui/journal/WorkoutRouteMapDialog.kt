package com.repforge.app.ui.journal
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.repforge.app.ui.theme.LocalTrainPilotColors
import com.repforge.app.ui.components.TrainPilotStatusBadge
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.tan

@Composable
fun WorkoutRouteMapDialog(route: RouteCoordinatesUiModel, onDismiss: () -> Unit, language: String = "hu") {
    val colors = LocalTrainPilotColors.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val text = androidx.compose.runtime.remember(context, language) { journalResources(context, language) }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        androidx.compose.material3.Surface(Modifier.fillMaxSize(), color = colors.bg) {
            Column(Modifier.fillMaxSize().systemBarsPadding().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(text.getString(com.repforge.app.R.string.journal_route_title), Modifier.weight(1f), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)
                        .testTag("route_close").clip(RoundedCornerShape(12.dp)).background(colors.danger)) {
                        Icon(Icons.Default.Close, text.getString(com.repforge.app.R.string.journal_close), tint = colors.dangerText)
                    }
                }
                // Only observed, saved coordinates: never show Studio's illustrative map metrics.
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    TrainPilotStatusBadge(text.getString(com.repforge.app.R.string.journal_route_points, route.pointCount))
                    if (route.missingRouteParts > 0)
                        Text(text.getString(com.repforge.app.R.string.journal_partial_route),
                            color = colors.muted, style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
                }
                androidx.compose.material3.Surface(
                    modifier = Modifier.fillMaxWidth().weight(1f).testTag("route_map_canvas"),
                    shape = RoundedCornerShape(20.dp),
                    color = colors.card2,
                    border = BorderStroke(1.dp, colors.selectedBorder)
                ) {
                    Box(Modifier.fillMaxSize().padding(8.dp)) {
                        // Preserve the individual GPS segments; gaps never become a drawn path.
                        MercatorRouteCanvas(route.segments)
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    RouteLegendItem(colors.accent2, text.getString(com.repforge.app.R.string.journal_route_start))
                    RouteLegendItem(colors.success, text.getString(com.repforge.app.R.string.journal_route_path))
                    RouteLegendItem(colors.accent, text.getString(com.repforge.app.R.string.journal_route_finish))
                }
            }
        }
    }
}

@Composable
fun MercatorRouteCanvas(segments: List<List<Pair<Double, Double>>>) {
    val colors = LocalTrainPilotColors.current
    val allPoints = segments.flatten()
    if (allPoints.isEmpty()) return

    Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        fun mercatorY(lat: Double): Double {
            val rad = Math.toRadians(min(85.0, max(-85.0, lat)))
            return ln(tan(Math.PI / 4.0 + rad / 2.0))
        }

        val projected = allPoints.map { (lat, lon) ->
            Math.toRadians(lon) to -mercatorY(lat)
        }
        val minX = projected.minOf { it.first }
        val maxX = projected.maxOf { it.first }
        val minY = projected.minOf { it.second }
        val maxY = projected.maxOf { it.second }
        val spanX = max(maxX - minX, 0.00001)
        val spanY = max(maxY - minY, 0.00001)

        val scale = min(size.width / spanX, size.height / spanY) * 0.9
        val cx = (minX + maxX) / 2.0
        val cy = (minY + maxY) / 2.0

        fun toCanvasOffset(p: Pair<Double, Double>): Offset {
            val x = (size.width / 2.0 + (Math.toRadians(p.second) - cx) * scale).toFloat()
            val y = (size.height / 2.0 + (-mercatorY(p.first) - cy) * scale).toFloat()
            return Offset(x, y)
        }

        // Rajzoljuk ki a szegmenseket külön Path-ként (így a szünetek nem kötődnek össze egyenes vonallal!)
        for (segment in segments) {
            if (segment.isEmpty()) continue
            val path = Path()
            segment.forEachIndexed { idx, pt ->
                val offset = toCanvasOffset(pt)
                if (idx == 0) path.moveTo(offset.x, offset.y) else path.lineTo(offset.x, offset.y)
            }
            drawPath(
                path = path,
                color = colors.success,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }

        // Rajt pont (#30DFF2)
        val startOffset = toCanvasOffset(allPoints.first())
        drawCircle(color = colors.bg, radius = 9.dp.toPx(), center = startOffset)
        drawCircle(color = colors.accent2, radius = 6.dp.toPx(), center = startOffset)

        // Cél pont (#FFB93D)
        val finishOffset = toCanvasOffset(allPoints.last())
        drawCircle(color = colors.bg, radius = 9.dp.toPx(), center = finishOffset)
        drawCircle(color = colors.accent, radius = 6.dp.toPx(), center = finishOffset)
    }
}

@Composable
fun RouteLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, color = LocalTrainPilotColors.current.muted, fontSize = 11.sp)
    }
}
