package com.repforge.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.view.View;
import android.view.animation.LinearInterpolator;

/** One full-window logo layer avoids the system icon's circular/SurfaceView crop. */
final class StartupFade {
    static final float INITIAL_SCALE = .55f;
    static final long EXIT_MS = 480;

    static float loadingScale(long elapsedMs) {
        float elapsed = Math.max(0, elapsedMs);
        // Keep growing while the WebView loads; never loop, reset or hold at a keyframe.
        return INITIAL_SCALE + 2.45f * elapsed / (elapsed + 2200f);
    }

    static float coveringScale(int width, int height, int logoSize) {
        // The visible icon card occupies half the padded vector's viewport.
        return Math.max(1f, Math.max(width, height) * 1.4f / (logoSize * .5f));
    }

    static float fadeAlpha(float progress) {
        float t = Math.max(0f, Math.min(1f, (progress - .55f) / .45f));
        return 1f - t * t * (3f - 2f * t);
    }

    static ValueAnimator start(LogoView overlay, Runnable remove, long durationMs) {
        float from = overlay.currentScale();
        float to = Math.max(from, coveringScale(overlay.getWidth(), overlay.getHeight(), overlay.logoSize));
        overlay.loading = false;
        final boolean[] removed = {false};
        Runnable finish = () -> {
            if (removed[0]) return;
            removed[0] = true;
            overlay.loading = false;
            remove.run();
        };
        ValueAnimator zoom = ValueAnimator.ofFloat(0f, 1f);
        zoom.setDuration(durationMs);
        zoom.setInterpolator(new LinearInterpolator());
        zoom.addUpdateListener(animation -> {
            float progress = (float) animation.getAnimatedValue();
            overlay.scale = from * (float) Math.pow(to / from, progress);
            overlay.setAlpha(fadeAlpha(progress));
            overlay.invalidate();
        });
        zoom.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) { finish.run(); }
            @Override public void onAnimationCancel(Animator animation) { finish.run(); }
        });
        zoom.start();
        return zoom;
    }

    static final class LogoView extends View {
        final int logoSize;
        private Bitmap logo;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final RectF bounds;
        private long startedAt;
        private boolean loading;
        float scale = INITIAL_SCALE;
        private float centerX = Float.NaN, centerY = Float.NaN;

        LogoView(Context context, Drawable drawable, int size) {
            super(context);
            logoSize = Math.max(1, size);
            bounds = new RectF(-logoSize / 2f, -logoSize / 2f, logoSize / 2f, logoSize / 2f);
            // Rasterize once. VectorDrawable otherwise reallocates its scaled cache
            // each zoom frame; one filtered GPU texture keeps the motion inexpensive.
            int pixels = Math.min(2048, logoSize * 2);
            logo = Bitmap.createBitmap(pixels, pixels, Bitmap.Config.ARGB_8888);
            drawable.setBounds(0, 0, pixels, pixels);
            drawable.draw(new Canvas(logo));
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            setClickable(true);
        }

        void beginLoading(float x, float y) {
            centerX = x; centerY = y;
            startedAt = SystemClock.uptimeMillis();
            loading = true;
            invalidate();
        }

        float currentScale() {
            if (loading) scale = loadingScale(SystemClock.uptimeMillis() - startedAt);
            return scale;
        }

        void stop() { loading = false; logo = null; }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawColor(0xff0e1015);
            float size = currentScale();
            canvas.save();
            canvas.translate(Float.isNaN(centerX) ? getWidth() / 2f : centerX,
                    Float.isNaN(centerY) ? getHeight() / 2f : centerY);
            canvas.scale(size, size);
            if (logo != null) canvas.drawBitmap(logo, null, bounds, paint);
            canvas.restore();
            if (loading) postInvalidateOnAnimation();
        }
    }
}
