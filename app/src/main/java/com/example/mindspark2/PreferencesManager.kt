package com.example.mindspark2

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class PreferencesManager(context: Context) {

    // Existing app preferences
    private val prefs: SharedPreferences =
        context.getSharedPreferences(
            "MindSpark2Prefs",
            Context.MODE_PRIVATE
        )

    // Encrypted storage for authentication token
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val securePrefs: SharedPreferences =
        EncryptedSharedPreferences.create(
            context,
            "MindSpark2SecurePrefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

    // Logged-in user email
    var userEmail: String
        get() = prefs.getString("USER_EMAIL", "") ?: ""
        set(value) {
            prefs.edit().putString("USER_EMAIL", value).apply()
        }

    // PostgreSQL user_profile.id
    var userId: Int
        get() = prefs.getInt("USER_ID", -1)
        set(value) {
            prefs.edit().putInt("USER_ID", value).apply()
        }

    var lastLoginTime: Long
        get() = prefs.getLong("LAST_LOGIN_TIME", 0L)
        set(value) {
            prefs.edit().putLong("LAST_LOGIN_TIME", value).apply()
        }

    // Secure session token
    var sessionToken: String?
        get() = securePrefs.getString("SESSION_TOKEN", null)
        set(value) {
            securePrefs.edit()
                .putString("SESSION_TOKEN", value)
                .apply()
        }

    // Voice settings
    var voiceGender: String
        get() = prefs.getString("VOICE_GENDER", "Female") ?: "Female"
        set(value) {
            prefs.edit().putString("VOICE_GENDER", value).apply()
        }

    // Theme settings
    var theme: String
        get() = prefs.getString("THEME", "Light") ?: "Light"
        set(value) {
            prefs.edit().putString("THEME", value).apply()
        }

    var autoPlayAudio: Boolean
        get() = prefs.getBoolean("AUTO_PLAY_AUDIO", true)
        set(value) {
            prefs.edit().putBoolean("AUTO_PLAY_AUDIO", value).apply()
        }

    var hapticFeedback: Boolean
        get() = prefs.getBoolean("HAPTIC_FEEDBACK", true)
        set(value) {
            prefs.edit().putBoolean("HAPTIC_FEEDBACK", value).apply()
        }

    var language: String
        get() = prefs.getString("LANGUAGE", "English") ?: "English"
        set(value) {
            prefs.edit().putString("LANGUAGE", value).apply()
        }

    // Clear authentication data on logout
    fun clearSession() {
        securePrefs.edit()
            .remove("SESSION_TOKEN")
            .apply()

        prefs.edit()
            .remove("USER_EMAIL")
            .remove("USER_ID")
            .remove("LAST_LOGIN_TIME")
            .apply()
    }
}