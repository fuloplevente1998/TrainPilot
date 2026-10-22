package __PACKAGE_NAME__

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/**
 * Központi haptikus jelzésvezérlő a TrainPilot alkalmazáshoz.
 * Kezeli a #227 sorozat rögzítési rezgést, az Undo jelzést, valamint az időzítő visszaszámlálásait.
 */
object AppHapticsManager {
    private const val TAG = "AppHapticsManager"

    private fun getVibrator(context: Context): Vibrator? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (e: Exception) {
            Log.w(TAG, "Nem sikerült lekérni a vibrátort: ${e.message}")
            null
        }
    }

    /**
     * #227 Követelmény: 40 ms diszkrét haptikus jelzés szett kipipálásakor.
     * Csak akkor rezeg, ha a felhasználói beállításokban (hapticSetRecorded227) engedélyezve van.
     */
    fun setRecorded(context: Context, enabled: Boolean) {
        if (!enabled) return
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(40L, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(40L)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Hiba a #227 haptikus jelzés küldésekor: ${e.message}")
        }
    }

    /**
     * Általános gombnyomás és műveleti visszajelzés (pl. #188 Undo Set).
     */
    fun signal(context: Context) {
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50L, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(50L)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Hiba a haptikus jelzésnél: ${e.message}")
        }
    }

    /**
     * Időzítő visszaszámlálás: diszkrét impulzus az utolsó 3 másodpercben.
     */
    fun timerCountdown(context: Context) {
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(30L, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(30L)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Hiba a visszaszámlálási rezgésnél: ${e.message}")
        }
    }

    /**
     * Időzítő lejárta: határozott kettős rezgésmintázat.
     */
    fun timerFinish(context: Context) {
        val vibrator = getVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return

        try {
            val timings = longArrayOf(0, 120, 80, 180)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(timings, -1)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Hiba a befejezési rezgésnél: ${e.message}")
        }
    }
}
