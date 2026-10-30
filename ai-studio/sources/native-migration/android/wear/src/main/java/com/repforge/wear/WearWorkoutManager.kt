package com.repforge.wear

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject

enum class WearOwnershipRole {
    WATCH_PRIMARY, // Az óra az elsődleges mérőeszköz
    MIRROR         // A telefon vezérel, az óra tükrözött kijelző
}

data class WearWorkoutState(
    val title: String = "Nincs aktív edzés",
    val stateName: String = "IDLE",
    val elapsedSeconds: Long = 0L,
    val restSecondsRemaining: Int = 0,
    val isRestActive: Boolean = false,
    val currentExerciseName: String = "Fekvenyomás",
    val currentSetIndex: Int = 1,
    val targetReps: Int = 10,
    val targetWeightKg: Double = 80.0,
    val currentBpm: Int = 0,
    val role: WearOwnershipRole = WearOwnershipRole.MIRROR
)

/**
 * A Wear OS óra központi edzésvezérlője és szenzorkezelője.
 */
class WearWorkoutManager(private val context: Context) : SensorEventListener {

    private val tag = "WearWorkoutManager"
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val heartRateSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vm.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private val messageClient = Wearable.getMessageClient(context)
    private val nodeClient = Wearable.getNodeClient(context)

    private val _state = MutableStateFlow(WearWorkoutState())
    val state: StateFlow<WearWorkoutState> = _state.asStateFlow()

    private var localTickerJob: Job? = null

    init {
        startHeartRateMonitoring()
    }

    fun updateFromPhone(
        stateName: String,
        elapsedSeconds: Long,
        restSeconds: Int,
        isRestActive: Boolean
    ) {
        if (_state.value.role == WearOwnershipRole.MIRROR) {
            val previousRest = _state.value.restSecondsRemaining
            _state.update {
                it.copy(
                    stateName = stateName,
                    elapsedSeconds = elapsedSeconds,
                    restSecondsRemaining = restSeconds,
                    isRestActive = isRestActive
                )
            }

            // Haptikus jelzés pihenőidő lejárásakor (3 mp és 0 mp)
            if (isRestActive && restSeconds in 1..3 && restSeconds != previousRest) {
                vibrateShort()
            } else if (isRestActive && restSeconds == 0 && previousRest > 0) {
                vibrateDouble()
            }
        }
    }

    fun completeSet(actualRir: Int) {
        val current = _state.value
        val payload = JSONObject().apply {
            put("command", "COMPLETE_SET")
            put("reps", current.targetReps)
            put("weight", current.targetWeightKg)
            put("rir", actualRir)
            put("timestamp", System.currentTimeMillis())
        }
        sendCommandToPhone(payload)
        vibrateShort()
    }

    fun skipRest() {
        val payload = JSONObject().apply {
            put("command", "SKIP_REST")
            put("timestamp", System.currentTimeMillis())
        }
        sendCommandToPhone(payload)
        _state.update { it.copy(isRestActive = false, restSecondsRemaining = 0) }
    }

    fun adjustRest(deltaSeconds: Int) {
        val payload = JSONObject().apply {
            put("command", "ADJUST_REST")
            put("deltaSeconds", deltaSeconds)
            put("timestamp", System.currentTimeMillis())
        }
        sendCommandToPhone(payload)
    }

    /**
     * Kétirányú Ownership Handoff: Vezérlés átadása vagy visszavétele (#193).
     */
    fun toggleOwnership() {
        val newRole = if (_state.value.role == WearOwnershipRole.MIRROR) {
            WearOwnershipRole.WATCH_PRIMARY
        } else {
            WearOwnershipRole.MIRROR
        }

        _state.update { it.copy(role = newRole) }

        val payload = JSONObject().apply {
            put("action", "HANDOFF_REQUEST")
            put("source", "WATCH")
            put("target", if (newRole == WearOwnershipRole.WATCH_PRIMARY) "WATCH" else "PHONE")
            put("timestamp", System.currentTimeMillis())
        }

        scope.launch(Dispatchers.IO) {
            try {
                val nodes = Tasks.await(nodeClient.connectedNodes)
                for (node in nodes) {
                    messageClient.sendMessage(node.id, "/trainpilot/workout/handoff", payload.toString().toByteArray(Charsets.UTF_8))
                }
            } catch (e: Exception) {
                Log.e(tag, "Hiba az óra oldali handoff üzenet küldésekor", e)
            }
        }

        if (newRole == WearOwnershipRole.WATCH_PRIMARY) {
            startLocalTicker()
        } else {
            localTickerJob?.cancel()
        }
    }

    private fun startLocalTicker() {
        localTickerJob?.cancel()
        localTickerJob = scope.launch {
            while (isActive && _state.value.role == WearOwnershipRole.WATCH_PRIMARY) {
                delay(1000L)
                _state.update { current ->
                    val newRest = if (current.isRestActive && current.restSecondsRemaining > 0) current.restSecondsRemaining - 1 else 0
                    val isRestStillActive = current.isRestActive && newRest > 0
                    current.copy(
                        elapsedSeconds = current.elapsedSeconds + 1L,
                        restSecondsRemaining = newRest,
                        isRestActive = isRestStillActive
                    )
                }
            }
        }
    }

    private fun sendCommandToPhone(payload: JSONObject) {
        scope.launch(Dispatchers.IO) {
            try {
                val nodes = Tasks.await(nodeClient.connectedNodes)
                val bytes = payload.toString().toByteArray(Charsets.UTF_8)
                for (node in nodes) {
                    messageClient.sendMessage(node.id, "/trainpilot/workout/command", bytes)
                }
            } catch (e: Exception) {
                Log.e(tag, "Nem sikerült elküldeni a parancsot a telefonnak", e)
            }
        }
    }

    private fun startHeartRateMonitoring() {
        heartRateSensor?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_HEART_RATE) {
            val bpm = event.values[0].toInt()
            if (bpm > 0) {
                _state.update { it.copy(currentBpm = bpm) }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun vibrateShort() {
        vibrator.vibrate(VibrationEffect.createOneShot(70L, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun vibrateDouble() {
        val pattern = longArrayOf(0, 100, 100, 150)
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
    }

    companion object {
        @Volatile
        private var instance: WearWorkoutManager? = null

        fun getInstance(context: Context): WearWorkoutManager {
            return instance ?: synchronized(this) {
                instance ?: WearWorkoutManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
