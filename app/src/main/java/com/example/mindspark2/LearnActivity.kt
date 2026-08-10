package com.example.mindspark2.learn

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.mindspark2.ApiService
import com.example.mindspark2.PreferencesManager
import com.example.mindspark2.ui.theme.Mindspark2Theme
import kotlinx.coroutines.launch
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
                        onPlayAudio = { text, isSinhala ->
                            speakText(text, isSinhala)
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
            Log.e("LearnActivity", "TTS Initialization Failed")
        }
    }

    private fun applyVoicePreferences() {
        if (prefsManager.voiceGender.equals("Male", ignoreCase = true)) {
            tts?.setPitch(0.7f)
            tts?.setSpeechRate(0.9f)
        } else {
            tts?.setPitch(1.2f)
            tts?.setSpeechRate(1.0f)
        }
    }

    private fun speakText(text: String, isSinhala: Boolean) {
        if (!isTtsReady || tts == null) {
            Toast.makeText(this, "TTS Engine is initializing...", Toast.LENGTH_SHORT).show()
            return
        }

        if (isSinhala) {
            val result = tts?.setLanguage(Locale("si", "LK"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.language = Locale.US
            }
        } else {
            tts?.language = Locale.US
        }

        applyVoicePreferences()
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
    val translationId: Int? = null,
    val dbImageUrl: String?,
    val englishText: String,
    val sinhalaText: String,
    val category: String,
    val isFavorite: Boolean = false
)

class LocalFavoriteManager(context: Context) {
    private val prefs = context.getSharedPreferences("local_favorites_prefs", Context.MODE_PRIVATE)

    fun getFavoriteSet(): Set<String> {
        return prefs.getStringSet("favorite_items", emptySet()) ?: emptySet()
    }

    fun toggleFavorite(englishText: String): Boolean {
        val currentSet = getFavoriteSet().toMutableSet()
        val key = englishText.trim().lowercase()
        val isNowFavorite: Boolean

        if (currentSet.contains(key)) {
            currentSet.remove(key)
            isNowFavorite = false
        } else {
            currentSet.add(key)
            isNowFavorite = true
        }

        prefs.edit().putStringSet("favorite_items", currentSet).apply()
        return isNowFavorite
    }
}

@OptIn(UnstableApi::class)
@Composable
fun DynamicVideoPlayer(videoUrl: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val formattedUrl = remember(videoUrl) {
        videoUrl
            .trim()
            .replace("localhost", "10.0.2.2")
            .replace(" ", "%20")
            .replace("(", "%28")
            .replace(")", "%29")
    }

    key(formattedUrl) {
        val exoPlayer = remember {
            val dataSourceFactory = DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)

            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(formattedUrl.toUri()))

            ExoPlayer.Builder(context).build().apply {
                setMediaSource(mediaSource)
                repeatMode = Player.REPEAT_MODE_ONE
                prepare()
                playWhenReady = true
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                exoPlayer.release()
            }
        }

        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                }
            },
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnScreen(
    isDark: Boolean,
    onBackClick: () -> Unit,
    onPlayAudio: (text: String, isSinhala: Boolean) -> Unit,
    apiService: ApiService = ApiService.create()
) {
    val context = LocalContext.current
    val favoriteManager = remember { LocalFavoriteManager(context) }

    val categories = listOf("All", "Favorites ⭐", "Alphabet", "Numbers", "Months", "Verbs", "SSL Sentences", "Additional Words")
    var selectedCategory by remember { mutableStateOf("All") }

    var signList by remember { mutableStateOf<List<SignItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var selectedSign by remember { mutableStateOf<SignItem?>(null) }
    var selectedVariantIndex by remember { mutableStateOf(0) }

    val coroutineScope = rememberCoroutineScope()

    fun fetchTranslations() {
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            try {
                val response = apiService.getAllTranslations()
                val savedFavSet = favoriteManager.getFavoriteSet()

                if (response.isSuccessful && response.body() != null) {
                    val dbList = response.body()!!

                    signList = dbList.map { translation ->
                        val english = translation.english_text?.trim() ?: "N/A"
                        val sinhala = translation.sinhala_text?.trim() ?: "N/A"
                        val url = translation.image_url ?: ""

                        val category = when {
                            url.contains("/A-Z/", ignoreCase = true) -> "Alphabet"
                            url.contains("/20-99/", ignoreCase = true) ||
                                    url.contains("/100-1%20million/", ignoreCase = true) ||
                                    url.contains("/100-1 million/", ignoreCase = true) -> "Numbers"
                            url.contains("/Months/", ignoreCase = true) -> "Months"
                            url.contains("/Verbs/", ignoreCase = true) -> "Verbs"
                            url.contains("/SSL%20Sentences/", ignoreCase = true) ||
                                    url.contains("/SSL Sentences/", ignoreCase = true) -> "SSL Sentences"
                            url.contains("/Additional%20words/", ignoreCase = true) ||
                                    url.contains("/Additional words/", ignoreCase = true) -> "Additional Words"
                            else -> "Additional Words"
                        }

                        val isFav = savedFavSet.contains(english.lowercase())

                        SignItem(
                            translationId = translation.id,
                            dbImageUrl = translation.image_url,
                            englishText = english,
                            sinhalaText = sinhala,
                            category = category,
                            isFavorite = isFav
                        )
                    }
                } else {
                    errorMessage = "Server Error (${response.code()}): Unable to fetch sign data."
                }
            } catch (e: Exception) {
                errorMessage = "Network Error: Please check your connection and retry."
            } finally {
                isLoading = false
            }
        }
    }

    fun handleFavoriteToggle(sign: SignItem) {
        val isFav = favoriteManager.toggleFavorite(sign.englishText)

        signList = signList.map { item ->
            if (item.englishText.trim().equals(sign.englishText.trim(), ignoreCase = true)) {
                item.copy(isFavorite = isFav)
            } else {
                item
            }
        }

        if (selectedSign?.englishText?.trim().equals(sign.englishText.trim(), ignoreCase = true)) {
            selectedSign = selectedSign?.copy(isFavorite = isFav)
        }

        val message = if (isFav) "Added to Favorites ⭐" else "Removed from Favorites"
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(Unit) {
        fetchTranslations()
    }

    val filteredList = when (selectedCategory) {
        "All" -> signList
        "Favorites ⭐" -> signList.filter { it.isFavorite }
        else -> signList.filter { it.category == selectedCategory }
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
                IconButton(onClick = onBackClick, modifier = Modifier.size(36.dp)) {
                    Text(text = "←", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = "Learn Sign Language", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
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

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else if (errorMessage != null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = errorMessage!!, color = Color.White, fontSize = 16.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { fetchTranslations() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2682CC))
                    ) {
                        Text(text = "Retry", color = Color.White)
                    }
                }
            } else if (filteredList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (selectedCategory == "Favorites ⭐") "No favorites added yet ⭐" else "No items found",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize().padding(bottom = 20.dp)
                ) {
                    itemsIndexed(filteredList) { _, sign ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(175.dp)
                                .clickable {
                                    selectedSign = sign
                                    selectedVariantIndex = 0
                                },
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                                IconButton(
                                    onClick = { handleFavoriteToggle(sign) },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = if (sign.isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                                        contentDescription = "Favorite",
                                        tint = if (sign.isFavorite) Color(0xFFFFC107) else Color.Gray
                                    )
                                }

                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f).padding(top = 10.dp),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = sign.sinhalaText,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = cardTextColor,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = sign.englishText,
                                            fontSize = 13.sp,
                                            color = if (isDark) Color.LightGray else Color.Gray,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "👉 Tap to View",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF2682CC)
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        Button(
                                            onClick = { onPlayAudio(sign.sinhalaText, true) },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2682CC))
                                        ) {
                                            Text(text = "🔊 SI", fontSize = 10.sp, color = Color.White)
                                        }

                                        OutlinedButton(
                                            onClick = { onPlayAudio(sign.englishText, false) },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text(text = "🔊 EN", fontSize = 10.sp, color = cardTextColor)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (selectedSign != null) {
            val currentSign = selectedSign!!

            val rawMediaUrls = currentSign.dbImageUrl
                ?.split(",")
                ?.map { url ->
                    url.trim()
                        .replace("localhost", "10.0.2.2")
                        .replace(" ", "%20")
                        .replace("(", "%28")
                        .replace(")", "%29")
                }
                ?.filter { it.isNotEmpty() } ?: emptyList()

            AlertDialog(
                onDismissRequest = { selectedSign = null },
                confirmButton = {
                    Button(
                        onClick = { selectedSign = null },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF042F62)),
                        modifier = Modifier.fillMaxWidth().height(45.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Close", color = Color.White, fontSize = 16.sp)
                    }
                },
                title = {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        IconButton(
                            onClick = { handleFavoriteToggle(currentSign) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (currentSign.isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                                contentDescription = "Favorite",
                                tint = if (currentSign.isFavorite) Color(0xFFFFC107) else Color.Gray
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp)
                        ) {
                            Text(
                                text = currentSign.sinhalaText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                color = if (isDark) Color.White else Color(0xFF042F62),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = currentSign.englishText,
                                fontSize = 16.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (rawMediaUrls.size > 1) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                rawMediaUrls.forEachIndexed { index, _ ->
                                    FilterChip(
                                        selected = selectedVariantIndex == index,
                                        onClick = { selectedVariantIndex = index },
                                        label = { Text("Design ${index + 1}") },
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                }
                            }
                        }

                        val activeUrl = rawMediaUrls.getOrNull(selectedVariantIndex)

                        if (!activeUrl.isNullOrEmpty()) {
                            val isVideo = activeUrl.endsWith(".mp4", ignoreCase = true) ||
                                    activeUrl.contains("/video/", ignoreCase = true) ||
                                    activeUrl.contains("video", ignoreCase = true)

                            if (isVideo) {
                                DynamicVideoPlayer(
                                    videoUrl = activeUrl,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(220.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.Black)
                                )
                            } else {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(activeUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Sign Media",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(220.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.LightGray.copy(alpha = 0.2f)),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        } else {
                            // Local Drawable Fallback System
                            val context = LocalContext.current
                            val resourceName = currentSign.englishText.lowercase().replace(" ", "_").trim()
                            val localResId = remember(resourceName) {
                                context.resources.getIdentifier(resourceName, "drawable", context.packageName)
                            }

                            if (localResId != 0) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(localResId)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Sign Media Local",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(220.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.LightGray.copy(alpha = 0.2f)),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .background(Color.LightGray.copy(alpha = 0.2f), shape = RoundedCornerShape(16.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "📷 Sign Media Pending",
                                            color = Color.Gray,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "Media update scheduled in DB",
                                            color = Color.Gray.copy(alpha = 0.7f),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                shape = RoundedCornerShape(24.dp),
                containerColor = if (isDark) Color(0xFF1E293B) else Color.White
            )
        }
    }
}