package com.example.mindspark2.camera

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.mindspark2.TTSManager
import com.example.mindspark2.ui.theme.Mindspark2Theme

class CameraActivity : ComponentActivity() {

    private lateinit var ttsManager: TTSManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Activity එක Level එකේදී TTSManager Initialize කිරීම
        ttsManager = TTSManager(this)

        setContent {
            Mindspark2Theme {
                val navController = rememberNavController()

                // CameraScreen එකට ttsManager එක Pass කිරීම
                CameraScreen(
                    navController = navController,
                    onGestureRecognized = ::onGestureRecognized
                )
            }
        }
    }

    // Gesture/Sign එක Detect වූ පසු sound සහ vibration ක්‍රියාත්මක වන Function එක
    fun onGestureRecognized(translatedText: String) {
        ttsManager.speakAndVibrate(translatedText)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Activity එක Close වෙද්දී Memory Leaks නැති කිරීමට TTS Stop කරයි
        if (::ttsManager.isInitialized) {
            ttsManager.shutdown()
        }
    }
}