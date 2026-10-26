package com.repforge.app.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.repforge.app.R
import com.repforge.app.data.store.TrainPilotUserSettings
import com.repforge.app.ui.settings.TrainingProfileDialog
import com.repforge.app.ui.text.nativeResources
import com.repforge.app.ui.theme.LocalTrainPilotColors

/**
 * The only first-launch surface. Existing Settings profile dialog performs field
 * editing and the canonical NativeTrainingProfile validator performs saving.
 * No training, program generation, backup or workout state is changed here.
 */
@Composable
internal fun NativeOnboardingScreen(
    settings: TrainPilotUserSettings,
    destination: NativeOnboardingPolicy.Destination,
    saving: Boolean,
    failed: Boolean,
    onBegin: () -> Unit,
    onSkip: () -> Unit,
    onProfileSave: (Map<String, String>) -> Unit,
    onBack: () -> Unit
) {
    val colors = LocalTrainPilotColors.current
    val context = LocalContext.current
    val strings = remember(context, settings.language) { nativeResources(context, settings.language) }
    var profileOpen by remember(destination, settings.profileJson) { mutableStateOf(false) }

    BackHandler {
        when {
            profileOpen && !saving -> profileOpen = false
            !saving -> onBack()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(colors.bg)
            .verticalScroll(rememberScrollState()).padding(24.dp)
            .testTag("onboarding_screen"),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(Modifier.heightIn(min = 20.dp))
        Text(
            text = strings.getString(if (destination == NativeOnboardingPolicy.Destination.WELCOME)
                R.string.native_onboarding_welcome else R.string.native_onboarding_profile),
            fontSize = 26.sp, fontWeight = FontWeight.Bold, color = colors.text,
            modifier = Modifier.testTag(if (destination == NativeOnboardingPolicy.Destination.WELCOME)
                "onboarding_welcome" else "onboarding_profile")
        )
        Text(
            text = strings.getString(if (destination == NativeOnboardingPolicy.Destination.WELCOME)
                R.string.native_onboarding_intro else R.string.native_onboarding_profile_intro),
            color = colors.muted, fontSize = 16.sp
        )
        if (failed) Text(strings.getString(R.string.native_onboarding_error),
            modifier = Modifier.testTag("onboarding_error"), color = colors.dangerText)
        if (saving) CircularProgressIndicator(modifier = Modifier.testTag("onboarding_saving"))
        Button(
            onClick = {
                if (destination == NativeOnboardingPolicy.Destination.WELCOME) onBegin()
                else profileOpen = true
            },
            enabled = !saving,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag(
                if (destination == NativeOnboardingPolicy.Destination.WELCOME)
                    "onboarding_begin" else "onboarding_edit_profile"),
            colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
        ) {
            Text(strings.getString(if (destination == NativeOnboardingPolicy.Destination.WELCOME)
                R.string.native_onboarding_begin else R.string.native_onboarding_edit),
                color = colors.onAccent)
        }
        OutlinedButton(
            onClick = onSkip, enabled = !saving,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .testTag("onboarding_skip")
        ) { Text(strings.getString(R.string.native_onboarding_skip), color = colors.text) }
        if (destination == NativeOnboardingPolicy.Destination.PROFILE)
            Text(strings.getString(R.string.native_onboarding_resume_info), color = colors.muted,
                fontSize = 14.sp)
    }

    if (profileOpen) {
        TrainingProfileDialog(
            profileJson = settings.profileJson, language = settings.language,
            saving = saving, failed = failed,
            onSave = onProfileSave, onDismiss = { if (!saving) profileOpen = false }
        )
    }
}
