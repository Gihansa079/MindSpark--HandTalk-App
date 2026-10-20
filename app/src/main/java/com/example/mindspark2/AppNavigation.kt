
package com.example.mindspark2.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import com.example.mindspark2.PreferencesManager
import com.example.mindspark2.TTSManager
import com.example.mindspark2.camera.CameraScreen
import com.example.mindspark2.camera.HistoryScreen

@Composable
fun AppNavigation() {

    val navController = rememberNavController()
    val context = LocalContext.current

    val prefsManager = remember(context) {
        PreferencesManager(context)
    }

    val ttsManager = remember(context) {
        TTSManager(context)
    }

    DisposableEffect(ttsManager) {
        onDispose {
            ttsManager.shutdown()
        }
    }

    // Logged-in user's ID
    val userId = prefsManager.userId

    NavHost(
        navController = navController,
        startDestination = "camera_screen"
    ) {

        // Camera Screen
        composable("camera_screen") {
            CameraScreen(
                navController = navController,
                userId = userId,
                onGestureRecognized = { translatedText ->
                    if (prefsManager.autoPlayAudio) {
                        ttsManager.speakAndVibrate(translatedText)
                    }
                }
            )
        }

        // History Screen
        composable("history_screen") {
            HistoryScreen(
                navController = navController,
                userId = userId
            )
        }
    }
}
