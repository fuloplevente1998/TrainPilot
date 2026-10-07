package com.repforge.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.FrameMetrics;
import android.view.View;
import android.view.Window;
import android.view.animation.LinearInterpolator;

/** One full-window logo layer avoids the system icon's circular/SurfaceView crop. */
final class StartupFade {
    static final float INITIAL_SCALE = .55f;
    static final long EXIT_MS = 480;

    /**
     * TEMPORARY measurement switch (logcat tag "StartupFade"). Logs update/draw gaps and,
     * on API 24+, per-frame FrameMetrics for the exit. Set to false once the cause is known.
     */
    static final boolean TRACE = false;
    private static final String TAG = "StartupFade";

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
        final boolean paced = overlay.isAttachedToWindow() && overlay.isHardwareAccelerated();
        float from = paced && !Float.isNaN(overlay.drawnScale)
                ? overlay.drawnScale : overlay.currentScale();
        overlay.scale = from;
        float to = Math.max(from, coveringScale(overlay.getWidth(), overlay.getHeight(), overlay.logoSize));
        overlay.loading = false;
        final Trace trace = TRACE ? Trace.create(overlay.getContext(), from, to, overlay) : null;
        overlay.trace = trace;
        final boolean[] removed = {false};
        Runnable finish = () -> {
            if (removed[0]) return;
            removed[0] = true;
            overlay.loading = false;
            if (trace != null) trace.end();
            remove.run();
        };
        ValueAnimator zoom = ValueAnimator.ofFloat(0f, 1f);
        zoom.setDuration(durationMs);
        zoom.setInterpolator(new LinearInterpolator());
        // A wall-clock animator can finish while WebView startup prevents any draws.
        // On a real window, consume time only after the previous state was drawn and
        // cap catch-up to two 60 Hz frames. Keep the unattached/software path seekable.
        if (trace != null) Trace.log("draw-paced exit=" + paced);
        final long[] lastTick = {SystemClock.uptimeMillis()};
        final float[] elapsed = {0f};
        overlay.frameDrawn = true;
        if (paced) zoom.setRepeatCount(ValueAnimator.INFINITE);
        zoom.addUpdateListener(animation -> {
            if (removed[0]) return;
            float progress = (float) animation.getAnimatedValue();
            if (paced) {
                long now = SystemClock.uptimeMillis();
                long delta = Math.max(0, now - lastTick[0]);
                lastTick[0] = now;
                if (!overlay.frameDrawn) return;
                elapsed[0] += Math.min(32L, delta);
                progress = Math.min(1f, elapsed[0] / Math.max(1L, durationMs));
                overlay.frameDrawn = false;
                if (progress >= 1f) overlay.finishAfterDraw = zoom::end;
            }
            overlay.scale = from * (float) Math.pow(to / from, progress);
            overlay.opacity = fadeAlpha(progress);
            if (trace != null) trace.update(progress);
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
        private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
        private final Paint background = new Paint();
        private final RectF bounds = new RectF();
        // Invisible warm-up sample: a 2x2 patch of the opaque padding colour drawn into one
        // pixel at alpha 254 over the identical background colour. It makes the GPU create
        // the translucent bitmap pipeline/shader during loading, not on the first fade frame.
        private final Rect warmSrc = new Rect(0, 0, 2, 2);
        private final RectF warmDst = new RectF(0, 0, 1, 1);
        private long startedAt;
        private boolean loading;
        private float drawnScale = Float.NaN;
        private boolean frameDrawn;
        private Runnable finishAfterDraw;
        Trace trace;
        float scale = INITIAL_SCALE;
        float opacity = 1f;
        private float centerX = Float.NaN, centerY = Float.NaN;

        LogoView(Context context, Drawable drawable, int size) {
            super(context);
            logoSize = Math.max(1, size);
            // Rasterize once. VectorDrawable otherwise reallocates its scaled cache
            // each zoom frame; one filtered GPU texture keeps the motion inexpensive.
            int pixels = Math.min(2048, logoSize * 2);
            logo = Bitmap.createBitmap(pixels, pixels, Bitmap.Config.ARGB_8888);
            drawable.setBounds(0, 0, pixels, pixels);
            Canvas texture = new Canvas(logo);
            // Opaque padding lets logo and surrounding background cover disjoint
            // regions, so each pixel receives the fade alpha exactly once.
            texture.drawColor(0xff0e1015);
            drawable.draw(texture);
            background.setColor(0xff0e1015);
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

        void stop() {
            loading = false;
            finishAfterDraw = null;
            logo = null;
        }

        @Override public boolean hasOverlappingRendering() { return false; }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            final Trace t = trace;
            final long drawStart = t != null ? System.nanoTime() : 0L;
            drawnScale = currentScale();
            float half = logoSize * drawnScale / 2f;
            float x = Float.isNaN(centerX) ? getWidth() / 2f : centerX;
            float y = Float.isNaN(centerY) ? getHeight() / 2f : centerY;
            bounds.set(x - half, y - half, x + half, y + half);
            int alpha = Math.round(255 * opacity);
            paint.setAlpha(alpha);
            background.setAlpha(alpha);
            float width = getWidth(), height = getHeight();
            float left = Math.max(0, Math.min(width, bounds.left));
            float right = Math.max(0, Math.min(width, bounds.right));
            float top = Math.max(0, Math.min(height, bounds.top));
            float bottom = Math.max(0, Math.min(height, bounds.bottom));
            // Fade drawing operations directly. View alpha would allocate/repaint
            // a full-window intermediate layer at the first translucent frame.
            canvas.drawRect(0, 0, width, top, background);
            canvas.drawRect(0, bottom, width, height, background);
            canvas.drawRect(0, top, left, bottom, background);
            canvas.drawRect(right, top, width, bottom, background);
            if (logo != null) canvas.drawBitmap(logo, null, bounds, paint);
            if (logo != null && opacity >= 1f) {
                // Drawn last, over opaque identical colour: pixel result is unchanged.
                paint.setAlpha(254);
                canvas.drawBitmap(logo, warmSrc, warmDst, paint);
            }
            frameDrawn = true;
            if (finishAfterDraw != null) {
                Runnable finish = finishAfterDraw;
                finishAfterDraw = null;
                // Submit the transparent endpoint before removing the view and releasing
                // startup completion work. Cancellation still removes immediately.
                postOnAnimation(finish);
            }
            if (t != null) t.draw(System.nanoTime() - drawStart);
            if (loading) postInvalidateOnAnimation();
        }
    }

    /** Temporary, failure-proof measurement helper; never throws into the animation. */
    static final class Trace {
        private final long startMs = SystemClock.uptimeMillis();
        private final Handler handler;
        private FrameTrace frames;
        private long lastUpdateMs = -1, lastDrawMs = -1;
        private long maxUpdateGap, maxDrawGap, maxDrawNs;
        private int updates, draws;
        private boolean fadeLogged, ended;

        private Trace(Handler handler) { this.handler = handler; }

        static Trace create(Context context, float from, float to, LogoView overlay) {
            try {
                Trace trace = new Trace(new Handler(Looper.getMainLooper()));
                if (Build.VERSION.SDK_INT >= 24) trace.frames = FrameTrace.attach(context);
                log("exit start from=" + from + " to=" + to + " view=" + overlay.getWidth() + "x"
                        + overlay.getHeight() + " logoSize=" + overlay.logoSize + " sdk=" + Build.VERSION.SDK_INT);
                return trace;
            } catch (RuntimeException e) {
                return null;
            }
        }

        static void log(String message) {
            try { Log.d(TAG, message); } catch (RuntimeException ignored) { }
        }

        void update(float progress) {
            try {
                long now = SystemClock.uptimeMillis();
                updates++;
                if (lastUpdateMs >= 0) {
                    long gap = now - lastUpdateMs;
                    maxUpdateGap = Math.max(maxUpdateGap, gap);
                    if (gap > 24) log("update gap " + gap + "ms at +" + (now - startMs) + "ms p=" + progress);
                }
                lastUpdateMs = now;
                if (!fadeLogged && progress >= .55f) {
                    fadeLogged = true;
                    log("fade begins at +" + (now - startMs) + "ms");
                }
            } catch (RuntimeException ignored) { }
        }

        void draw(long nanos) {
            try {
                long now = SystemClock.uptimeMillis();
                draws++;
                maxDrawNs = Math.max(maxDrawNs, nanos);
                if (lastDrawMs >= 0) {
                    long gap = now - lastDrawMs;
                    maxDrawGap = Math.max(maxDrawGap, gap);
                    if (gap > 24) log("draw gap " + gap + "ms at +" + (now - startMs) + "ms");
                }
                lastDrawMs = now;
            } catch (RuntimeException ignored) { }
        }

        void end() {
            try {
                if (ended) return;
                ended = true;
                log("finish at +" + (SystemClock.uptimeMillis() - startMs) + "ms updates=" + updates
                        + " draws=" + draws + " maxUpdateGap=" + maxUpdateGap + "ms maxDrawGap="
                        + maxDrawGap + "ms maxOnDraw=" + (maxDrawNs / 1000) + "us");
                final FrameTrace f = frames;
                if (f != null) handler.postDelayed(f::detach, 400);
            } catch (RuntimeException ignored) { }
        }
    }

    /** Per-frame UI/RenderThread/GPU timings for the exit (API 24+; GPU time on API 31+). */
    @TargetApi(24)
    static final class FrameTrace implements Window.OnFrameMetricsAvailableListener {
        private final Window window;
        private final long startNs = System.nanoTime();
        private final HandlerThread worker;
        private boolean detached;
        private int count;

        private FrameTrace(Window window, HandlerThread worker) {
            this.window = window;
            this.worker = worker;
        }

        static FrameTrace attach(Context context) {
            Window window = findWindow(context);
            if (window == null) return null;
            HandlerThread worker = new HandlerThread("StartupFadeMetrics");
            worker.start();
            FrameTrace trace = new FrameTrace(window, worker);
            try {
                window.addOnFrameMetricsAvailableListener(trace, new Handler(worker.getLooper()));
            } catch (RuntimeException e) {
                worker.quitSafely();
                return null;
            }
            return trace;
        }

        private static Window findWindow(Context context) {
            Context c = context;
            for (int i = 0; i < 8 && c != null; i++) {
                if (c instanceof Activity) return ((Activity) c).getWindow();
                if (!(c instanceof ContextWrapper)) return null;
                c = ((ContextWrapper) c).getBaseContext();
            }
            return null;
        }

        void detach() {
            // Removal and logging share the metrics thread, including queued reports.
            new Handler(worker.getLooper()).post(() -> {
                if (detached) return;
                detached = true;
                try { window.removeOnFrameMetricsAvailableListener(this); }
                catch (RuntimeException ignored) { }
                finally { worker.quitSafely(); }
                Trace.log("frames reported=" + count);
            });
        }

        private static String ms(long nanos) {
            return nanos < 0 ? "n/a" : String.valueOf(Math.round(nanos / 100000f) / 10f);
        }

        @Override public void onFrameMetricsAvailable(Window w, FrameMetrics fm, int droppedFrames) {
            try {
                if (detached) return;
                count++;
                long timestamp = Build.VERSION.SDK_INT >= 26
                        ? fm.getMetric(FrameMetrics.VSYNC_TIMESTAMP) : -1;
                long at = ((timestamp >= 0 ? timestamp : System.nanoTime()) - startNs) / 1000000L;
                StringBuilder sb = new StringBuilder("frame +").append(at).append("ms total=")
                        .append(ms(fm.getMetric(FrameMetrics.TOTAL_DURATION)))
                        .append(" layout=").append(ms(fm.getMetric(FrameMetrics.LAYOUT_MEASURE_DURATION)))
                        .append(" draw=").append(ms(fm.getMetric(FrameMetrics.DRAW_DURATION)))
                        .append(" sync=").append(ms(fm.getMetric(FrameMetrics.SYNC_DURATION)))
                        .append(" cmd=").append(ms(fm.getMetric(FrameMetrics.COMMAND_ISSUE_DURATION)))
                        .append(" swap=").append(ms(fm.getMetric(FrameMetrics.SWAP_BUFFERS_DURATION)));
                if (Build.VERSION.SDK_INT >= 31) sb.append(" gpu=").append(ms(fm.getMetric(FrameMetrics.GPU_DURATION)));
                sb.append(" droppedReports=").append(droppedFrames);
                Trace.log(sb.toString());
            } catch (RuntimeException ignored) { }
        }
    }
}
