package com.example.mindspark2

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.mindspark2.camera.CameraActivity
import com.example.mindspark2.history.HistoryActivity
import com.example.mindspark2.image.ImageActivity
import com.example.mindspark2.learn.LearnActivity
import com.example.mindspark2.ui.theme.Mindspark2Theme
import kotlinx.coroutines.launch
import java.util.Locale

class HomeActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private lateinit var prefsManager: PreferencesManager
    private var currentTheme: String = "Light"
    private var selectedVoiceGender = mutableStateOf("Female")
    private var loggedInUserName: String = "User"

    private var latestEnglishText = mutableStateOf("Hello")
    private var latestSinhalaText = mutableStateOf("ආයුබෝවන්")

    private val apiService by lazy { ApiService.create() }

    private val requestAudioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Audio permission granted!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Audio permission is required for voice features", Toast.LENGTH_SHORT).show()
        }
    }

    private val profileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val returnedVoice = result.data?.getStringExtra("SELECTED_VOICE")
            if (returnedVoice != null) {
                selectedVoiceGender.value = returnedVoice
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        checkAudioPermission()

        loggedInUserName = intent.getStringExtra("USER_NAME") ?: "User"

        prefsManager = PreferencesManager(this)
        currentTheme = prefsManager.theme
        selectedVoiceGender.value = prefsManager.voiceGender

        tts = TextToSpeech(this, this)

        fetchLatestTranslation()

        applyUI()
    }

    private fun checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun fetchLatestTranslation() {
        lifecycleScope.launch {
            try {
                val response = apiService.getLatestTranslation()
                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    latestEnglishText.value = data.english_text ?: "Hello"
                    latestSinhalaText.value = data.sinhala_text ?: "ආයුබෝවන්"
                }
            } catch (e: Exception) {
                Log.e("HomeActivity", "PostgreSQL Fetch Error: ${e.message}")
            }
        }
    }

    private fun applyUI() {
        val isDark = currentTheme == "Dark"

        setContent {
            Mindspark2Theme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeScreen(
                        userName = loggedInUserName,
                        currentVoiceGender = selectedVoiceGender.value,
                        isDark = isDark,
                        latestEnglishState = latestEnglishText,
                        latestSinhalaState = latestSinhalaText,
                        onOpenProfile = { openProfileActivity() },
                        onPlayAudio = { text, languageCode ->
                            val isFemale = selectedVoiceGender.value.equals("Female", ignoreCase = true)
                            speakText(text, languageCode, isFemale)
                        },
                        onExitApp = { finishAffinity() } // FR 43: Safe App Exit
                    )
                }
            }
        }
    }

    private fun openProfileActivity() {
        val intent = Intent(this, ProfileActivity::class.java).apply {
            putExtra("SELECTED_VOICE", selectedVoiceGender.value)
        }
        profileLauncher.launch(intent)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
        } else {
            Log.e("TTS", "Initialization Failed!")
        }
    }

    private fun speakText(text: String, languageCode: String, isFemale: Boolean) {
        if (!isTtsReady || tts == null) {
            Toast.makeText(this, "TTS Engine is initializing...", Toast.LENGTH_SHORT).show()
            return
        }

        val locale = when (languageCode) {
            "si" -> Locale("si", "LK")
            else -> Locale.US
        }

        val result = tts?.setLanguage(locale)

        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Toast.makeText(this, "Language ($languageCode) not supported on this device", Toast.LENGTH_SHORT).show()
            return
        }

        var voiceSet = false
        val availableVoices = tts?.voices

        if (availableVoices != null) {
            for (voice in availableVoices) {
                if (voice.locale.language == locale.language) {
                    val voiceName = voice.name.lowercase()
                    if (isFemale && (voiceName.contains("female") || voiceName.contains("woman") || voiceName.contains("f00"))) {
                        tts?.voice = voice
                        voiceSet = true
                        break
                    } else if (!isFemale && (voiceName.contains("male") || voiceName.contains("man") || voiceName.contains("m00"))) {
                        tts?.voice = voice
                        voiceSet = true
                        break
                    }
                }
            }
        }

        if (!voiceSet) {
            if (isFemale) {
                tts?.setPitch(1.3f)
                tts?.setSpeechRate(1.0f)
            } else {
                tts?.setPitch(0.7f)
                tts?.setSpeechRate(0.95f)
            }
        }

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "MindsparkTTS")
    }

    override fun onResume() {
        super.onResume()
        fetchLatestTranslation()

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
fun HomeScreen(
    userName: String = "User",
    currentVoiceGender: String,
    isDark: Boolean,
    latestEnglishState: State<String>,
    latestSinhalaState: State<String>,
    onOpenProfile: () -> Unit,
    onPlayAudio: (text: String, languageCode: String) -> Unit,
    onExitApp: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var showExitDialog by remember { mutableStateOf(false) }

    // FR 43: Back button handler for exit confirmation
    BackHandler {
        showExitDialog = true
    }

    val bgGradient = if (isDark) {
        listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF0F172A))
    } else {
        listOf(Color(0xFF042F62), Color(0xFF2682CC), Color(0xFFEAF6FF))
    }

    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White.copy(alpha = 0.95f)
    val cardTextColor = if (isDark) Color.White else Color.Black
    val featureCardBg1 = if (isDark) Color(0xFF0369A1) else Color(0xFFE0F2FE)
    val featureCardBg2 = if (isDark) Color(0xFF15803D) else Color(0xFFDCFCE7)
    val featureCardBg3 = if (isDark) Color(0xFFB45309) else Color(0xFFFEF3C7)
    val featureTextColor = if (isDark) Color.White else Color(0xFF0F172A)

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            NavigationBar(
                containerColor = if (isDark) Color(0xFF1E293B) else Color.White
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Text("🏠") },
                    label = { Text("Home") }
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        context.startActivity(Intent(context, CameraActivity::class.java))
                    },
                    icon = { Text("📷") },
                    label = { Text("Camera") }
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
                        context.startActivity(Intent(context, LearnActivity::class.java))
                    },
                    icon = { Text("📚") },
                    label = { Text("Learn") }
                )

                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = {
                        selectedTab = 3
                        onOpenProfile()
                    },
                    icon = { Text("👤") },
                    label = { Text("Profile") }
                )
            }
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(colors = bgGradient))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(bottom = paddingValues.calculateBottomPadding())
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Hello, $userName 👋",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Let's communicate freely",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable { onOpenProfile() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "👤",
                            fontSize = 26.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(30.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🤖",
                                fontSize = 35.sp
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = "AI Vision Ready",
                                    fontSize = 20.sp,
                                    color = cardTextColor,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = "Real-time Sinhala Sign Recognition",
                                    color = if (isDark) Color.LightGray else Color.Gray,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = {
                                context.startActivity(Intent(context, CameraActivity::class.java))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF042F62)
                            )
                        ) {
                            Text(
                                text = "📷 Start Translation",
                                fontSize = 18.sp,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Explore Features",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                FeatureCard(
                    modifier = Modifier.fillMaxWidth(),
                    icon = "🖼",
                    title = "Image Recognition",
                    backgroundColor = featureCardBg1,
                    textColor = featureTextColor,
                    onClick = {
                        val intent = Intent(context, ImageActivity::class.java).apply {
                            putExtra("SELECTED_VOICE", currentVoiceGender)
                        }
                        context.startActivity(intent)
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FeatureCard(
                        modifier = Modifier.weight(1f),
                        icon = "📚",
                        title = "Learn Signs",
                        backgroundColor = featureCardBg2,
                        textColor = featureTextColor,
                        onClick = {
                            context.startActivity(Intent(context, LearnActivity::class.java))
                        }
                    )

                    FeatureCard(
                        modifier = Modifier.weight(1f),
                        icon = "🕒",
                        title = "History Log",
                        backgroundColor = featureCardBg3,
                        textColor = featureTextColor,
                        onClick = {
                            val intent = Intent(context, HistoryActivity::class.java)
                            context.startActivity(intent)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(25.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Latest Translation",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = if (isDark) Color(0xFF60A5FA) else Color(0xFF042F62)
                            )

                            SuggestionChip(
                                onClick = { onOpenProfile() },
                                label = {
                                    Text(
                                        text = if (currentVoiceGender.equals("Female", ignoreCase = true)) "👩 Female Voice" else "👨 Male Voice",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "🤟 ${latestEnglishState.value}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = cardTextColor
                        )

                        Text(
                            text = "සිංහල : ${latestSinhalaState.value}",
                            color = if (isDark) Color.LightGray else Color.DarkGray,
                            fontSize = 16.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onPlayAudio(latestSinhalaState.value, "si") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("🔊 Sinhala")
                            }

                            OutlinedButton(
                                onClick = { onPlayAudio(latestEnglishState.value, "en") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("🔊 English")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // FR 43 Exit Application Dialog
            if (showExitDialog) {
                AlertDialog(
                    onDismissRequest = { showExitDialog = false },
                    title = { Text("Exit Application") },
                    text = { Text("Are you sure you want to exit MindSpark?") },
                    confirmButton = {
                        Button(
                            onClick = {
                                showExitDialog = false
                                onExitApp()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4D4D))
                        ) {
                            Text("Exit", color = Color.White)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { showExitDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureCard(
    modifier: Modifier = Modifier,
    icon: String,
    title: String,
    backgroundColor: Color,
    textColor: Color = Color(0xFF0F172A),
    onClick: () -> Unit = {}
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(105.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = icon,
                fontSize = 32.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = textColor
            )
        }
    }
}