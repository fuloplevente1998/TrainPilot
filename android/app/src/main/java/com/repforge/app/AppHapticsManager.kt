package com.repforge.app

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Foreground interval cues, manual set haptics (#227), and system fontScale helper. */
object AppHapticsManager {

    @JvmStatic
    fun fontScale(context: Context): Float {
        return context.resources.configuration.fontScale
    }

    /** 90ms interval / alert cue. */
    @JvmStatic
    fun signal(context: Context) {
        vibrate(context, 90L)
    }

    /** #227: A single accepted manual set (40ms); independent of interval transition cues. */
    @JvmStatic
    fun setRecorded(context: Context, enabled: Boolean = true) {
        if (!enabled) return
        vibrate(context, 40L)
    }

    @JvmStatic
    fun vibrate(context: Context, durationMillis: Long) {
        try {
            val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        VibrationEffect.createOneShot(durationMillis, VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(durationMillis)
                }
            }
        } catch (_: SecurityException) {
        }
    }
}
