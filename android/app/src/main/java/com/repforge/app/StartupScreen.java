package com.repforge.app;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import java.util.ArrayList;
import java.util.List;
import androidx.core.splashscreen.SplashScreen;

/** Release the system splash at the first native frame, then zoom while the WebView loads. */
final class StartupScreen {
    private final Activity activity;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean attached, started, ready, failed, removed;
    private StartupFade.LogoView overlay;
    private ValueAnimator exitAnimation;
    private StartupPaint paint;
    private final List<Runnable> completions = new ArrayList<>();
    private final Runnable fallback = () -> ready(true);

    static StartupScreen install(Activity activity) {
        return new StartupScreen(activity, SplashScreen.installSplashScreen(activity));
    }

    private StartupScreen(Activity activity, SplashScreen splash) {
        this.activity = activity;
        // Waiting for JS here would freeze the system logo throughout WebView startup.
        splash.setKeepOnScreenCondition(() -> !attached && !failed && !removed && !activity.isFinishing());
        splash.setOnExitAnimationListener(provider -> {
            if (removed || failed || !animationsEnabled()) {
                provider.remove();
                removeOverlay();
                return;
            }
            View icon = provider.getIconView();
            int[] origin = new int[2], location = new int[2];
            overlay.getLocationOnScreen(origin);
            icon.getLocationOnScreen(location);
            // Preserve the system logo's center during the hand-off, including OEM insets.
            overlay.beginLoading(location[0] - origin[0] + icon.getWidth() / 2f,
                    location[1] - origin[1] + icon.getHeight() / 2f);
            started = true;
            provider.remove();
            if (ready) finishZoom();
        });
        handler.postDelayed(fallback, 8000);
    }

    void attach() {
        if (attached || removed) return;
        ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();
        int size = Math.round(288 * activity.getResources().getDisplayMetrics().density);
        overlay = new StartupFade.LogoView(activity,
                activity.getDrawable(R.drawable.trainpilot_startup_logo), size);
        decor.addView(overlay, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        attached = true;
        decor.invalidate();
    }

    void ready(WebView webView, boolean startupFailed, Runnable complete) {
        if (removed) { complete.run(); return; }
        completions.add(complete);
        if (startupFailed || webView == null || !animationsEnabled()) ready(startupFailed);
        else if (!ready && paint == null) paint = StartupPaint.await(webView, () -> ready(false));
    }

    private void ready(boolean startupFailed) {
        if (ready || removed) return;
        failed = startupFailed;
        ready = true;
        handler.removeCallbacks(fallback);
        if (failed || !animationsEnabled()) removeOverlay();
        else if (started) finishZoom();
        activity.getWindow().getDecorView().invalidate();
    }

    private void finishZoom() {
        if (removed || exitAnimation != null) return;
        exitAnimation = StartupFade.start(overlay, this::removeOverlay, StartupFade.EXIT_MS);
    }

    private void removeOverlay() {
        if (removed) return;
        removed = true;
        handler.removeCallbacks(fallback);
        if (paint != null) paint.cancel();
        if (overlay != null) {
            overlay.stop();
            ViewGroup parent = (ViewGroup) overlay.getParent();
            if (parent != null) parent.removeView(overlay);
        }
        List<Runnable> finished = new ArrayList<>(completions);
        completions.clear();
        for (Runnable complete : finished) complete.run();
    }

    void destroy() {
        ready = true;
        handler.removeCallbacks(fallback);
        if (exitAnimation != null) exitAnimation.cancel();
        removeOverlay();
    }

    private boolean animationsEnabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) return ValueAnimator.areAnimatorsEnabled();
        return Settings.Global.getFloat(activity.getContentResolver(),
                Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f;
    }
}
