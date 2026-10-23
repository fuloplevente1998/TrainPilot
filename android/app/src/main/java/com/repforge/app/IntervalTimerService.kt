package com.repforge.app
import com.repforge.shared.IntervalTimeline193

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.repforge.shared.WearDataLayerPaths
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max

/** Explicitly started, finite fitness timer with an ongoing Stop notification. */
class IntervalTimerService : Service() {
    companion object {
        const val ARM: String = "interval.ARM"
        const val START: String = "interval.START"
        const val STOP: String = "interval.STOP"
        const val PAUSE: String = "interval.PAUSE"
        const val RESUME: String = "interval.RESUME"
        const val PREFS: String = "trainpilot-interval"
        const val CHANNEL: String = "trainpilot_interval"
        private const val NOTIFICATION: Int = 162
        private val KEY_REGEX = Regex("[A-Za-z0-9:._|+\\-]{1,200}")

        @JvmStatic
        private var instance: IntervalTimerService? = null

        private val _timerStateFlow = MutableStateFlow(JSONObject())
        val timerStateFlow: StateFlow<JSONObject> = _timerStateFlow.asStateFlow()

        @JvmStatic
        @Throws(Exception::class)
        fun validate(plan: JSONObject): IntervalTimeline {
            val key = plan.optString("key")
            if (!KEY_REGEX.matches(key)) throw IllegalArgumentException("Invalid interval key")
            if (plan.has("initialRest") && plan.getDouble("initialRest") != plan.getInt("initialRest").toDouble()) {
                throw IllegalArgumentException("Invalid initial rest")
            }
            val rows = plan.getJSONArray("steps")
            val parsed = ArrayList<IntervalTimeline193.Step>()
            val owners = HashSet<String>()
            for (i in 0 until rows.length()) {
                val row = rows.getJSONObject(i)
                val set = row.getString("setId")
                if (!owners.add(set)) throw IllegalArgumentException("Duplicate interval owner")
                val gps = row.optString("gpsKey", "")
                if (gps.isNotEmpty() && !KEY_REGEX.matches(gps)) {
                    throw IllegalArgumentException("Invalid GPS key")
                }
                parsed.add(
                    IntervalTimeline193.Step(
                        row.getString("exerciseId"),
                        set,
                        row.optString("name", "TrainPilot"),
                        row.getInt("work"),
                        row.getInt("rest"),
                        row.getInt("rounds"),
                        gps
                    )
                )
            }
            if (!plan.optBoolean("autoAdvance") && parsed.size != 1) {
                throw IllegalArgumentException("Manual interval has one owner")
            }
            return IntervalTimeline(
                parsed,
                plan.optBoolean("autoAdvance"),
                plan.optInt("initialRest", 0),
                plan.optBoolean("restOnly")
            )
        }

        @JvmStatic
        fun armed(workoutId: String?, generation: Long): Boolean {
            val s = instance ?: return false
            return !workoutId.isNullOrEmpty() && workoutId == s.armedWorkoutId && generation == s.armedGeneration
        }

        /** Runs only on the main looper, in a service already started by a visible phone workout. */
        @JvmStatic
        @Throws(Exception::class)
        fun startPrepared(plan: JSONObject): Boolean {
            val s = instance ?: return false
            if (!armed(plan.getString("workoutId"), plan.getLong("generation"))) return false
            if (!PhoneOwnership193.phoneAllowed(s, plan.getString("workoutId"))) return false
            if (plan.optString("key") == s.key) return true
            if (s.timeline != null && !s.stopped && "complete" != s.timeline!!.at(s.now()).phase) return false
            s.handler.removeCallbacks(s.tick)
            s.parse(plan)
            s.stopped = false
            s.elapsed = 0L
            s.began = SystemClock.elapsedRealtime()
            s.running = true
            s.save()
            s.acquire()
            s.handler.post(s.tick)
            s.notifyStatus()
            s.publishWatchClock()
            return true
        }

        @JvmStatic
        fun trackSetStopwatch(workoutId: String, generation: Long) {
            val s = instance ?: return
            if (!armed(workoutId, generation)) return
            s.handler.removeCallbacks(s.stopwatchTick)
            s.handler.postDelayed(s.stopwatchTick, 1000L)
        }

        @JvmStatic
        fun closeWorkout(workoutId: String, generation: Long) {
            val s = instance ?: return
            if (!armed(workoutId, generation)) return
            s.armedWorkoutId = ""
            if (s.timeline == null) {
                s.stopForeground(STOP_FOREGROUND_REMOVE)
                s.stopSelf()
            } else if (workoutId == s.workoutId) {
                s.stopRun()
            }
        }

        @JvmStatic
        fun stopForOwnership(context: Context, workoutId: String) {
            val s = instance
            if (s != null && (workoutId == s.workoutId || workoutId == s.armedWorkoutId)) {
                PhoneWorkoutExecution.store(context).checkpointStopwatch(s.stopwatchOwnerId, s.stopwatchOwnerGeneration,
                    SystemClock.elapsedRealtime(), pause = true)
                s.armedWorkoutId = ""
                if (workoutId == s.workoutId) s.stopRun()
                else {
                    // An armed lower stopwatch need not have an interval timeline.
                    s.stopForeground(STOP_FOREGROUND_REMOVE)
                    s.stopSelf()
                }
            }
        }

        @JvmStatic
        @Throws(Exception::class)
        fun installOwnership193(plan: JSONObject): JSONObject {
            val s = instance
            if (s == null || !armed(plan.getString("workoutId"), plan.getLong("generation"))) {
                throw IllegalStateException("Arm the returned workout before continuing")
            }
            if (plan.optString("key") == s.key && s.executionGeneration == plan.getLong("generation")) {
                return s.current()
            }
            val frame = PhoneWorkoutExecution.store(s).snapshot()
            val t = frame?.optJSONObject("transition")
            if (frame == null ||
                !frame.optBoolean("workoutPaused") ||
                !PhoneOwnership193.phoneAllowed(s, plan.getString("workoutId")) ||
                t == null ||
                t.optJSONObject("plan") == null ||
                t.getJSONObject("plan").getString("key") != plan.getString("key")
            ) {
                throw IllegalStateException("The returned workout changed")
            }
            s.handler.removeCallbacks(s.tick)
            s.release()
            s.cancelVibration()
            s.parse(plan)
            s.elapsed = plan.getLong("elapsedMs")
            s.began = 0L
            s.running = false
            s.stopped = false
            s.timeLedger = IntervalTimeLedger.restore(
                plan.getJSONArray("steps"),
                plan.getJSONObject("ledger"),
                SystemClock.elapsedRealtime()
            )
            s.save()
            s.notifyStatus()
            s.publishWatchClock()
            return s.current()
        }

        @JvmStatic
        fun disarm() {
            val s = instance ?: return
            s.armedWorkoutId = ""
            if (s.timeline == null) {
                s.stopForeground(STOP_FOREGROUND_REMOVE)
                s.stopSelf()
            } else if (!s.running && "complete" == s.timeline!!.at(s.now()).phase) {
                s.stopForeground(STOP_FOREGROUND_REMOVE)
                s.stopSelf()
            }
        }

        /** Retire the old clock after accounting real elapsed time, while retaining the foreground controller. */
        @JvmStatic
        @Throws(Exception::class)
        fun pauseForUndo(context: Context, key: String) {
            val s = instance ?: return
            if (key != s.key) return
            s.pauseRun()
            s.stopped = true
            s.handler.removeCallbacks(s.tick)
            s.release()
            s.cancelVibration()
            s.save()
            s.notifyStatus()
        }

        @JvmStatic
        fun snapshot(context: Context): JSONObject {
            val service = instance
            if (service != null && service.timeline != null) {
                service.update()
                return service.current()
            }
            return try {
                val out = JSONObject(context.getSharedPreferences(PREFS, MODE_PRIVATE).getString("state", "{}") ?: "{}")
                if (out.optBoolean("running")) {
                    out.put("running", false).put("interrupted", true)
                    context.getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString("state", out.toString()).apply()
                }
                _timerStateFlow.value = out
                out
            } catch (_: Exception) {
                JSONObject()
            }
        }

        @JvmStatic
        @Throws(Exception::class)
        fun command(context: Context, key: String?, action: String): JSONObject {
            val s = instance
            if (s == null || key != s.key || s.timeline == null) {
                throw IllegalStateException("Interval owner is no longer active")
            }
            if (("resume" == action || "skipRest" == action) && !PhoneOwnership193.phoneAllowed(context, s.workoutId)) {
                throw IllegalStateException("Workout ownership changed")
            }
            if (("resume" == action || "skipRest" == action) && s.executionGeneration > 0L) {
                val frame = PhoneWorkoutExecution.store(context).snapshot()
                if (frame != null && s.workoutId == frame.optString("workoutId") && frame.optBoolean("workoutPaused")) {
                    throw IllegalStateException("Workout is suspended")
                }
            }
            val tl = s.timeline!!
            when {
                "pause" == action -> s.pauseRun()
                "stop" == action -> s.stopRun()
                "resume" == action && !s.running && !s.stopped && "complete" != tl.at(s.elapsed).phase -> {
                    s.timeLedger!!.sample(tl, s.elapsed, SystemClock.elapsedRealtime(), false, false, false)
                    s.began = SystemClock.elapsedRealtime()
                    s.running = true
                    s.acquire()
                    DistanceTrackingService.pauseOwned(
                        context,
                        s.steps[s.gpsStep].gpsKey ?: "",
                        "work" != tl.at(s.elapsed).phase
                    )
                    s.handler.post(s.tick)
                    s.save()
                }
                "skipRest" == action && !s.stopped -> {
                    val p = tl.at(s.now())
                    if ("rest" != p.phase) throw IllegalStateException("Not resting")
                    s.handler.removeCallbacks(s.tick)
                    s.timeLedger!!.sample(tl, p.elapsedMs, SystemClock.elapsedRealtime(), s.running, s.stopped, s.interrupted)
                    s.elapsed = p.elapsedMs + p.remainingMs
                    s.timeLedger!!.skipTo(s.elapsed)
                    s.began = SystemClock.elapsedRealtime()
                    s.running = true
                    s.acquire()
                    s.update()
                    if (s.running) s.handler.post(s.tick)
                }
            }
            s.publishWatchClock()
            return s.current()
        }

        /** Change this rest's duration without crediting skipped or added virtual seconds as actual rest. */
        @JvmStatic
        fun adjustRest(context: Context, target: NativeTimerTarget, deltaSeconds: Int): JSONObject {
            require(deltaSeconds in setOf(-15, 15)) { "Invalid rest adjustment" }
            val s = instance ?: error("A pihenő időzítője már nem aktív.")
            val frame = PhoneWorkoutExecution.store(context).snapshot() ?: error("Nincs aktív natív edzés.")
            check(target.key.isNotEmpty() && target.key == s.key && target.workoutId == s.workoutId &&
                target.generation == s.executionGeneration && target.workoutId == frame.optString("workoutId") &&
                target.generation == frame.optLong("generation") && !frame.optBoolean("workoutPaused") &&
                frame.optBoolean("active") && frame.optString("timerOwner") == "phone" &&
                !PhoneWorkoutExecution.store(context).restoreBlocked() && !PhoneWorkoutExecution.store(context).commandsSuspended() &&
                PhoneOwnership193.phoneAllowed(context, target.workoutId) && s.restOnly && s.steps.size == 1 &&
                s.running && !s.stopped && !s.interrupted) { "A pihenő tulajdonosa vagy állapota megváltozott." }
            s.update() // Account elapsed real time on the old timeline first.
            val phase = s.timeline!!.at(s.now())
            check(phase.phase == "rest" && s.running) { "A pihenő már véget ért." }
            if (phase.remainingMs + deltaSeconds * 1000L <= 0L) return command(context, target.key, "skipRest")
            val old = s.steps.single()
            val rest = (old.rest + deltaSeconds).coerceIn(0, 3600)
            val step = IntervalTimeline193.Step(old.exerciseId, old.setId, old.name, old.work, rest, old.rounds, old.gpsKey)
            val priorTimeline = s.timeline
            val priorRow = JSONObject(s.planSteps.getJSONObject(0).toString())
            s.steps[0] = step
            s.planSteps.getJSONObject(0).put("rest", rest)
            s.timeline = IntervalTimeline(s.steps, false, 0, true)
            s.elapsed = phase.elapsedMs
            s.began = SystemClock.elapsedRealtime()
            try { s.save() } catch (failure: Exception) {
                s.steps[0] = old
                s.planSteps.put(0, priorRow)
                s.timeline = priorTimeline
                s.elapsed = phase.elapsedMs
                s.began = SystemClock.elapsedRealtime()
                _timerStateFlow.value = s.current()
                throw failure
            }
            s.publishWatchClock()
            s.notifyStatus()
            return s.current()
        }

        @JvmStatic
        @Throws(Exception::class)
        fun attachGPS(context: Context, key: String, index: Int, tracking: String): JSONObject {
            val s = instance
            val tl = s?.timeline
            if (s == null || tl == null || key != s.key || tl.at(s.now()).step != index || s.stopped || "complete" == tl.at(s.now()).phase) {
                throw IllegalStateException("Interval owner changed")
            }
            val gps = DistanceTrackingService.snapshot(context)
            if (tracking != gps.optString("key") || !gps.optBoolean("active")) {
                throw IllegalStateException("GPS owner changed")
            }
            s.steps[index].gpsKey = tracking
            DistanceTrackingService.pauseOwned(context, tracking, !s.running || "work" != tl.at(s.now()).phase)
            s.save()
            return s.current()
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private val steps = ArrayList<IntervalTimeline193.Step>()
    private val segments = JSONArray()
    private var timeline: IntervalTimeline? = null
    private var timeLedger: IntervalTimeLedger? = null
    private var ownershipBaseLedger193: JSONObject? = null
    private var executionGeneration: Long = 0L
    private var key: String = ""
    private var language: String = "en"
    private var lastToken: String = ""
    private var workoutId: String = ""
    private var armedWorkoutId: String = ""
    private var armedGeneration: Long = 0L
    private var stopwatchOwnerId: String = ""
    private var stopwatchOwnerGeneration: Long = 0L
    private var planSteps = JSONArray()
    private var phaseNumber: Long = 0L
    private var running: Boolean = false
    private var advance: Boolean = false
    private var sound: Boolean = false
    private var vibration: Boolean = false
    private var stopped: Boolean = false
    private var restOnly: Boolean = false
    private var interrupted: Boolean = false
    private var initialRest: Int = 0
    private var elapsed: Long = 0L
    private var began: Long = 0L
    private var lastSavedSecond: Long = -1L
    private var gpsStep: Int = 0
    private var wake: PowerManager.WakeLock? = null
    private var tones: ToneGenerator? = null
    private var haptics = IntervalHapticCues()

    private val expire = Runnable {
        armedWorkoutId = ""
        if (timeline != null) {
            stopRun()
        } else {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    // The existing foreground controller owns the lower stopwatch too. It only
    // checkpoints elapsed time: process death / task removal cannot mark a set done.
    private val stopwatchTick = object : Runnable {
        override fun run() {
            if (armedWorkoutId.isEmpty()) return
            val stillRunning = runCatching {
                val store = PhoneWorkoutExecution.store(this@IntervalTimerService)
                store.checkpointStopwatch(stopwatchOwnerId, stopwatchOwnerGeneration, SystemClock.elapsedRealtime(),
                    pause = store.commandsSuspended() || !PhoneOwnership193.phoneAllowed(this@IntervalTimerService, armedWorkoutId))
            }.getOrDefault(true)
            // No perpetual SQLite writes for an idle/completed stopwatch.
            if (stillRunning) handler.postDelayed(this, 1000L)
        }
    }

    private val tick = object : Runnable {
        override fun run() {
            if (!running) return
            update()
            if (running) handler.postDelayed(this, 200L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, id: Int): Int {
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (STOP == intent.action) {
            if (key == intent.getStringExtra("key")) {
                armedWorkoutId = ""
                stopRun()
            }
            return START_NOT_STICKY
        }
        if (ARM == intent.action) {
            val requested = intent.getStringExtra("workoutId")
            if (!PhoneOwnership193.phoneAllowed(this, requested.orEmpty())) return START_NOT_STICKY
            if (requested.isNullOrEmpty() || intent.getLongExtra("generation", 0L) <= 0L) return START_NOT_STICKY
            if (timeline != null && !stopped && "complete" != timeline!!.at(now()).phase && requested != workoutId) {
                return START_NOT_STICKY
            }
            if (requested != armedWorkoutId) {
                handler.removeCallbacks(expire)
                handler.postDelayed(expire, IntervalTimeline.MAX_MS)
            }
            armedWorkoutId = requested
            armedGeneration = intent.getLongExtra("generation", 0L)
            stopwatchOwnerId = requested
            stopwatchOwnerGeneration = armedGeneration
            handler.removeCallbacks(stopwatchTick)
            handler.postDelayed(stopwatchTick, 1000L)
            language = intent.getStringExtra("language") ?: "en"
            channel()
            startForeground(NOTIFICATION, notification())
            try {
                PhoneWorkoutExecution.store(this).beginManualWork(requested, armedGeneration, SystemClock.elapsedRealtime())
            } catch (_: Exception) {
            }
            return START_NOT_STICKY
        }
        if (PAUSE == intent.action || RESUME == intent.action) {
            try {
                command(this, intent.getStringExtra("key"), if (PAUSE == intent.action) "pause" else "resume")
            } catch (_: Exception) {
            }
            return START_NOT_STICKY
        }
        if (START != intent.action || running) return START_NOT_STICKY
        try {
            val plan = JSONObject(intent.getStringExtra("plan") ?: "{}")
            parse(plan)
            stopped = false
            elapsed = plan.optLong("elapsedMs", 0L)
            began = SystemClock.elapsedRealtime()
            running = true
            channel()
            startForeground(NOTIFICATION, notification())
            acquire()
            handler.post(tick)
            save()
        } catch (_: Exception) {
            running = false
            release()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun channel() {
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL, "TrainPilot intervals", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    @Throws(Exception::class)
    private fun parse(plan: JSONObject) {
        val current = ActiveWorkoutStore.current(this)
        if (current != null && !PhoneOwnership193.phoneAllowed(this, current.optString("workoutId"))) {
            throw IllegalStateException("Workout ownership changed")
        }
        workoutId = plan.optString("workoutId", current?.optString("workoutId").orEmpty())
        val execution = PhoneWorkoutExecution.store(this).snapshot()
        executionGeneration = plan.optLong(
            "generation",
            if (execution != null && execution.optString("workoutId") == workoutId) execution.optLong("generation") else 0L
        )
        planSteps = JSONArray(plan.getJSONArray("steps").toString())
        phaseNumber = 0L
        ownershipBaseLedger193 = plan.optJSONObject("ownershipBaseLedger193")
        timeline = validate(plan)
        timeLedger = IntervalTimeLedger(plan.getJSONArray("steps").length(), SystemClock.elapsedRealtime())
        interrupted = false
        lastSavedSecond = -1L
        steps.clear()
        val rows = plan.getJSONArray("steps")
        for (i in 0 until rows.length()) {
            val row = rows.getJSONObject(i)
            steps.add(
                IntervalTimeline193.Step(
                    row.getString("exerciseId"),
                    row.getString("setId"),
                    row.optString("name", "TrainPilot"),
                    row.getInt("work"),
                    row.getInt("rest"),
                    row.getInt("rounds"),
                    row.optString("gpsKey", "")
                )
            )
        }
        initialRest = plan.optInt("initialRest", 0)
        restOnly = plan.optBoolean("restOnly")
        timeline = IntervalTimeline(steps, plan.optBoolean("autoAdvance"), initialRest, restOnly)
        key = plan.getString("key")
        language = plan.optString("language", "en")
        advance = plan.optBoolean("autoAdvance")
        sound = plan.optBoolean("sound", true)
        vibration = plan.optBoolean("vibration", true)
        while (segments.length() > 0) segments.remove(0)
        gpsStep = 0
        lastToken = ""
        haptics = IntervalHapticCues()
    }

    private fun now(): Long {
        return elapsed + if (running) max(0L, SystemClock.elapsedRealtime() - began) else 0L
    }

    private fun update() {
        val tl = timeline ?: return
        val p = tl.at(now())
        timeLedger?.sample(tl, p.elapsedMs, SystemClock.elapsedRealtime(), running, stopped, interrupted)
        if (p.step != gpsStep || (running && "complete" == p.phase)) {
            handoff(p)
        }
        DistanceTrackingService.pauseOwned(
            this,
            steps[p.step].gpsKey ?: "",
            !running || "work" != p.phase
        )
        val vibrationMs = haptics.next(p, running && !stopped)
        if (vibrationMs > 0) vibrate(vibrationMs)
        if (p.token != lastToken) {
            lastToken = p.token
            phaseNumber++
            if (!stopped) cue("complete" == p.phase)
            notifyStatus()
            publishWatchClock()
        }
        if ("complete" == p.phase) {
            elapsed = p.elapsedMs
            running = false
            began = 0L
            release()
            handler.removeCallbacks(tick)
            save()
            if (armedWorkoutId.isEmpty()) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            } else {
                notifyStatus()
            }
        } else {
            val currentSecond = (if (running) p.elapsedMs else SystemClock.elapsedRealtime()) / 1000L
            if (currentSecond != lastSavedSecond) {
                lastSavedSecond = currentSecond
                save()
                notifyStatus()
            } else {
                _timerStateFlow.value = current()
            }
        }
    }

    private fun handoff(p: IntervalTimeline193.Phase) {
        val old = steps[gpsStep]
        val tracking = old.gpsKey ?: ""
        if (tracking.isNotEmpty()) {
            val current = DistanceTrackingService.snapshot(this)
            if (tracking == current.optString("key")) {
                val nextGpsKey = steps[p.step].gpsKey ?: ""
                val capture = if ("complete" != p.phase && p.step != gpsStep && nextGpsKey.isNotEmpty()) {
                    DistanceTrackingService.rollover(this, tracking, nextGpsKey, steps[p.step].name ?: "TrainPilot")
                } else {
                    DistanceTrackingService.stopOwned(this, tracking)
                }
                if (capture != null) {
                    try {
                        capture.put("exerciseId", old.exerciseId)
                        capture.put("setId", old.setId)
                        segments.put(capture)
                    } catch (_: Exception) {
                    }
                }
            }
        }
        gpsStep = p.step
    }

    private fun acquire() {
        if (wake == null) {
            wake = getSystemService(PowerManager::class.java)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TrainPilot:interval")
        }
        if (wake?.isHeld != true) {
            wake?.acquire(IntervalTimeline.MAX_MS + 60_000L)
        }
    }

    private fun release() {
        if (wake?.isHeld == true) {
            wake?.release()
        }
    }

    private fun cue(complete: Boolean) {
        if (sound) {
            try {
                if (tones == null) tones = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
                tones?.startTone(
                    if (complete) ToneGenerator.TONE_PROP_ACK else ToneGenerator.TONE_PROP_BEEP,
                    if (complete) 450 else 180
                )
            } catch (_: Exception) {
            }
        }
    }

    private fun vibrator(): Vibrator? {
        return if (Build.VERSION.SDK_INT >= 31) {
            getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private fun vibrate(milliseconds: Int) {
        if (vibration) {
            try {
                val v = vibrator()
                if (v != null && v.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= 26) {
                        v.vibrate(VibrationEffect.createOneShot(milliseconds.toLong(), VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        v.vibrate(milliseconds.toLong())
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun cancelVibration() {
        try {
            vibrator()?.cancel()
        } catch (_: Exception) {
        }
    }

    private fun publishWatchClock() {
        val tl = timeline ?: return
        if (workoutId.isEmpty() || (!advance && !restOnly)) return
        try {
            val p = tl.at(now())
            val value = JSONObject()
                .put("workoutId", workoutId)
                .put("key", key)
                .put("owner", "phone")
                .put("enabled", !stopped)
                .put("phase", if (stopped) "ready" else p.phase)
                .put("paused", !running && "complete" != p.phase)
                .put("remainingMs", p.remainingMs)
                .put("deadlineAt", if (running) System.currentTimeMillis() + p.remainingMs else 0L)
                .put("workSeconds", steps[p.step].work)
                .put("restSeconds", if ("rest" == p.phase) tl.phaseDurationMs(p) / 1000L else steps[p.step].rest.toLong())
                .put("rounds", steps[p.step].rounds)
                .put("round", p.round)
                .put("vibration", false)
                .put("autoAdvance", advance)
                .put("restOnly", restOnly)
                .put("stopped", stopped)
                .put("completed", p.completed)
                .put("ledger", timeLedger!!.json(planSteps))
                .put("stepIndex", p.step)
                .put("stepElapsedMs", p.stepElapsedMs)
                .put("steps", planSteps)
                .put("phaseNumber", phaseNumber)
            val map = PutDataMapRequest.create(WearDataLayerPaths.PHONE_AUTOMATIC_PATH)
            map.dataMap.putString("automatic", value.toString())
            map.dataMap.putLong("publishedAt", System.currentTimeMillis())
            Wearable.getDataClient(this).putDataItem(map.asPutDataRequest().setUrgent())
        } catch (_: Exception) {
        }
    }

    private fun current(): JSONObject {
        val out = JSONObject()
        val tl = timeline ?: return out
        val p = tl.at(now())
        try {
            out.put("key", key)
                .put("workoutId", workoutId)
                .put("generation", executionGeneration)
                .put("ledger", timeLedger!!.json(planSteps))
                .put("interrupted", interrupted)
                .put("phaseNumber", phaseNumber)
                .put("running", running)
                .put("stopped", stopped)
                .put("restOnly", restOnly)
                .put("initialRest", initialRest)
                .put("autoAdvance", advance)
                .put("sound", sound)
                .put("vibration", vibration)
                .put("language", language)
                .put("elapsedMs", p.elapsedMs)
                .put("stepElapsedMs", p.stepElapsedMs)
                .put("stepIndex", p.step)
                .put("completed", p.completed)
                .put("phase", p.phase)
                .put("round", p.round)
                .put("remainingMs", p.remainingMs)
                .put("totalMs", p.totalMs)
            if (ownershipBaseLedger193 != null) {
                out.put("ownershipBaseLedger193", JSONObject(ownershipBaseLedger193.toString()))
            }
            val rows = JSONArray()
            for (i in steps.indices) {
                val s = steps[i]
                rows.put(
                    JSONObject(planSteps.getJSONObject(i).toString())
                        .put("exerciseId", s.exerciseId)
                        .put("setId", s.setId)
                        .put("name", s.name)
                        .put("work", s.work)
                        .put("rest", s.rest)
                        .put("rounds", s.rounds)
                        .put("gpsKey", s.gpsKey)
                )
            }
            out.put("steps", rows).put("gpsSegments", segments)
            val gps = DistanceTrackingService.snapshot(this)
            val stepGpsKey = steps[p.step].gpsKey ?: ""
            if (stepGpsKey.isNotEmpty() && stepGpsKey == gps.optString("key")) {
                out.put("gps", gps)
            }
        } catch (_: Exception) {
        }
        return out
    }

    private fun save() {
        val tl = timeline ?: return
        if (key.isEmpty()) return
        timeLedger?.sample(tl, tl.at(now()).elapsedMs, SystemClock.elapsedRealtime(), running, stopped, interrupted)
        val checkpoint = current()
        _timerStateFlow.value = checkpoint
        try {
            if (executionGeneration > 0L) {
                if (PhoneWorkoutExecution.store(this).checkpointTimer(checkpoint, SystemClock.elapsedRealtime())) {
                    PhoneWorkoutExecution.publish(this)
                }
            }
        } catch (error: Exception) {
            throw IllegalStateException("Unable to checkpoint workout", error)
        }
        if (!getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString("state", checkpoint.toString()).commit()) {
            throw IllegalStateException("Unable to checkpoint interval")
        }
    }

    private fun notification(): Notification {
        val title = DistanceTrackingService.text(
            language,
            "Intervallumidőzítő",
            "Interval timer",
            "Intervalltimer",
            "Cronometru de intervale"
        )
        val pause = DistanceTrackingService.text(language, "Szünetel", "Paused", "Pausiert", "În pauză")
        val tl = timeline
        if (tl == null || (armedWorkoutId.isNotEmpty() && !running && (stopped || "complete" == tl.at(now()).phase))) {
            val open = PendingIntent.getActivity(
                this,
                162,
                Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val stop = PendingIntent.getService(
                this,
                162,
                Intent(this, IntervalTimerService::class.java).setAction(STOP).putExtra("key", key),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            return NotificationCompat.Builder(this, CHANNEL)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(
                    DistanceTrackingService.text(
                        language,
                        "Automata edzés · Sorozat rögzítésére vár",
                        "Automatic workout · Waiting for set recording",
                        "Automatiktraining · Satz erfassen",
                        "Antrenament automat · Înregistrează seria"
                    )
                )
                .setContentIntent(open)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .addAction(0, DistanceTrackingService.text(language, "Leállítás", "Stop", "Stoppen", "Oprește"), stop)
                .build()
        }
        val p = tl.at(now())
        var label = DistanceTrackingService.text(language, "Munka", "Work", "Belastung", "Efort")
        if ("rest" == p.phase) {
            label = DistanceTrackingService.text(language, "Pihenő", "Rest", "Pause", "Odihnă")
        }
        val body = (if (running) "$label · ${p.round}/${steps[p.step].rounds} · ${(p.remainingMs + 999L) / 1000L} s" else pause) +
            " · ${steps[p.step].name}"
        val open = PendingIntent.getActivity(
            this,
            162,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stop = PendingIntent.getService(
            this,
            162,
            Intent(this, IntervalTimerService::class.java).setAction(STOP).putExtra("key", key),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val toggle = PendingIntent.getService(
            this,
            163,
            Intent(this, IntervalTimerService::class.java).setAction(if (running) PAUSE else RESUME).putExtra("key", key),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val toggleLabel = if (running) pause else DistanceTrackingService.text(language, "Folytatás", "Resume", "Fortsetzen", "Continuă")
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(0, toggleLabel, toggle)
            .addAction(0, DistanceTrackingService.text(language, "Leállítás", "Stop", "Stoppen", "Oprește"), stop)
            .build()
    }

    private fun notifyStatus() {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION, notification())
    }

    private fun pauseRun() {
        if (!running) {
            cancelVibration()
            return
        }
        update()
        cancelVibration()
        if (!running) return
        elapsed = now()
        running = false
        began = 0L
        handler.removeCallbacks(tick)
        release()
        DistanceTrackingService.pauseOwned(this, steps[gpsStep].gpsKey ?: "", true)
        save()
        notifyStatus()
        publishWatchClock()
    }

    private fun stopRun() {
        runCatching { PhoneWorkoutExecution.store(this).checkpointStopwatch(stopwatchOwnerId, stopwatchOwnerGeneration, SystemClock.elapsedRealtime(), pause = true) }
        if (timeline == null) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }
        pauseRun()
        stopped = true
        DistanceTrackingService.stopOwned(this, steps[gpsStep].gpsKey ?: "")
        save()
        publishWatchClock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onTaskRemoved(root: Intent?) {
        armedWorkoutId = ""
        if (timeline != null) {
            stopRun()
        } else {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onDestroy() {
        runCatching { PhoneWorkoutExecution.store(this).checkpointStopwatch(stopwatchOwnerId, stopwatchOwnerGeneration, SystemClock.elapsedRealtime(), pause = true) }
        handler.removeCallbacks(stopwatchTick)
        if (running) {
            cancelVibration()
            elapsed = now()
            running = false
            interrupted = true
            DistanceTrackingService.stopOwned(this, steps[gpsStep].gpsKey ?: "")
            save()
        }
        handler.removeCallbacks(tick)
        handler.removeCallbacks(expire)
        release()
        tones?.release()
        if (instance === this) instance = null
        super.onDestroy()
    }
}
