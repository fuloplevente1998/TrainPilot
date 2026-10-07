package com.repforge.app;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/** Foreground interval cues and the user's system text size. No background alarms. */
@CapacitorPlugin(name = "AppFeedback")
public class AppFeedbackPlugin extends Plugin {
    @PluginMethod public void startupReady(PluginCall call) {
        getActivity().runOnUiThread(() -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).finishStartup(call.getBoolean("failed", false), call::resolve);
            } else call.resolve();
        });
    }

    @PluginMethod public void appearance(PluginCall call) {
        JSObject result = new JSObject();
        result.put("fontScale", getContext().getResources().getConfiguration().fontScale);
        call.resolve(result);
    }

    @PluginMethod public void signal(PluginCall call) {
        try {
            Vibrator vibrator;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager manager = (VibratorManager) getContext().getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                vibrator = manager == null ? null : manager.getDefaultVibrator();
            } else {
                vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
            }
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(90, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(90);
                }
            }
            call.resolve();
        } catch (SecurityException error) {
            call.reject("Vibration unavailable", error);
        }
    }
}
