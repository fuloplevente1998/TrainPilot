package com.repforge.app.ui.journal
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.TextButton
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
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.tan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutRouteMapDialog(
    route: RouteCoordinatesUiModel,
    onDismiss: () -> Unit
) {
    val colors = LocalTrainPilotColors.current

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(colors.card)
            .border(1.dp, colors.line, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("GPS-útvonal", color = colors.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = onDismiss) {
                    Text("Bezárás", color = colors.accent, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("Offline útvonalnézet • ${route.pointCount} pont", color = colors.muted, fontSize = 12.sp)
            Spacer(Modifier.height(14.dp))

            // Canvas útvonal rajzoló
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF090B10))
                    .border(1.dp, colors.line, RoundedCornerShape(14.dp))
            ) {
                if (route.segments.isEmpty() || route.segments.all { it.isEmpty() }) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Nincs kirajzolható koordináta", color = colors.muted, fontSize = 12.sp)
                    }
                } else {
                    MercatorRouteCanvas(segments = route.segments)
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RouteLegendItem(color = Color(0xFF30DFF2), label = "Rajt")
                RouteLegendItem(color = Color(0xFF3BE68A), label = "Nyomvonal")
                RouteLegendItem(color = Color(0xFFFFB93D), label = "Cél")
            }
        }
    }
}

@Composable
fun MercatorRouteCanvas(segments: List<List<Pair<Double, Double>>>) {
    val allPoints = segments.flatten()
    if (allPoints.size < 2) return

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
                color = Color(0xFF3BE68A),
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }

        // Rajt pont (#30DFF2)
        val startOffset = toCanvasOffset(allPoints.first())
        drawCircle(color = Color(0xFF30DFF2), radius = 6.dp.toPx(), center = startOffset)

        // Cél pont (#FFB93D)
        val finishOffset = toCanvasOffset(allPoints.last())
        drawCircle(color = Color(0xFFFFB93D), radius = 6.dp.toPx(), center = finishOffset)
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
