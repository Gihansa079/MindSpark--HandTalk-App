package com.example.mindspark2.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mindspark2.camera.CameraScreen
import com.example.mindspark2.camera.HistoryScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "camera_screen"
    ) {
        // Route 1: Camera / Main Screen
        composable("camera_screen") {
            CameraScreen(
                navController = navController,
                onGestureRecognized = TODO(),
            )
        }

        // Route 2: Lens History Screen
        composable("history_screen") {
            HistoryScreen(navController = navController)
        }
    }
}