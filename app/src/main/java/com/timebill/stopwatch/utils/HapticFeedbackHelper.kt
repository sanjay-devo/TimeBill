package com.timebill.stopwatch.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object HapticFeedbackHelper {

    /**
     * Triggers a haptic vibration with the specified duration.
     * Uses VibratorManager on Android 12+ and falls back to Vibrator on older versions.
     */
    private fun vibrate(context: Context, durationMillis: Long) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        vibrator?.let {
            if (it.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    it.vibrate(
                        VibrationEffect.createOneShot(
                            durationMillis,
                            VibrationEffect.DEFAULT_AMPLITUDE
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(durationMillis)
                }
            }
        }
    }

    fun triggerStartFeedback(context: Context) {
        vibrate(context, 25)
    }

    fun triggerPauseFeedback(context: Context) {
        vibrate(context, 35)
    }

    fun triggerResumeFeedback(context: Context) {
        vibrate(context, 25)
    }

    fun triggerStopFeedback(context: Context) {
        vibrate(context, 55)
    }
}
