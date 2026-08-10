package com.example.mindspark2.camera

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.mindspark2.PreferencesManager
import com.example.mindspark2.TTSManager
import com.example.mindspark2.ui.theme.Mindspark2Theme

class CameraActivity : ComponentActivity() {

    private lateinit var ttsManager: TTSManager
    private lateinit var prefsManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        ttsManager = TTSManager(this)
        prefsManager = PreferencesManager(this)

        setContent {
            Mindspark2Theme {
                val navController = rememberNavController()

                CameraScreen(
                    navController = navController,
                    onGestureRecognized = ::onGestureRecognized
                )
            }
        }
    }

    private fun onGestureRecognized(translatedText: String) {
        if (prefsManager.autoPlayAudio) {
            ttsManager.speakAndVibrate(translatedText)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::ttsManager.isInitialized) {
            ttsManager.shutdown()
        }
    }
}