package com.repforge.app;

import android.os.Bundle;
import android.view.View;
import androidx.activity.OnBackPressedCallback;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    private StartupScreen startupScreen;
    @Override public void onCreate(Bundle savedInstanceState) {
        startupScreen = StartupScreen.install(this);
        registerPlugin(NativeFilesPlugin.class);
        registerPlugin(BackupArchivePlugin.class);
        registerPlugin(GoogleSyncPlugin.class);
        registerPlugin(WorkoutPhotosPlugin.class);
        registerPlugin(HealthBridgePlugin.class);
        registerPlugin(HealthJournalPlugin.class);
        registerPlugin(HealthBackgroundPlugin.class);
        registerPlugin(DistanceTrackerPlugin.class);
        registerPlugin(AppFeedbackPlugin.class);
        registerPlugin(BleDiscoveryPlugin.class);
        super.onCreate(savedInstanceState);
        applySystemTextSize();
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (getBridge() == null || getBridge().getWebView() == null) {
                    finish();
                    return;
                }
                String js = "(function(){try{return !!(window.TrainPilotAndroidBack&&window.TrainPilotAndroidBack());}catch(e){return false;}})()";
                getBridge().getWebView().evaluateJavascript(js, value -> {
                    if (!"true".equals(value)) {
                        finish();
                    }
                });
            }
        });

        View content = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(content, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
        ViewCompat.requestApplyInsets(content);
        startupScreen.attach();
    }

    @Override public void onResume() {
        super.onResume();
        applySystemTextSize();
        HealthBackgroundService.stop(this);
        try { HealthJournalStore.get(this).retireWatchUi(); } catch (Exception ignored) {}
        try { HealthBackgroundJob.schedule(this); } catch (Exception ignored) {}
    }

    private void applySystemTextSize() {
        if (getBridge() != null && getBridge().getWebView() != null) {
            float scale = getResources().getConfiguration().fontScale;
            getBridge().getWebView().getSettings().setTextZoom(Math.round(scale * 100));
        }
    }

    void finishStartup(boolean failed, Runnable complete) {
        if (startupScreen == null) { complete.run(); return; }
        startupScreen.ready(getBridge() == null ? null : getBridge().getWebView(), failed, complete);
    }

    @Override public void onDestroy() {
        if (startupScreen != null) startupScreen.destroy();
        super.onDestroy();
    }
}
