package com.example.mindspark2

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("MindSpark2Prefs", Context.MODE_PRIVATE)

    var userEmail: String
        get() = prefs.getString("USER_EMAIL", "") ?: ""
        set(value) = prefs.edit().putString("USER_EMAIL", value).apply()

    var lastLoginTime: Long
        get() = prefs.getLong("LAST_LOGIN_TIME", 0L)
        set(value) = prefs.edit().putLong("LAST_LOGIN_TIME", value).apply()

    var voiceGender: String
        get() = prefs.getString("VOICE_GENDER", "Female") ?: "Female"
        set(value) = prefs.edit().putString("VOICE_GENDER", value).apply()

    var theme: String
        get() = prefs.getString("THEME", "Light") ?: "Light"
        set(value) = prefs.edit().putString("THEME", value).apply()

    var autoPlayAudio: Boolean
        get() = prefs.getBoolean("AUTO_PLAY_AUDIO", true)
        set(value) = prefs.edit().putBoolean("AUTO_PLAY_AUDIO", value).apply()

    var hapticFeedback: Boolean
        get() = prefs.getBoolean("HAPTIC_FEEDBACK", true)
        set(value) = prefs.edit().putBoolean("HAPTIC_FEEDBACK", value).apply()

    var language: String
        get() = prefs.getString("LANGUAGE", "English") ?: "English"
        set(value) = prefs.edit().putString("LANGUAGE", value).apply()

    fun clearSession() {
        prefs.edit().remove("USER_EMAIL").remove("LAST_LOGIN_TIME").apply()
    }
}