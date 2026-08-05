package com.example.mindspark2

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)

    var autoPlayAudio: Boolean
        get() = prefs.getBoolean("AUTO_PLAY_AUDIO", true)
        set(value) = prefs.edit().putBoolean("AUTO_PLAY_AUDIO", value).apply()

    var hapticFeedback: Boolean
        get() = prefs.getBoolean("HAPTIC_FEEDBACK", true)
        set(value) = prefs.edit().putBoolean("HAPTIC_FEEDBACK", value).apply()

    var language: String
        get() = prefs.getString("APP_LANGUAGE", "English") ?: "English"
        set(value) = prefs.edit().putString("APP_LANGUAGE", value).apply()

    var voiceGender: String
        get() = prefs.getString("SELECTED_VOICE", "Female") ?: "Female"
        set(value) = prefs.edit().putString("SELECTED_VOICE", value).apply()

    var theme: String
        get() = prefs.getString("APP_THEME", "Light") ?: "Light"
        set(value) = prefs.edit().putString("APP_THEME", value).apply()
}