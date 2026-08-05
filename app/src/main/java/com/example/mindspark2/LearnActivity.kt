package com.example.mindspark2.learn

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mindspark2.PreferencesManager
import com.example.mindspark2.ui.theme.Mindspark2Theme
import java.util.Locale

class LearnActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private lateinit var prefsManager: PreferencesManager
    private var currentTheme: String = "Light"

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        prefsManager = PreferencesManager(this)
        currentTheme = prefsManager.theme
        tts = TextToSpeech(this, this)

        applyUI()
    }

    private fun applyUI() {
        val isDark = currentTheme == "Dark"

        setContent {
            Mindspark2Theme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LearnScreen(
                        isDark = isDark,
                        onBackClick = { finish() },
                        onPlayAudio = { sinhalaText ->
                            speakText(sinhalaText)
                        }
                    )
                }
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("si", "LK"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.language = Locale.US
            }
            isTtsReady = true
        } else {
            Log.e("LearnActivity", "TTS Initialization Failed")
        }
    }

    private fun speakText(text: String) {
        if (!isTtsReady || tts == null) {
            Toast.makeText(this, "TTS Engine is initializing...", Toast.LENGTH_SHORT).show()
            return
        }

        if (prefsManager.voiceGender.equals("Male", ignoreCase = true)) {
            tts?.setPitch(0.7f)
            tts?.setSpeechRate(0.9f)
        } else {
            tts?.setPitch(1.2f)
            tts?.setSpeechRate(1.0f)
        }

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "LearnTTS")
    }

    override fun onResume() {
        super.onResume()
        if (::prefsManager.isInitialized && prefsManager.theme != currentTheme) {
            currentTheme = prefsManager.theme
            recreate()
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

data class SignItem(
    val icon: String,
    val englishText: String,
    val sinhalaText: String,
    val category: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnScreen(
    isDark: Boolean,
    onBackClick: () -> Unit,
    onPlayAudio: (String) -> Unit
) {
    val categories = listOf("All", "Alphabet", "Numbers", "Greetings", "Daily")
    var selectedCategory by remember { mutableStateOf("All") }

    val signList = remember {
        listOf(
            SignItem("🅰️", "A", "අ", "Alphabet"),
            SignItem("🅱️", "B", "බ", "Alphabet"),
            SignItem("1️⃣", "One", "එක", "Numbers"),
            SignItem("2️⃣", "Two", "දෙක", "Numbers"),
            SignItem("👋", "Hello", "ආයුබෝවන්", "Greetings"),
            SignItem("🙏", "Thank You", "ස්තූතියි", "Greetings"),
            SignItem("🤝", "Please", "කරුණාකර", "Greetings"),
            SignItem("🥛", "Water", "වතුර", "Daily"),
            SignItem("🍚", "Food", "කෑම", "Daily"),
            SignItem("🏠", "Home", "ගෙදර", "Daily")
        )
    }

    val filteredList = if (selectedCategory == "All") {
        signList
    } else {
        signList.filter { it.category == selectedCategory }
    }

    val bgGradient = if (isDark) {
        listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF0F172A))
    } else {
        listOf(Color(0xFF042F62), Color(0xFF2682CC), Color(0xFFEAF6FF))
    }

    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White.copy(alpha = 0.95f)
    val cardTextColor = if (isDark) Color.White else Color(0xFF042F62)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = bgGradient))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 20.dp)
        ) {

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Text(
                        text = "←",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = "Learn Sign Language",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            ScrollableTabRow(
                selectedTabIndex = categories.indexOf(selectedCategory),
                containerColor = Color.Transparent,
                contentColor = Color.White,
                edgePadding = 0.dp,
                divider = {}
            ) {
                categories.forEach { category ->
                    Tab(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        text = {
                            Text(
                                text = category,
                                fontSize = 15.sp,
                                fontWeight = if (selectedCategory == category) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedCategory == category) Color.White else Color.White.copy(alpha = 0.7f)
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 20.dp)
            ) {
                items(filteredList) { sign ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = sign.icon,
                                fontSize = 42.sp
                            )

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = sign.englishText,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = cardTextColor,
                                    textAlign = TextAlign.Center
                                )

                                Text(
                                    text = "සිංහල: ${sign.sinhalaText}",
                                    fontSize = 14.sp,
                                    color = if (isDark) Color.LightGray else Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }

                            IconButton(
                                onClick = { onPlayAudio(sign.sinhalaText) },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Text(text = "🔊", fontSize = 18.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}