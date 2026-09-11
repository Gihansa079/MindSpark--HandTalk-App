package com.example.mindspark2.camera

import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
import androidx.navigation.NavController
import com.example.mindspark2.ApiService
import com.example.mindspark2.PreferencesManager
import kotlinx.coroutines.launch
import java.util.Locale

// history list එකේ එක item එකක තිබිය යුතු දත්ත ආකෘතිය (Data Structure) defin කර ගැනීම
data class HistoryModel(
    val id: Int,
    val sinhalaText: String,
    val englishText: String,
    val date: String = "Recent",
    val type: String = "IMAGE"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    navController: NavController,
    apiService: ApiService = ApiService.create()
) {
    // Android Context එක සහ Coroutine Scope එක ලබා ගැනීම
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // User preferences (dark/light theme, voice gender) manage කරන class එක
    val prefsManager = remember { PreferencesManager(context) }
    val isDark = prefsManager.theme == "Dark"

    // UI States (දත්ත වෙනස් වන විට UI එක update වීමට භාවිතා කරන variables)
    var historyList by remember { mutableStateOf<List<HistoryModel>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showClearDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<HistoryModel?>(null) }

    // TextToSpeech (TTS) Engine එක සඳහා වන Variables
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var isTtsReady by remember { mutableStateOf(false) }

    // Component එක Screen එකට එන විට TTS Initialize කිරීම සහ Screen එකෙන් යන විට shutdown කිරීම
    DisposableEffect(context) {
        val ttsEngine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true // TTS වැඩ කිරීමට සූදානම්
            } else {
                Log.e("HistoryScreen", "TTS Init Failed")
            }
        }
        tts = ttsEngine

        onDispose {
            ttsEngine.stop()
            ttsEngine.shutdown() // Memory Leaks වැළැක්වීමට TTS Engine එක නවත්වයි
        }
    }

    // පෙළ (Text) ශබ්දයට (Audio) හැරවීමේ ශ්‍රිතය (TTS Speak Function)
    fun speakText(text: String, languageCode: String) {
        if (!isTtsReady || tts == null) {
            Toast.makeText(context, "TTS Engine is initializing...", Toast.LENGTH_SHORT).show()
            return
        }
        // සිංහල හෝ ඉංග්‍රීසි භාෂාව තෝරා ගැනීම
        val locale = if (languageCode == "si") Locale("si", "LK") else Locale.US
        val result = tts?.setLanguage(locale)

        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Toast.makeText(context, "Language ($languageCode) not supported", Toast.LENGTH_SHORT).show()
            return
        }

        // Settings වල ඇති පිරිමි/ගැහැණු කටහඬ අනුව Voice Pitch එක වෙනස් කිරීම
        if (prefsManager.voiceGender.equals("Male", ignoreCase = true)) {
            tts?.setPitch(0.7f)
            tts?.setSpeechRate(0.9f)
        } else {
            tts?.setPitch(1.2f)
            tts?.setSpeechRate(1.0f)
        }

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "HistoryTTS")
    }

    // Backend API එකෙන් History දත්ත ලබා ගැනීමේ ශ්‍රිතය
    fun fetchHistory() {
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            try {
                val response = apiService.getAllTranslations()
                if (response.isSuccessful && response.body() != null) {
                    val dbList = response.body()!!
                    // show_in_history true වන දත්ත පමණක් Filter කර HistoryModel වලට Map කිරීම
                    historyList = dbList
                        .filter { it.show_in_history == true }
                        .map { translation ->
                            val imageUrl = translation.image_url ?: ""
                            val isVideo = imageUrl.contains(".mp4", ignoreCase = true) || imageUrl.contains("/video/", ignoreCase = true)

                            HistoryModel(
                                id = translation.id ?: 0,
                                sinhalaText = translation.sinhala_text ?: "N/A",
                                englishText = translation.english_text ?: "N/A",
                                date = translation.created_at?.take(10) ?: "Recent",
                                type = if (isVideo) "VIDEO" else "IMAGE"
                            )
                        }
                } else {
                    errorMessage = "Failed to load history data"
                }
            } catch (e: Exception) {
                errorMessage = "Network Error: ${e.message}"
            } finally {
                isLoading = false // Loading එක අවසන් කිරීම
            }
        }
    }

    // Screen එක load වන විට ප්‍රථමයෙන්ම fetchHistory() ක්‍රියාත්මක කිරීම
    LaunchedEffect(Unit) {
        fetchHistory()
    }

    // තනි History Item එකක් ඉවත් කිරීමේ ශ්‍රිතය
    fun deleteSingleItem(item: HistoryModel) {
        coroutineScope.launch {
            try {
                val res = apiService.hideFromHistory(item.id)
                if (res.isSuccessful) {
                    // List එකෙන් එම item එක ඉවත් කර UI එක Update කිරීම
                    historyList = historyList.filter { it.id != item.id }
                    Toast.makeText(context, "Item removed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // සියලුම History Items ඉවත් කිරීමේ ශ්‍රිතය
    fun clearAllHistory() {
        coroutineScope.launch {
            try {
                val res = apiService.hideAllFromHistory()
                if (res.isSuccessful) {
                    historyList = emptyList() // List එක සම්පූර්ණයෙන්ම හිස් කිරීම
                    Toast.makeText(context, "History cleared", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Search Box එකේ টাইප් කරන අකුරු වලට අනුව History List එක Filter කිරීම
    val filteredHistory = historyList.filter {
        it.englishText.contains(searchQuery, ignoreCase = true) ||
                it.sinhalaText.contains(searchQuery, ignoreCase = true)
    }

    // Theme එක අනුව පසුබිම් වර්ණය (Gradient Background) තෝරා ගැනීම
    val bgGradient = if (isDark) {
        listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF0F172A))
    } else {
        listOf(Color(0xFF042F62), Color(0xFF2682CC), Color(0xFFEAF6FF))
    }

    // ප්‍රධාන Screen Layout එක
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = bgGradient))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Header කොටස (ආපසු යාමේ බොත්තම, මාතෘකාව සහ Clear All බොත්තම)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Back Button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable { navController.popBackStack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "←", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "Translation History",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Data තිබේ නම් පමණක් Clear All බොත්තම පෙන්වීම
                if (historyList.isNotEmpty()) {
                    TextButton(onClick = { showClearDialog = true }) {
                        Text(text = "Clear All", color = Color(0xFFFF6B6B), fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar Input කොටස
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search translations...", color = Color.LightGray) },
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

            // දත්ත Load වන අවස්ථා (UI States: Loading / Error / Empty / Data List)
            if (isLoading) {
                // Data load වන අතරතුර පෙන්වන Circular Indicator එක
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else if (errorMessage != null) {
                // Error එකක් ආ විට පෙන්වන UI එක
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = errorMessage!!, color = Color.White, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { fetchHistory() }) { Text("Retry") }
                }
            } else if (filteredHistory.isEmpty()) {
                // දත්ත මුකුත් නැති විට හෝ Search එකට ගැළපෙන දත්ත නැති විට
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (searchQuery.isEmpty()) "No history available" else "No matching results found",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
            } else {
                // Filter වූ History Items ලැයිස්තුව (LazyColumn)
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(filteredHistory, key = { it.id }) { item ->
                        HistoryItemCard(
                            item = item,
                            isDark = isDark,
                            onPlayAudio = { text, lang -> speakText(text, lang) },
                            onDeleteClick = { itemToDelete = item }
                        )
                    }
                }
            }
        }

        // තනි Item එකක් මකා දැමීමට තහවුරු කරගන්නා Dialog එක (Delete Single Item Dialog)
        if (itemToDelete != null) {
            AlertDialog(
                onDismissRequest = { itemToDelete = null },
                title = { Text(text = "Remove History Item") },
                text = { Text(text = "Are you sure you want to remove this item?") },
                confirmButton = {
                    Button(
                        onClick = {
                            itemToDelete?.let { deleteSingleItem(it) }
                            itemToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4D4D))
                    ) { Text("Remove", color = Color.White) }
                },
                dismissButton = {
                    OutlinedButton(onClick = { itemToDelete = null }) { Text("Cancel") }
                }
            )
        }

        // සියලුම දත්ත මකා දැමීමට තහවුරු කරගන්නා Dialog එක (Clear All Dialog)
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
                    ) { Text("Clear All", color = Color.White) }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showClearDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}

// History List එකේ තනි Card එකක් නිර්මාණය කරන Component එක
@Composable
fun HistoryItemCard(
    item: HistoryModel,
    isDark: Boolean,
    onPlayAudio: (String, String) -> Unit,
    onDeleteClick: () -> Unit
) {
    // Theme එක අනුව Card එකේ වර්ණය තෝරා ගැනීම
    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White.copy(alpha = 0.95f)
    val mainTextColor = if (isDark) Color.White else Color(0xFF042F62)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Card එකේ ඉහළ කොටස (Type Icon, Date, Delete Button)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Image ද Video ද යන්න පෙන්වීම
                Text(
                    text = if (item.type == "IMAGE") "🖼️ Image" else "🎥 Video",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFF60A5FA) else Color(0xFF2682CC)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = item.date, fontSize = 12.sp, color = if (isDark) Color.LightGray else Color.Gray)
                    Spacer(modifier = Modifier.width(8.dp))
                    // Delete Button
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF6B6B))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // පරිවර්තනය වූ ඉංග්‍රීසි පෙළ
            Text(
                text = "🤟 ${item.englishText}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = mainTextColor
            )

            // පරිවර්තනය වූ සිංහල පෙළ
            Text(
                text = "සිංහල: ${item.sinhalaText}",
                fontSize = 15.sp,
                color = if (isDark) Color.LightGray else Color.DarkGray
            )

            Spacer(modifier = Modifier.height(10.dp))

            // ශබ්දය වාදනය කිරීමේ (Audio Playback) බොත්තම්
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                // සිංහලෙන් කියවීමට
                IconButton(onClick = { onPlayAudio(item.sinhalaText, "si") }, modifier = Modifier.size(32.dp)) {
                    Text(text = "🔊🇱🇰", fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                // ඉංග්‍රීසියෙන් කියවීමට
                IconButton(onClick = { onPlayAudio(item.englishText, "en") }, modifier = Modifier.size(32.dp)) {
                    Text(text = "🔊🇺🇸", fontSize = 14.sp)
                }
            }
        }
    }
}