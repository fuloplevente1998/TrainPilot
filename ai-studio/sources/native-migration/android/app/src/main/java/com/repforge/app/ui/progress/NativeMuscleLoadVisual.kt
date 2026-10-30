package com.repforge.app.ui.progress

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.repforge.app.R
import com.repforge.app.ui.components.TrainPilotSectionSurface
import com.repforge.app.ui.text.nativeResources
import com.repforge.app.ui.theme.LocalTrainPilotColors
import com.repforge.app.ui.theme.TrainPilotExtendedColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Same relative-load levels as main 7473b30 app.js:14226; never a fatigue estimate. */
internal fun muscleLoadColor(ratio: Float, colors: TrainPilotExtendedColors): Color = when {
    ratio * 100f <= 33f -> colors.loadLow
    ratio * 100f <= 66f -> colors.loadMedium
    else -> colors.loadHigh
}

/** Main's grayscale masks carry intensity, not alpha. Convert that intensity only while painting. */
private fun maskTint(color: Color) = ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
    0f, 0f, 0f, 0f, color.red * 255f,
    0f, 0f, 0f, 0f, color.green * 255f,
    0f, 0f, 0f, 0f, color.blue * 255f,
    1f, 0f, 0f, 0f, 0f
)))

private val anatomyResources = mapOf(
    "base" to R.drawable.progress_anatomy_main,
    "arms" to R.drawable.progress_muscle_arms_main,
    "back" to R.drawable.progress_muscle_back_main,
    "chest" to R.drawable.progress_muscle_chest_main,
    "core" to R.drawable.progress_muscle_core_main,
    "legs" to R.drawable.progress_muscle_legs_main,
    "shoulders" to R.drawable.progress_muscle_shoulders_main
)

@Composable
internal fun NativeMuscleLoadVisual(loads: List<MuscleLoadUiModel>, language: String,
    modifier: Modifier = Modifier) {
    var zoom by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val strings = remember(context, language) { nativeResources(context, language) }
    val label = strings.getString(R.string.native_visual_anatomy_open)
    AnatomyPaint(loads, language, modifier.heightIn(min = 48.dp).testTag("progress_anatomy")
        .semantics { contentDescription = label }
        .clickable(role = Role.Button, onClickLabel = label) { zoom = true })
    if (zoom) Dialog(onDismissRequest = { zoom = false },
        properties = DialogProperties(usePlatformDefaultWidth = false)) {
        TrainPilotSectionSurface(Modifier.fillMaxWidth().padding(16.dp).testTag("progress_anatomy_dialog")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = { zoom = false }, modifier = Modifier.size(48.dp).testTag("progress_anatomy_close")) {
                    Icon(Icons.Default.Close, strings.getString(R.string.journal_close))
                }
            }
            AnatomyPaint(loads, language, Modifier.fillMaxWidth())
            Text(strings.getString(R.string.native_visual_load_note),
                color = LocalTrainPilotColors.current.muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun AnatomyPaint(loads: List<MuscleLoadUiModel>, language: String, modifier: Modifier) {
    val context = LocalContext.current
    val colors = LocalTrainPilotColors.current
    val strings = remember(context, language) { nativeResources(context, language) }
    BoxWithConstraints(modifier.aspectRatio(1.5f)) {
        val target = with(LocalDensity.current) { maxWidth.roundToPx() }.coerceIn(1, 768)
        val images by produceState<Map<String, ImageBitmap>?>(null, context.resources, target) {
            value = withContext(Dispatchers.IO) {
                anatomyResources.mapValues { (_, resource) ->
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeResource(context.resources, resource, bounds)
                    var sample = 1
                    while (bounds.outWidth / (sample * 2) >= target) sample *= 2
                    val options = BitmapFactory.Options().apply { inSampleSize = sample; inScaled = false }
                    BitmapFactory.decodeResource(context.resources, resource, options)?.asImageBitmap()
                }.mapNotNull { (key, bitmap) -> bitmap?.let { key to it } }.toMap()
            }
        }
        val base = images?.get("base")
        if (base == null) Text(strings.getString(if (images == null) R.string.native_visual_anatomy_loading else R.string.native_progress_error),
            Modifier.align(Alignment.Center).testTag("progress_anatomy_loading"),
            color = colors.muted, style = MaterialTheme.typography.bodySmall)
        else {
            val gray = remember { ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) }
            Image(base, null, Modifier.fillMaxSize().testTag("progress_anatomy_loaded"),
                contentScale = ContentScale.Fit, colorFilter = gray)
            for (load in loads) if (load.setsCount > 0 && load.loadPercentage.isFinite()) {
                images?.get(load.muscleKey)?.let { bitmap ->
                    Image(bitmap, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit,
                        colorFilter = maskTint(muscleLoadColor(load.loadPercentage, colors)))
                }
            }
        }
    }
}
