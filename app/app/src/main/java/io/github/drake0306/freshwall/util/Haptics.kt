package io.github.drake0306.freshwall.util

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * Centralised haptic vocabulary so each interaction has a distinct signature
 * the user can feel without looking. Built on top of the View-level
 * `performHapticFeedback` API so it respects the system "Touch feedback"
 * setting — if the user has haptics disabled globally, nothing fires.
 *
 * For interactions that need a richer pattern than the built-in constants
 * cover (e.g. a "double-bump, something is opening" feel), we fall back to
 * a direct [VibrationEffect] via the system [Vibrator]. Older devices
 * without amplitude control degrade gracefully to a plain LONG_PRESS.
 */
class Haptics(
    private val view: View,
    private val vibrator: Vibrator?,
) {
    /** Standard button click — the workhorse. Use on filter chips, settings rows. */
    fun click() {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    /** Slightly lighter than [click] — for tab segment swaps. */
    fun tabSwitch() {
        val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            HapticFeedbackConstants.CONTEXT_CLICK
        } else {
            HapticFeedbackConstants.VIRTUAL_KEY
        }
        view.performHapticFeedback(constant)
    }

    /** Long-press feel for previewing a wallpaper tile. */
    fun longPress() {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    /**
     * Double-bump pattern — used for opening drawers / overlays so the user
     * feels "something just opened" instead of a single click. Two short
     * pulses with a tiny gap; on API < 26 (no VibrationEffect) we fall back
     * to a single LONG_PRESS so the action still feels different from a
     * plain click.
     */
    fun open() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vibrator != null) {
            val timings = longArrayOf(0, 14, 35, 22)
            val amplitudes = intArrayOf(0, 90, 0, 130)
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }

    /**
     * A warm, escalating tick used when the user hearts or un-hearts a
     * wallpaper. Two pulses where the second is stronger than the first —
     * reads as "yes, that landed" rather than the flat tap of a button.
     *
     * On API 30+ we use the platform CONFIRM constant where it exists; on
     * older devices we synthesise the escalation via a custom waveform.
     * Older still (no VibrationEffect at all) gracefully degrades to a
     * standard VIRTUAL_KEY so the action still produces *some* feedback.
     */
    fun like() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vibrator != null) {
            val timings = longArrayOf(0, 18, 28, 38)
            val amplitudes = intArrayOf(0, 70, 0, 170)
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }
}

@Composable
fun rememberHaptics(): Haptics {
    val view = LocalView.current
    val context = view.context
    val vibrator = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
    }
    return remember(view, vibrator) { Haptics(view, vibrator) }
}
