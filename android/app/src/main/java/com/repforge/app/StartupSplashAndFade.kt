package com.repforge.app

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.FrameMetrics
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.LinearInterpolator
import android.webkit.WebView
import androidx.core.splashscreen.SplashScreen
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/** Kept for StartupPaintTest compatibility and optional WebView frame readiness. */
internal class StartupPaint {
    private var completed: Boolean = false
    private var cancelled: Boolean = false

    companion object {
        @JvmStatic
        fun await(webView: WebView, afterPaint: Runnable): StartupPaint {
            val wait = StartupPaint()
            val finish = Runnable {
                if (wait.completed || wait.cancelled) return@Runnable
                wait.completed = true
                afterPaint.run()
            }
            try {
                webView.postVisualStateCallback(0L, object : WebView.VisualStateCallback() {
                    override fun onComplete(requestId: Long) {
                        finish.run()
                    }
                })
            } catch (_: RuntimeException) {
                finish.run()
            }
            return wait
        }
    }

    fun cancel() {
        cancelled = true
    }
}

/** One full-window logo layer avoids the system icon's circular/SurfaceView crop. */
internal object StartupFade {
    const val INITIAL_SCALE: Float = 0.55f
    const val EXIT_MS: Long = 480L
    const val TRACE: Boolean = false
    private const val TAG: String = "StartupFade"

    @JvmStatic
    fun loadingScale(elapsedMs: Long): Float {
        val elapsed = max(0L, elapsedMs).toFloat()
        return INITIAL_SCALE + 2.45f * elapsed / (elapsed + 2200f)
    }

    @JvmStatic
    fun coveringScale(width: Int, height: Int, logoSize: Int): Float {
        return max(1f, max(width, height) * 1.4f / (logoSize * 0.5f))
    }

    @JvmStatic
    fun fadeAlpha(progress: Float): Float {
        val t = max(0f, min(1f, (progress - 0.55f) / 0.45f))
        return 1f - t * t * (3f - 2f * t)
    }

    @JvmStatic
    fun start(overlay: LogoView, remove: Runnable, durationMs: Long): ValueAnimator {
        val paced = overlay.isAttachedToWindow && overlay.isHardwareAccelerated
        val from = if (paced && !overlay.drawnScale.isNaN()) overlay.drawnScale else overlay.currentScale()
        overlay.scale = from
        val to = max(from, coveringScale(overlay.width, overlay.height, overlay.logoSize))
        overlay.loading = false
        val trace = if (TRACE) Trace.create(overlay.context, from, to, overlay) else null
        overlay.trace = trace
        val removed = booleanArrayOf(false)
        val finish = Runnable {
            if (removed[0]) return@Runnable
            removed[0] = true
            overlay.loading = false
            trace?.end()
            remove.run()
        }
        val zoom = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = durationMs
            interpolator = LinearInterpolator()
        }
        if (trace != null) Trace.log("draw-paced exit=$paced")
        val lastTick = longArrayOf(SystemClock.uptimeMillis())
        val elapsed = floatArrayOf(0f)
        overlay.frameDrawn = true
        if (paced) zoom.repeatCount = ValueAnimator.INFINITE
        zoom.addUpdateListener { animation ->
            if (removed[0]) return@addUpdateListener
            var progress = animation.animatedValue as Float
            if (paced) {
                val now = SystemClock.uptimeMillis()
                val delta = max(0L, now - lastTick[0])
                lastTick[0] = now
                if (!overlay.frameDrawn) return@addUpdateListener
                elapsed[0] += min(32L, delta).toFloat()
                progress = min(1f, elapsed[0] / max(1L, durationMs).toFloat())
                overlay.frameDrawn = false
                if (progress >= 1f) overlay.finishAfterDraw = Runnable { zoom.end() }
            }
            overlay.scale = from * (to / from).toDouble().pow(progress.toDouble()).toFloat()
            overlay.opacity = fadeAlpha(progress)
            trace?.update(progress)
            overlay.invalidate()
        }
        zoom.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) = finish.run()
            override fun onAnimationCancel(animation: Animator) = finish.run()
        })
        zoom.start()
        return zoom
    }

    internal class LogoView(context: Context, drawable: Drawable?, size: Int) : View(context) {
        @JvmField val logoSize: Int = max(1, size)
        private var logo: Bitmap?
        private val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        private val background = Paint()
        private val bounds = RectF()
        private val warmSrc = Rect(0, 0, 2, 2)
        private val warmDst = RectF(0f, 0f, 1f, 1f)
        private var startedAt: Long = 0L
        @JvmField var loading: Boolean = false
        @JvmField var drawnScale: Float = Float.NaN
        @JvmField var frameDrawn: Boolean = false
        @JvmField var finishAfterDraw: Runnable? = null
        @JvmField var trace: Trace? = null
        @JvmField var scale: Float = INITIAL_SCALE
        @JvmField var opacity: Float = 1f
        private var centerX: Float = Float.NaN
        private var centerY: Float = Float.NaN

        init {
            val pixels = min(2048, logoSize * 2)
            val bmp = Bitmap.createBitmap(pixels, pixels, Bitmap.Config.ARGB_8888)
            val texture = Canvas(bmp)
            texture.drawColor(0xff0e1015.toInt())
            if (drawable != null) {
                drawable.setBounds(0, 0, pixels, pixels)
                drawable.draw(texture)
            }
            logo = bmp
            background.color = 0xff0e1015.toInt()
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
            isClickable = true
        }

        fun beginLoading(x: Float, y: Float) {
            centerX = x
            centerY = y
            startedAt = SystemClock.uptimeMillis()
            loading = true
            invalidate()
        }

        fun currentScale(): Float {
            if (loading) scale = loadingScale(SystemClock.uptimeMillis() - startedAt)
            return scale
        }

        fun stop() {
            loading = false
            finishAfterDraw = null
            logo = null
        }

        override fun hasOverlappingRendering(): Boolean = false

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val t = trace
            val drawStart = if (t != null) System.nanoTime() else 0L
            drawnScale = currentScale()
            val half = logoSize * drawnScale / 2f
            val x = if (centerX.isNaN()) width / 2f else centerX
            val y = if (centerY.isNaN()) height / 2f else centerY
            bounds.set(x - half, y - half, x + half, y + half)
            val alpha = (255f * opacity).roundToInt()
            paint.alpha = alpha
            background.alpha = alpha
            val w = width.toFloat()
            val h = height.toFloat()
            val left = max(0f, min(w, bounds.left))
            val right = max(0f, min(w, bounds.right))
            val top = max(0f, min(h, bounds.top))
            val bottom = max(0f, min(h, bounds.bottom))
            canvas.drawRect(0f, 0f, w, top, background)
            canvas.drawRect(0f, bottom, w, h, background)
            canvas.drawRect(0f, top, left, bottom, background)
            canvas.drawRect(right, top, w, bottom, background)
            val bmp = logo
            if (bmp != null) {
                canvas.drawBitmap(bmp, null, bounds, paint)
                if (opacity >= 1f) {
                    paint.alpha = 254
                    canvas.drawBitmap(bmp, warmSrc, warmDst, paint)
                }
            }
            frameDrawn = true
            val finish = finishAfterDraw
            if (finish != null) {
                finishAfterDraw = null
                postOnAnimation(finish)
            }
            t?.draw(System.nanoTime() - drawStart)
            if (loading) postInvalidateOnAnimation()
        }
    }

    internal class Trace private constructor(private val handler: Handler) {
        private val startMs = SystemClock.uptimeMillis()
        private var frames: FrameTrace? = null
        private var lastUpdateMs = -1L
        private var lastDrawMs = -1L
        private var maxUpdateGap = 0L
        private var maxDrawGap = 0L
        private var maxDrawNs = 0L
        private var updates = 0
        private var draws = 0
        private var fadeLogged = false
        private var ended = false

        companion object {
            fun create(context: Context, from: Float, to: Float, overlay: LogoView): Trace? {
                return try {
                    val trace = Trace(Handler(Looper.getMainLooper()))
                    if (Build.VERSION.SDK_INT >= 24) trace.frames = FrameTrace.attach(context)
                    log("exit start from=$from to=$to view=${overlay.width}x${overlay.height} logoSize=${overlay.logoSize}")
                    trace
                } catch (_: RuntimeException) {
                    null
                }
            }

            fun log(message: String) {
                try {
                    Log.d(TAG, message)
                } catch (_: RuntimeException) {
                }
            }
        }

        fun update(progress: Float) {
            try {
                val now = SystemClock.uptimeMillis()
                updates++
                if (lastUpdateMs >= 0L) {
                    val gap = now - lastUpdateMs
                    maxUpdateGap = max(maxUpdateGap, gap)
                }
                lastUpdateMs = now
                if (!fadeLogged && progress >= 0.55f) {
                    fadeLogged = true
                }
            } catch (_: RuntimeException) {
            }
        }

        fun draw(nanos: Long) {
            try {
                val now = SystemClock.uptimeMillis()
                draws++
                maxDrawNs = max(maxDrawNs, nanos)
                if (lastDrawMs >= 0L) {
                    val gap = now - lastDrawMs
                    maxDrawGap = max(maxDrawGap, gap)
                }
                lastDrawMs = now
            } catch (_: RuntimeException) {
            }
        }

        fun end() {
            try {
                if (ended) return
                ended = true
                val f = frames
                if (f != null) handler.postDelayed({ f.detach() }, 400L)
            } catch (_: RuntimeException) {
            }
        }
    }

    @TargetApi(24)
    internal class FrameTrace private constructor(
        private val window: Window,
        private val worker: HandlerThread
    ) : Window.OnFrameMetricsAvailableListener {
        private var detached = false
        private var count = 0

        companion object {
            fun attach(context: Context): FrameTrace? {
                val window = findWindow(context) ?: return null
                val worker = HandlerThread("StartupFadeMetrics").apply { start() }
                val trace = FrameTrace(window, worker)
                return try {
                    window.addOnFrameMetricsAvailableListener(trace, Handler(worker.looper))
                    trace
                } catch (_: RuntimeException) {
                    worker.quitSafely()
                    null
                }
            }

            private fun findWindow(context: Context?): Window? {
                var c = context
                for (i in 0 until 8) {
                    if (c == null) break
                    if (c is Activity) return c.window
                    if (c !is ContextWrapper) return null
                    c = c.baseContext
                }
                return null
            }
        }

        fun detach() {
            Handler(worker.looper).post {
                if (detached) return@post
                detached = true
                try {
                    window.removeOnFrameMetricsAvailableListener(this)
                } catch (_: RuntimeException) {
                } finally {
                    worker.quitSafely()
                }
            }
        }

        override fun onFrameMetricsAvailable(w: Window?, fm: FrameMetrics?, droppedFrames: Int) {
            if (!detached) count++
        }
    }
}

/** Release the system splash at the first native frame, then zoom out smoothly. */
internal class StartupScreen private constructor(
    private val activity: Activity,
    splash: SplashScreen
) {
    private val handler = Handler(Looper.getMainLooper())
    private var attached = false
    private var started = false
    private var ready = false
    private var failed = false
    private var removed = false
    private var overlay: StartupFade.LogoView? = null
    private var exitAnimation: ValueAnimator? = null
    private var paint: StartupPaint? = null
    private val completions = ArrayList<Runnable>()
    private val fallback = Runnable { ready(true) }

    init {
        splash.setKeepOnScreenCondition { !attached && !failed && !removed && !activity.isFinishing }
        splash.setOnExitAnimationListener { provider ->
            val ov = overlay
            if (removed || failed || ov == null || !animationsEnabled()) {
                provider.remove()
                removeOverlay()
                return@setOnExitAnimationListener
            }
            val origin = loadingOrigin(ov, provider.iconView)
            ov.beginLoading(origin[0], origin[1])
            started = true
            provider.remove()
            if (ready) finishZoom()
        }
        handler.postDelayed(fallback, 8000L)
    }

    companion object {
        @JvmStatic
        fun install(activity: Activity): StartupScreen {
            return StartupScreen(activity, SplashScreen.installSplashScreen(activity))
        }

        @JvmStatic
        fun loadingOrigin(overlay: View, icon: View?): FloatArray {
            if (icon == null || icon.width <= 0 || icon.height <= 0) {
                return floatArrayOf(Float.NaN, Float.NaN)
            }
            val origin = IntArray(2)
            val location = IntArray(2)
            overlay.getLocationOnScreen(origin)
            icon.getLocationOnScreen(location)
            return floatArrayOf(
                location[0] - origin[0] + icon.width / 2f,
                location[1] - origin[1] + icon.height / 2f
            )
        }
    }

    fun attach() {
        if (attached || removed) return
        val decor = activity.window.decorView as ViewGroup
        val size = (288f * activity.resources.displayMetrics.density).roundToInt()
        val ov = StartupFade.LogoView(
            activity,
            activity.getDrawable(R.drawable.trainpilot_startup_logo),
            size
        )
        overlay = ov
        decor.addView(
            ov,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        attached = true
        decor.invalidate()
    }

    fun ready(webView: WebView?, startupFailed: Boolean, complete: Runnable) {
        if (removed) {
            complete.run()
            return
        }
        completions.add(complete)
        if (startupFailed || webView == null || !animationsEnabled()) {
            ready(startupFailed)
        } else if (!ready && paint == null) {
            paint = StartupPaint.await(webView) { ready(false) }
        }
    }

    /** Native Compose entry point: triggers the exit zoom animation as soon as the first frame is ready. */
    fun markNativeReady() {
        ready(null, false, Runnable {})
    }

    private fun ready(startupFailed: Boolean) {
        if (ready || removed) return
        failed = startupFailed
        ready = true
        handler.removeCallbacks(fallback)
        if (failed || !animationsEnabled()) {
            removeOverlay()
        } else if (started) {
            finishZoom()
        }
        activity.window.decorView.invalidate()
    }

    private fun finishZoom() {
        val ov = overlay
        if (removed || exitAnimation != null || ov == null) return
        exitAnimation = StartupFade.start(ov, { removeOverlay() }, StartupFade.EXIT_MS)
    }

    private fun removeOverlay() {
        if (removed) return
        removed = true
        handler.removeCallbacks(fallback)
        paint?.cancel()
        val ov = overlay
        if (ov != null) {
            ov.stop()
            val parent = ov.parent as? ViewGroup
            parent?.removeView(ov)
        }
        val finished = ArrayList(completions)
        completions.clear()
        for (complete in finished) complete.run()
    }

    fun destroy() {
        ready = true
        handler.removeCallbacks(fallback)
        exitAnimation?.cancel()
        removeOverlay()
    }

    private fun animationsEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ValueAnimator.areAnimatorsEnabled()
        } else {
            Settings.Global.getFloat(
                activity.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) > 0f
        }
    }
}
