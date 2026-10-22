package com.repforge.app;

import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import androidx.activity.OnBackPressedCallback;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.getcapacitor.BridgeActivity;
import android.content.Context;
import android.os.SystemClock;
import android.util.Log;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends BridgeActivity {
    // Health sync holds the journal lock while committing/projection work runs.
    // Never wait for that lock (or SQLite disk I/O) on the input/render thread.
    private static final ExecutorService healthMaintenance = Executors.newSingleThreadExecutor();
    private StartupScreen startupScreen;
    private long startupStartedAt;
    private boolean startupLogged;
    @Override public void onCreate(Bundle savedInstanceState) {
        startupStartedAt = SystemClock.elapsedRealtime();
        startupScreen = StartupScreen.install(this);
        registerPlugin(NativeFilesPlugin.class);
        registerPlugin(BackupArchivePlugin.class);
        registerPlugin(GoogleSyncPlugin.class);
        registerPlugin(WorkoutPhotosPlugin.class);
        registerPlugin(HealthBridgePlugin.class);
        registerPlugin(SamsungHealthPlugin.class);
        registerPlugin(HealthJournalPlugin.class);
        registerPlugin(HealthBackgroundPlugin.class);
        registerPlugin(DistanceTrackerPlugin.class);
        registerPlugin(IntervalTimerPlugin.class);
        registerPlugin(AppFeedbackPlugin.class);
        registerPlugin(BleDiscoveryPlugin.class);
        registerPlugin(WearSyncPlugin.class);
        super.onCreate(savedInstanceState);
        applyWebViewDisplaySettings();
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
        applyWebViewDisplaySettings();
        HealthBackgroundService.stop(this);
        Context app = getApplicationContext();
        healthMaintenance.execute(() -> {
            try { HealthJournalStore.get(app).retireWatchUi(); } catch (Exception ignored) {}
            try { HealthBackgroundJob.schedule(app); } catch (Exception ignored) {}
        });
    }

    private void applyWebViewDisplaySettings() {
        if (getBridge() != null && getBridge().getWebView() != null) {
            WebSettings webSettings = getBridge().getWebView().getSettings();
            webSettings.setSupportZoom(false);
            webSettings.setBuiltInZoomControls(false);
            webSettings.setDisplayZoomControls(false);
            float scale = getResources().getConfiguration().fontScale;
            webSettings.setTextZoom(Math.round(scale * 100));
        }
    }

    void finishStartup(boolean failed, Runnable complete) {
        if (!startupLogged) {
            startupLogged = true;
            Log.i("TrainPilotStartup", "activityToJsReadyMs="
                    + Math.max(0, SystemClock.elapsedRealtime() - startupStartedAt)
                    + " failed=" + failed);
        }
        if (startupScreen == null) { complete.run(); return; }
        startupScreen.ready(getBridge() == null ? null : getBridge().getWebView(), failed, complete);
    }

    @Override public void onDestroy() {
        if (startupScreen != null) startupScreen.destroy();
        super.onDestroy();
    }
}
