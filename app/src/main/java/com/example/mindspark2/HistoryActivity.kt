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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mindspark2.ApiService
import com.example.mindspark2.PreferencesManager
import com.example.mindspark2.ui.theme.Mindspark2Theme
import kotlinx.coroutines.launch
import java.util.Locale

// UI එකේ Display කිරීමට භාවිත කරන History Item Data Structure එක
data class HistoryItem(
    val id: String,
    val englishText: String,
    val sinhalaText: String,
    val date: String,
    val type: String, // "IMAGE" හෝ "VIDEO"
    val showInHistory: Boolean = true
)

// History Activity එක - TextToSpeech සහ Context / Theme කළමනාකරණය කරයි
class HistoryActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private lateinit var prefsManager: PreferencesManager
    private var currentTheme: String = "Light"

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Edge-to-edge UI support සක්‍රිය කිරීම

        prefsManager = PreferencesManager(this)
        currentTheme = prefsManager.theme

        // TextToSpeech Engine එක Initialize කිරීම
        tts = TextToSpeech(this, this)

        applyUI()
    }

    // Jetpack Compose UI එක Render කිරීම
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
                        onBackClick = { finish() }, // Screen එක Close කිරීම
                        onPlayAudio = { text, lang ->
                            speakText(text, lang) // ශබ්ද විකාශනය කිරීමේ Function එක Call කිරීම
                        }
                    )
                }
            }
        }
    }

    // TextToSpeech Engine එක Ready වූ විට ක්‍රියාත්මක වන Callback එක
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
        } else {
            Log.e("HistoryActivity", "TTS Initialization Failed")
        }
    }

    // පෙළ හඬ බවට පරිවර්තනය කර කියවීමේ Function එක (Sinhala / English)
    private fun speakText(text: String, languageCode: String) {
        if (!isTtsReady || tts == null) {
            Toast.makeText(this, "TTS Engine is initializing...", Toast.LENGTH_SHORT).show()
            return
        }

        // භාෂාව තේරීම (si -> Sinhala, en -> US English)
        val locale = if (languageCode == "si") Locale("si", "LK") else Locale.US
        val result = tts?.setLanguage(locale)

        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Toast.makeText(this, "Language ($languageCode) not supported", Toast.LENGTH_SHORT).show()
            return
        }

        // Settings වල ඇති Gender Preference අනුව Voice Pitch එක වෙනස් කිරීම
        if (prefsManager.voiceGender.equals("Male", ignoreCase = true)) {
            tts?.setPitch(0.7f) // පහළ Pitch එක (පිරිමි හඬට සමාන)
            tts?.setSpeechRate(0.9f)
        } else {
            tts?.setPitch(1.2f) // ඉහළ Pitch එක (ගැහැණු හඬට සමාන)
            tts?.setSpeechRate(1.0f)
        }

        // ශබ්දය සක්‍රිය කිරීම
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "HistoryTTS")
    }

    // Screen එක නැවත Active වන විට Theme එක වෙනස් වී ඇත්නම් Recreate කිරීම
    override fun onResume() {
        super.onResume()
        if (::prefsManager.isInitialized && prefsManager.theme != currentTheme) {
            currentTheme = prefsManager.theme
            recreate()
        }
    }

    // Activity එක Destroy වන විට Memory Leak වැළැක්වීමට TTS Stop/Shutdown කිරීම
    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

// History UI Screen එක නිරූපණය කරන Composable Function එක
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    isDark: Boolean,
    onBackClick: () -> Unit,
    onPlayAudio: (String, String) -> Unit,
    apiService: ApiService = ApiService.create()
) {
    val context = LocalContext.current
    var historyList by remember { mutableStateOf<List<HistoryItem>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showClearDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<HistoryItem?>(null) }

    val coroutineScope = rememberCoroutineScope()

    // Backend එකෙන් පරිවර්තන දත්ත ලබාගැනීමේ Function එක
    fun fetchHistory() {
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            try {
                val response = apiService.getAllTranslations()
                if (response.isSuccessful && response.body() != null) {
                    val dbList = response.body()!!

                    // show_in_history == true වන Records පමණක් Filter කර UI Model එකට Map කිරීම
                    historyList = dbList
                        .filter { it.show_in_history == true }
                        .map { translation ->
                            val imageUrl = translation.image_url ?: ""
                            val isVideo = imageUrl.contains(".mp4", ignoreCase = true) || imageUrl.contains("/video/", ignoreCase = true)

                            HistoryItem(
                                id = translation.id?.toString() ?: "",
                                englishText = translation.english_text ?: "N/A",
                                sinhalaText = translation.sinhala_text ?: "N/A",
                                date = translation.created_at?.take(10) ?: "Recent",
                                type = if (isVideo) "VIDEO" else "IMAGE",
                                showInHistory = translation.show_in_history ?: true
                            )
                        }
                } else {
                    errorMessage = "Failed to load history from DB"
                }
            } catch (e: Exception) {
                errorMessage = "Network Error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    // Screen එක Load වන විට දත්ත Fetch කිරීම
    LaunchedEffect(Unit) {
        fetchHistory()
    }

    // Soft delete logic: Backend Database එකේ show_in_history = false කිරීම
    fun deleteSingleItem(item: HistoryItem) {
        coroutineScope.launch {
            try {
                val itemId = item.id.toIntOrNull() ?: 0
                val response = apiService.hideFromHistory(itemId)
                if (response.isSuccessful) {
                    historyList = historyList.filter { it.id != item.id }
                    Toast.makeText(context, "Item removed from history", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to hide item in DB", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Soft clear all logic: Database එකේ සියලුම Records වල show_in_history = false කිරීම
    fun clearAllHistory() {
        coroutineScope.launch {
            try {
                val response = apiService.hideAllFromHistory()
                if (response.isSuccessful) {
                    historyList = emptyList()
                    Toast.makeText(context, "History cleared successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to clear history in DB", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Search Query එකට අනුව History Items Filter කිරීම (English හෝ Sinhala Text අනුව)
    val filteredHistory = historyList.filter {
        it.englishText.contains(searchQuery, ignoreCase = true) ||
                it.sinhalaText.contains(searchQuery, ignoreCase = true)
    }

    // Dark/Light Theme එක අනුව Background Gradient එක සකස් කිරීම
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

            // Header Section (Back Button, Title සහ Clear All)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // History එකේ දත්ත තිබේ නම් පමණක් "Clear All" බොත්තම පෙන්වීම
                if (historyList.isNotEmpty()) {
                    TextButton(onClick = { showClearDialog = true }) {
                        Text(text = "Clear All", color = Color(0xFFFF6B6B), fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar (සෙවුම් තීරුව)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search translations...", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.15f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.1f),
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Statuses අනුව UI වෙනස් වීම (Loading, Error, Empty, Search Results)
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else if (errorMessage != null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = errorMessage!!,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { fetchHistory() }) {
                        Text("Retry")
                    }
                }
            } else if (filteredHistory.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isEmpty()) "No history available" else "No matching results found",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
            } else {
                // Filter වූ History Items ලැයිස්තුව LazyColumn එකක් ලෙස Render කිරීම
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(filteredHistory, key = { it.id }) { item ->
                        HistoryCard(
                            item = item,
                            isDark = isDark,
                            onPlayAudio = onPlayAudio,
                            onDeleteClick = { itemToDelete = item }
                        )
                    }
                }
            }
        }

        // --- SINGLE ITEM DELETE CONFIRMATION DIALOG ---
        if (itemToDelete != null) {
            AlertDialog(
                onDismissRequest = { itemToDelete = null },
                title = { Text(text = "Remove History Item") },
                text = { Text(text = "Are you sure you want to remove this item from history?") },
                confirmButton = {
                    Button(
                        onClick = {
                            itemToDelete?.let { deleteSingleItem(it) }
                            itemToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4D4D))
                    ) {
                        Text("Remove", color = Color.White)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { itemToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // --- CLEAR ALL CONFIRMATION DIALOG ---
        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                title = { Text(text = "Clear History") },
                text = { Text(text = "Are you sure you want to clear all history?") },
                confirmButton = {
                    Button(
                        onClick = {
                            clearAllHistory()
                            showClearDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4D4D))
                    ) {
                        Text("Clear All", color = Color.White)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showClearDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

// තනි History Item එකක් UI එකේ පෙන්වන Card Component එක
@Composable
fun HistoryCard(
    item: HistoryItem,
    isDark: Boolean,
    onPlayAudio: (String, String) -> Unit,
    onDeleteClick: () -> Unit
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
                // Media Type Badge එක (Image ද Video ද යන්න)
                Text(
                    text = if (item.type == "IMAGE") "🖼️ Image" else "🎥 Video",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFF60A5FA) else Color(0xFF2682CC)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.date,
                        fontSize = 12.sp,
                        color = if (isDark) Color.LightGray else Color.Gray
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Delete Button
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove Item",
                            tint = Color(0xFFFF6B6B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // English Translation Text
            Text(
                text = "🤟 ${item.englishText}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = mainTextColor
            )

            // Sinhala Translation Text
            Text(
                text = "සිංහල: ${item.sinhalaText}",
                fontSize = 15.sp,
                color = if (isDark) Color.LightGray else Color.DarkGray
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Audio Playback Buttons (Sinhala & English TTS)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sinhala Audio Playback Button
                IconButton(
                    onClick = { onPlayAudio(item.sinhalaText, "si") },
                    modifier = Modifier.size(32.dp)
                ) {
                    Text(text = "🔊🇱🇰", fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                // English Audio Playback Button
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