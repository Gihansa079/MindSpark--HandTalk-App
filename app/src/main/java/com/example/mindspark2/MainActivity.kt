
package com.example.mindspark2

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mindspark2.ui.theme.Mindspark2Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Mindspark2Theme {
                SplashScreen()
            }
        }
    }
}

@Composable
fun SplashScreen() {
    val context = LocalContext.current

    val transition = rememberInfiniteTransition(
        label = "pulse_transition"
    )

    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale_animation"
    )

    LaunchedEffect(Unit) {
        // Preserve existing model warmup behavior
        withContext(Dispatchers.IO) {
            preloadAndWarmupModels()
        }

        delay(2000)

        val intent = Intent(
            context,
            LoginActivity::class.java
        )
        context.startActivity(intent)
        (context as? ComponentActivity)?.finish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF07529E),
                        Color(0xFF1687D6),
                        Color(0xFF60C5F6)
                    )
                )
            )
    ) {

        // Soft decorative waves at the bottom
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .align(Alignment.BottomCenter)
        ) {
            drawCircle(
                color = Color.White.copy(alpha = 0.12f),
                radius = size.width * 0.8f,
                center = Offset(
                    x = size.width * 0.12f,
                    y = size.height * 1.25f
                )
            )

            drawCircle(
                color = Color(0xFFB8E8FF).copy(alpha = 0.25f),
                radius = size.width * 0.65f,
                center = Offset(
                    x = size.width * 0.95f,
                    y = size.height * 1.5f
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // Animated circular logo
            Box(
                modifier = Modifier
                    .size(125.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🤟",
                    fontSize = 76.sp
                )
            }

            Spacer(modifier = Modifier.height(25.dp))

            Text(
                text = "HandTalk",
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFFFD54F)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Breaking Communication Barriers",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Real-Time Sinhala Sign Language\nTranslator",
                fontSize = 16.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "සිංහල සංඥා භාෂා පරිවර්තකය",
                fontSize = 15.sp,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(45.dp))

            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 3.dp,
                modifier = Modifier.size(30.dp)
            )
        }

        // Footer remains near the bottom
        Text(
            text = "Powered by AI Vision Technology",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF07356D),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp)
        )
    }
}

// Existing model warmup helper
private fun preloadAndWarmupModels() {
    try {
        Thread.sleep(500)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
