package com.example.mindspark2.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF042F62),
    secondary = Color(0xFF2682CC),
    tertiary = Color(0xFFD3E9FA),
    background = Color(0xFF042F62),
    surface = Color(0xFF042F62)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF042F62),
    secondary = Color(0xFF2682CC),
    tertiary = Color(0xFFD3E9FA),
    background = Color(0xFF042F62),
    surface = Color(0xFF042F62)
)

@Composable
fun Mindspark2Theme(
    darkTheme: Boolean = false, // Defaults to false so splash & main screens stay bright blue
    dynamicColor: Boolean = false, // Set to false to prevent device wallpaper from painting the screen black
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}