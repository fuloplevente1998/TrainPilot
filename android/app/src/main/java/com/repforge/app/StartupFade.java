package com.repforge.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.os.Build;
import android.view.SurfaceControl;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/** Animated splash icons can live on a separate surface: parent alpha does not fade it. */
final class StartupFade {
    static ValueAnimator start(View background, View icon, Runnable remove, long durationMs) {
        SurfaceControl surface = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && icon instanceof SurfaceView) {
            surface = ((SurfaceView) icon).getSurfaceControl();
            if (surface != null && !surface.isValid()) surface = null;
        }
        final SurfaceControl iconSurface = surface;
        final SurfaceControl.Transaction transaction = iconSurface != null
                ? new SurfaceControl.Transaction() : null;
        final boolean[] removed = {false};
        Runnable finish = () -> {
            if (removed[0]) return;
            removed[0] = true;
            if (transaction != null) transaction.close();
            remove.run();
        };
        ValueAnimator fade = ValueAnimator.ofFloat(1f, 0f);
        fade.setDuration(durationMs);
        fade.setInterpolator(new DecelerateInterpolator());
        fade.addUpdateListener(animation -> {
            float alpha = (float) animation.getAnimatedValue();
            background.setAlpha(alpha);
            if (icon != null) {
                icon.setAlpha(alpha);
                float scale = 1f + (1f - alpha) * .08f;
                icon.setScaleX(scale);
                icon.setScaleY(scale);
            }
            // SurfaceView.setAlpha is ignored on older Android versions. Update its
            // compositor layer too, so the icon cannot remain opaque over the app.
            if (transaction != null && iconSurface.isValid()) {
                try { transaction.setAlpha(iconSurface, alpha).apply(); }
                catch (RuntimeException ignored) { /* A detached surface is removed below. */ }
            }
        });
        fade.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) { finish.run(); }
            @Override public void onAnimationCancel(Animator animation) { finish.run(); }
        });
        fade.start();
        return fade;
    }
}
