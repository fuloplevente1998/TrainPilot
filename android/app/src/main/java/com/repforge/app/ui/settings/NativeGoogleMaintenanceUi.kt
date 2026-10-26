package com.repforge.app.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import com.repforge.app.R
import com.repforge.app.ui.text.nativeResources
import com.repforge.app.ui.theme.LocalTrainPilotColors

private fun title(kind: NativeGoogleMaintenance.Kind) = when (kind) {
    NativeGoogleMaintenance.Kind.REVOKE -> R.string.google_maintenance_revoke
    NativeGoogleMaintenance.Kind.DRIVE -> R.string.google_maintenance_drive
    NativeGoogleMaintenance.Kind.CALENDAR -> R.string.google_maintenance_calendar
}
private fun scope(kind: NativeGoogleMaintenance.Kind) = when (kind) {
    NativeGoogleMaintenance.Kind.REVOKE -> R.string.google_maintenance_revoke_scope
    NativeGoogleMaintenance.Kind.DRIVE -> R.string.google_maintenance_drive_scope
    NativeGoogleMaintenance.Kind.CALENDAR -> R.string.google_maintenance_calendar_scope
}

@Composable
internal fun NativeGoogleMaintenanceControls(language: String, busy: Boolean,
    open: (NativeGoogleMaintenance.Kind) -> Unit) {
    val strings = nativeResources(LocalContext.current, language)
    val colors = LocalTrainPilotColors.current
    Column {
        for (kind in NativeGoogleMaintenance.Kind.values()) {
            TextButton(onClick = { open(kind) }, enabled = !busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("google_maintenance_${kind.key}")) {
                Text(strings.getString(title(kind)), color = colors.dangerText)
            }
        }
    }
}

@Composable
internal fun NativeGoogleMaintenanceConfirmation(request: NativeGoogleMaintenance.Request?, language: String,
    busy: Boolean, cancel: () -> Unit, confirm: () -> Unit) {
    if (request == null) return
    val strings = nativeResources(LocalContext.current, language)
    AlertDialog(onDismissRequest = { if (!busy) cancel() }, title = { Text(strings.getString(title(request.kind))) },
        text = { Column {
            Text(strings.getString(R.string.google_maintenance_account, request.email))
            Text(strings.getString(scope(request.kind)))
            Text(strings.getString(R.string.google_maintenance_local))
        } }, confirmButton = {
            TextButton(onClick = confirm, enabled = !busy, modifier = Modifier.heightIn(min = 48.dp).testTag("google_maintenance_confirm")) {
                Text(strings.getString(R.string.google_maintenance_confirm))
            }
        }, dismissButton = {
            TextButton(onClick = cancel, enabled = !busy, modifier = Modifier.heightIn(min = 48.dp).testTag("google_maintenance_cancel")) {
                Text(strings.getString(R.string.google_maintenance_cancel))
            }
        })
}

@Composable
internal fun NativeGoogleMaintenanceFeedback(language: String, outcome: String, count: Int) {
    if (outcome.isEmpty()) return
    val strings = nativeResources(LocalContext.current, language)
    val key = when (outcome) {
        "done" -> R.string.google_maintenance_done
        "partial" -> R.string.google_maintenance_partial
        else -> R.string.google_maintenance_failed
    }
    Text(strings.getString(key, count), color = LocalTrainPilotColors.current.text,
        modifier = Modifier.testTag("google_maintenance_result"))
}
