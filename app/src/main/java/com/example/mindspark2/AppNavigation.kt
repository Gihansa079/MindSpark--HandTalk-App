package com.example.mindspark2.navigation

import androidx.compose.runtime.Composable
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
    // Screens අතර මාරු වීම (Navigation) පාලනය කිරීමට NavController එක සාදා ගැනීම
    val navController = rememberNavController()

    // UI එක ඇතුලත Android Context එක ලබා ගැනීම (TTS / Preferences සඳහා අවශ්‍ය වේ)
    val context = LocalContext.current

    // Re-composition සිදුවන විට TTSManager සහ PreferencesManager හි Instances නැවත නැවත සෑදීම වැළැක්වීමට remember භාවිතය
    val ttsManager = remember { TTSManager(context) }
    val prefsManager = remember { PreferencesManager(context) }

    NavHost(
        navController = navController,
        startDestination = "camera_screen" // ඇප් එක open වූ විට ප්‍රථමයෙන්ම පෙන්වන Screen එක
    ) {
        // Route 1: Camera / Sign Language Detection Screen
        composable("camera_screen") {
            CameraScreen(
                navController = navController,
                onGestureRecognized = { translatedText ->
                    // පරිශීලකයා Auto Play Audio සක්‍රිය කර ඇත්නම් පමණක් හඳුනාගත් පෙළ Voice ලෙස Play කරයි
                    if (prefsManager.autoPlayAudio) {
                        ttsManager.speakAndVibrate(translatedText)
                    }
                }
            )
        }

        // Route 2: History Screen (පෙර හඳුනාගත් සයින් ලැන්වේජ් සටහන් ලැයිස්තුව)
        composable("history_screen") {
            HistoryScreen(navController = navController)
        }
    }
}