package com.example.mindspark2.history

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mindspark2.PreferencesManager
import com.example.mindspark2.ui.theme.Mindspark2Theme
import java.util.Locale

// Data Model for History Item
data class HistoryItem(
    val id: String,
    val englishText: String,
    val sinhalaText: String,
    val date: String,
    val type: String // "IMAGE" or "VIDEO"
)

class HistoryActivity : ComponentActivity(), TextToSpeech.OnInitListener {

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
                    HistoryScreen(
                        isDark = isDark,
                        onBackClick = { finish() },
                        onPlayAudio = { text, lang ->
                            speakText(text, lang)
                        }
                    )
                }
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
        } else {
            Log.e("HistoryActivity", "TTS Initialization Failed")
        }
    }

    private fun speakText(text: String, languageCode: String) {
        if (!isTtsReady || tts == null) {
            Toast.makeText(this, "TTS Engine is initializing...", Toast.LENGTH_SHORT).show()
            return
        }

        val locale = if (languageCode == "si") Locale("si", "LK") else Locale.US
        val result = tts?.setLanguage(locale)

        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Toast.makeText(this, "Language ($languageCode) not supported", Toast.LENGTH_SHORT).show()
            return
        }

        if (prefsManager.voiceGender.equals("Male", ignoreCase = true)) {
            tts?.setPitch(0.7f)
            tts?.setSpeechRate(0.9f)
        } else {
            tts?.setPitch(1.2f)
            tts?.setSpeechRate(1.0f)
        }

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "HistoryTTS")
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

@Composable
fun HistoryScreen(
    isDark: Boolean,
    onBackClick: () -> Unit,
    onPlayAudio: (String, String) -> Unit
) {
    val sampleHistory = listOf(
        HistoryItem("1", "Thank You", "ස්තූතියි", "2026-03-08", "IMAGE"),
        HistoryItem("2", "Hello", "ආයුබෝවන්", "2026-03-07", "VIDEO"),
        HistoryItem("3", "Good Morning", "සුබ උදෑසනක්", "2026-03-06", "IMAGE")
    )

    val bgGradient = if (isDark) {
        listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF0F172A))
    } else {
        listOf(Color(0xFF042F62), Color(0xFF2682CC), Color(0xFFEAF6FF))
    }

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

            // --- HEADER ---
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
                    text = "Translation History",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- HISTORY LIST ---
            if (sampleHistory.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No history available",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(sampleHistory) { item ->
                        HistoryCard(
                            item = item,
                            isDark = isDark,
                            onPlayAudio = onPlayAudio
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryCard(
    item: HistoryItem,
    isDark: Boolean,
    onPlayAudio: (String, String) -> Unit
) {
    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White.copy(alpha = 0.95f)
    val mainTextColor = if (isDark) Color.White else Color(0xFF042F62)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (item.type == "IMAGE") "🖼️ Image" else "🎥 Video",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFF60A5FA) else Color(0xFF2682CC)
                )

                Text(
                    text = item.date,
                    fontSize = 12.sp,
                    color = if (isDark) Color.LightGray else Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "🤟 ${item.englishText}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = mainTextColor
            )

            Text(
                text = "සිංහල: ${item.sinhalaText}",
                fontSize = 15.sp,
                color = if (isDark) Color.LightGray else Color.DarkGray
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onPlayAudio(item.sinhalaText, "si") },
                    modifier = Modifier.size(32.dp)
                ) {
                    Text(text = "🔊🇱🇰", fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { onPlayAudio(item.englishText, "en") },
                    modifier = Modifier.size(32.dp)
                ) {
                    Text(text = "🔊🇺🇸", fontSize = 14.sp)
                }
            }
        }
    }
}