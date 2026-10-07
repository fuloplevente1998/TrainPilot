package com.repforge.app;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import androidx.core.splashscreen.SplashScreen;

/** Keeps the launcher logo until the local WebView has rendered its first usable screen. */
final class StartupScreen {
    private final Activity activity;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean ready;
    private boolean failed;
    // If the JS/native bridge cannot signal readiness, reveal the web boot/retry screen.
    private final Runnable fallback = () -> ready(true);

    static StartupScreen install(Activity activity) {
        return new StartupScreen(activity, SplashScreen.installSplashScreen(activity));
    }

    private StartupScreen(Activity activity, SplashScreen splash) {
        this.activity = activity;
        splash.setKeepOnScreenCondition(() -> !ready && !activity.isFinishing());
        splash.setOnExitAnimationListener(provider -> {
            if (failed || !animationsEnabled()) {
                provider.remove();
                return;
            }
            View icon = provider.getIconView();
            if (icon != null) {
                icon.animate().scaleX(1.08f).scaleY(1.08f)
                        .setDuration(180).setInterpolator(new DecelerateInterpolator()).start();
            }
            provider.getView().animate().alpha(0f).setDuration(180)
                    .setInterpolator(new DecelerateInterpolator())
                    .withEndAction(provider::remove).start();
        });
        handler.postDelayed(fallback, 8000);
    }

    void ready(boolean startupFailed) {
        if (ready) return;
        failed = startupFailed;
        ready = true;
        handler.removeCallbacks(fallback);
        activity.getWindow().getDecorView().invalidate();
    }

    void destroy() {
        ready = true;
        handler.removeCallbacks(fallback);
    }

    private boolean animationsEnabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) return ValueAnimator.areAnimatorsEnabled();
        return Settings.Global.getFloat(activity.getContentResolver(),
                Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f;
    }
}
