package com.repforge.app.ui.navigation

import android.view.View
import android.widget.ImageView
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.repforge.app.R

/**
 * One brand symbol for the native TrainPilot shell: the ORIGINAL gold dumbbell
 * from the installed application's adaptive launcher foreground.
 *
 * Do not substitute the AI Studio mountain logo, the material FitnessCenter icon,
 * or another hand-drawn barbell. This intentionally renders the exact launcher
 * vector resource (including its original gold gradient and plate proportions).
 * Android ImageView handles its aapt gradient vector without rasterizing/repainting.
 */
@Composable
internal fun TrainPilotBrandLogo(modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                setImageResource(R.drawable.trainpilot_launcher_foreground)
                scaleType = ImageView.ScaleType.FIT_CENTER
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                isClickable = false
                isFocusable = false
            }
        },
        modifier = modifier.size(48.dp).testTag("trainpilot_brand_dumbbell")
    )
}
