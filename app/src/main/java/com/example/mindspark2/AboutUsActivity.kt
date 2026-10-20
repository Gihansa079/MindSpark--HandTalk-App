
package com.example.mindspark2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mindspark2.ui.theme.Mindspark2Theme

private val BrandBlue = Color(0xFF0868CB)
private val PageBackground = Color(0xFFF2F6FC)

class AboutUsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Mindspark2Theme {
                AboutUsScreen(
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutUsScreen(onBack: () -> Unit) {

    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "About MindSparks",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector =
                                Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrandBlue
                )
            )
        },
        containerColor = PageBackground
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {

            // APP HEADER
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BrandBlue)
                    .padding(
                        horizontal = 20.dp,
                        vertical = 30.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color.White,
                    modifier = Modifier.size(110.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.mindspark_logo),
                        contentDescription = "MindSpark logo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().padding(8.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "MindSpark",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "Breaking Communication Barriers",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(14.dp))

                Surface(
                    color = Color.White.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text(
                        text = "Version 1.0",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 7.dp
                        )
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // MISSION
            AboutSection(
                title = "Our Mission",
                icon = Icons.Default.Favorite
            ) {
                Text(
                    text = "To make communication more accessible " +
                            "through AI-powered Sinhala Sign Language " +
                            "recognition technology.",
                    color = Color(0xFF475569),
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )
            }

            // ABOUT APP
            AboutSection(
                title = "About Our App",
                icon = Icons.Default.Info
            ) {
                Text(
                    text = "MindSpark is a university research " +
                            "project designed to recognize supported " +
                            "Sinhala Sign Language gestures and provide " +
                            "meaningful text and audio outputs.",
                    color = Color(0xFF475569),
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )
            }

            // FEATURES
            AboutSection(
                title = "What MindSpark Offers",
                icon = Icons.Default.Star
            ) {

                FeatureItem(
                    "📷",
                    "Camera Recognition",
                    "Recognize supported signs using the camera"
                )

                FeatureItem(
                    "🖼️",
                    "Image Recognition",
                    "Identify supported signs from selected images"
                )

                FeatureItem(
                    "🌐",
                    "Text Translation",
                    "Display recognized words and translations"
                )

                FeatureItem(
                    "🔊",
                    "Voice Output",
                    "Listen to recognized text"
                )

                FeatureItem(
                    "🕘",
                    "Recognition History",
                    "Review previous recognition results"
                )
            }

            // DEVELOPERS
            AboutSection(
                title = "Meet the Developers",
                icon = Icons.Default.Info
            ) {
                Text(
                    text = "Team MindSparks",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandBlue
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "University Research Project",
                    color = Color(0xFF64748B),
                    fontSize = 13.sp
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "Developed with dedication to exploring " +
                            "AI-powered communication solutions.",
                    color = Color(0xFF475569),
                    fontSize = 14.sp,
                    lineHeight = 21.sp
                )
            }

            // DISCLAIMER
            AboutSection(
                title = "A Note to Our Users",
                icon = Icons.Default.Info
            ) {
                Text(
                    text = "MindSpark is a research prototype. " +
                            "Recognition accuracy may vary depending " +
                            "on hand position, lighting, image quality " +
                            "and the gestures supported by the model.",
                    color = Color(0xFF475569),
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )
            }

            Text(
                text = "Made with ❤️ by Team MindSparks\n© 2026 Team MindSparks",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                lineHeight = 20.sp,
                color = Color(0xFF64748B)
            )
        }
    }
}

@Composable
private fun AboutSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = 7.dp
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = BrandBlue
                )

                Spacer(Modifier.width(10.dp))

                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            }

            Spacer(Modifier.height(12.dp))

            content()
        }
    }
}

@Composable
private fun FeatureItem(
    icon: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Surface(
            color = Color(0xFFEAF3FF),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = icon,
                    fontSize = 23.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = Color(0xFF1E293B)
            )

            Spacer(Modifier.height(3.dp))

            Text(
                text = description,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = Color(0xFF64748B)
            )
        }
    }
}

