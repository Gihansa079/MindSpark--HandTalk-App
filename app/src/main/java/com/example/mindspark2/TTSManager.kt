package com.example.mindspark2

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import java.util.Locale

class TTSManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context, this)
    private var isInitialized = false
    private val prefsManager = PreferencesManager(context)

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("si", "LK"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.language = Locale.US
            }
            isInitialized = true
        }
    }

    fun speakAndVibrate(text: String) {
        // Haptic Feedback Check
        if (prefsManager.hapticFeedback) {
            triggerVibration()
        }

        // Auto Play Voice Check
        if (prefsManager.autoPlayAudio && isInitialized && text.isNotBlank()) {
            if (prefsManager.voiceGender.equals("Male", ignoreCase = true)) {
                tts?.setPitch(0.7f)
                tts?.setSpeechRate(0.9f)
            } else {
                tts?.setPitch(1.2f)
                tts?.setSpeechRate(1.0f)
            }
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TTS_ID")
        }
    }

    private fun triggerVibration() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator?.vibrate(
                VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.vibrate(
                VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}