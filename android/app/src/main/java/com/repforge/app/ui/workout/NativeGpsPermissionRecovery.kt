package com.repforge.app.ui.workout

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.repforge.app.ActiveWorkoutStore
import com.repforge.app.NativeWorkoutTarget
import com.repforge.app.R
import com.repforge.app.ui.text.nativeResources
import com.repforge.app.ui.theme.LocalTrainPilotColors
import com.repforge.shared.WorkoutOwnership193
import org.json.JSONObject

/** A permission grant and the phone's location switch are independent checks. */
internal enum class NativeGpsPermissionBlocker {
    READY, DENIED, APPROXIMATE, PERMANENTLY_DENIED, LOCATION_OFF
}

internal data class NativeGpsRecoveryPrompt(
    val target: NativeWorkoutTarget,
    val blocker: NativeGpsPermissionBlocker
)

internal object NativeGpsPermissionRecovery {
    fun decide(
        precise: Boolean,
        approximate: Boolean,
        gpsEnabled: Boolean,
        requestedBefore: Boolean,
        rationale: Boolean
    ): NativeGpsPermissionBlocker = when {
        !precise && approximate -> NativeGpsPermissionBlocker.APPROXIMATE
        !precise && requestedBefore && !rationale -> NativeGpsPermissionBlocker.PERMANENTLY_DENIED
        !precise -> NativeGpsPermissionBlocker.DENIED
        !gpsEnabled -> NativeGpsPermissionBlocker.LOCATION_OFF
        else -> NativeGpsPermissionBlocker.READY
    }

    fun read(context: Context, activity: Activity?, requestedBefore: Boolean): NativeGpsPermissionBlocker {
        val precise = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val approximate = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val gpsEnabled = runCatching {
            (context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager)
                ?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
        }.getOrDefault(false)
        val rationale = activity?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.ACCESS_FINE_LOCATION)
        } ?: true
        return decide(precise, approximate, gpsEnabled, requestedBefore, rationale)
    }

    /** Read-only admission: the original workout, generation, exercise and set must still exist. */
    fun targetIsCurrent(frame: JSONObject?, target: NativeWorkoutTarget, isFinishing: Boolean): Boolean =
        if (frame == null || isFinishing || !frame.optBoolean("active") ||
            frame.optBoolean("finalizationPending") || frame.optBoolean("workoutPaused") ||
            frame.optString("timerOwner", "phone") != "phone" ||
            WorkoutOwnership193.pending(frame) || frame.optInt("nativeEditorProjection") != 1
        ) false else runCatching {
            val resolved = target.resolve(frame)
            resolved.exerciseIndex == frame.optInt("currentExercise") &&
                !resolved.set.optBoolean("done")
        }.getOrDefault(false)

    fun settingsIntent(context: Context, blocker: NativeGpsPermissionBlocker): Intent? = when (blocker) {
        NativeGpsPermissionBlocker.LOCATION_OFF ->
            Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        NativeGpsPermissionBlocker.APPROXIMATE, NativeGpsPermissionBlocker.PERMANENTLY_DENIED ->
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
        else -> null
    }
}

@Composable
internal fun NativeGpsPermissionRecoveryDialog(
    blocker: NativeGpsPermissionBlocker,
    language: String,
    onRetry: () -> Unit,
    onSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalTrainPilotColors.current
    val context = LocalContext.current
    val strings = nativeResources(context, language)
    val message = when (blocker) {
        NativeGpsPermissionBlocker.READY -> R.string.native_gps_recovery_ready
        NativeGpsPermissionBlocker.DENIED -> R.string.native_gps_recovery_denied
        NativeGpsPermissionBlocker.APPROXIMATE -> R.string.native_gps_recovery_approximate
        NativeGpsPermissionBlocker.PERMANENTLY_DENIED -> R.string.native_gps_recovery_permanent
        NativeGpsPermissionBlocker.LOCATION_OFF -> R.string.native_gps_recovery_location_off
    }
    AlertDialog(
        modifier = Modifier.testTag("gps_recovery_dialog"),
        onDismissRequest = onDismiss,
        title = { Text(strings.getString(R.string.native_gps_recovery_title), color = colors.text) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(strings.getString(message), modifier = Modifier.testTag("gps_recovery_reason"),
                    color = colors.text)
                if (NativeGpsPermissionRecovery.settingsIntent(context, blocker) != null) {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("gps_recovery_settings"),
                        onClick = onSettings
                    ) { Text(strings.getString(if (blocker == NativeGpsPermissionBlocker.LOCATION_OFF)
                        R.string.native_gps_recovery_location_settings else R.string.native_gps_recovery_app_settings)) }
                }
            }
        },
        confirmButton = {
            Button(onClick = onRetry, modifier = Modifier.heightIn(min = 48.dp).testTag("gps_recovery_retry")) {
                Text(strings.getString(if (blocker == NativeGpsPermissionBlocker.DENIED)
                    R.string.native_gps_recovery_request else R.string.native_gps_recovery_retry))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp).testTag("gps_recovery_cancel")) {
                Text(strings.getString(R.string.native_gps_recovery_cancel))
            }
        },
        containerColor = colors.card
    )
}
