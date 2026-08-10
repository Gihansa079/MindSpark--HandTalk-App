package com.example.mindspark2.image

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import coil.compose.AsyncImage
import com.example.mindspark2.ApiService
import com.example.mindspark2.ProfileActivity
import com.example.mindspark2.ui.theme.Mindspark2Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class ImageActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private var selectedVoiceGender = mutableStateOf("Female")

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

        intent.getStringExtra("SELECTED_VOICE")?.let {
            selectedVoiceGender.value = it
        }

        tts = TextToSpeech(this, this)

        setContent {
            Mindspark2Theme(darkTheme = false) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MediaTranslationScreen(
                        currentVoiceGender = selectedVoiceGender.value,
                        onBackClick = { finish() },
                        onOpenProfile = { openProfileActivity() },
                        onPlayAudio = { text, languageCode ->
                            val isFemale = selectedVoiceGender.value.equals("Female", ignoreCase = true)
                            speakText(text, languageCode, isFemale)
                        }
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

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

enum class MediaType {
    IMAGE, VIDEO
}

// Helper to convert URI to temporary File for Multipart API request
fun uriToFile(context: Context, uri: Uri): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val tempFile = File.createTempFile("upload_media", ".tmp", context.cacheDir)
        val outputStream = FileOutputStream(tempFile)
        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        tempFile
    } catch (e: Exception) {
        Log.e("ImageActivity", "Error converting URI to File", e)
        null
    }
}

@Composable
fun MediaTranslationScreen(
    currentVoiceGender: String,
    onBackClick: () -> Unit,
    onOpenProfile: () -> Unit,
    onPlayAudio: (text: String, languageCode: String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val apiService = remember { ApiService.create() }

    var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }
    var selectedMediaType by remember { mutableStateOf<MediaType?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var englishTranslation by remember { mutableStateOf<String?>(null) }
    var sinhalaTranslation by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            selectedMediaUri = it
            val mimeType = context.contentResolver.getType(it)
            selectedMediaType = if (mimeType?.startsWith("video") == true) {
                MediaType.VIDEO
            } else {
                MediaType.IMAGE
            }

            englishTranslation = null
            sinhalaTranslation = null
            errorMessage = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF042F62),
                        Color(0xFF2682CC),
                        Color(0xFFEAF6FF)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
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
                    text = "Media Translation",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clickable {
                        if (!isProcessing) {
                            mediaPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        }
                    },
                shape = RoundedCornerShape(25.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.9f)
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedMediaUri != null) {
                        if (selectedMediaType == MediaType.IMAGE) {
                            AsyncImage(
                                model = selectedMediaUri,
                                contentDescription = "Selected Image",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(25.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            MediaVideoPlayer(
                                videoUri = selectedMediaUri!!,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(25.dp))
                            )
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Text(
                                text = "🖼️ 🎥",
                                fontSize = 48.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Tap to select an image or video",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF042F62),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Supports JPG, PNG, MP4, MOV",
                                fontSize = 13.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            errorMessage?.let { msg ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 15.dp),
                    color = Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = msg,
                        color = Color(0xFFD32F2F),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Button(
                onClick = {
                    if (selectedMediaUri == null) {
                        Toast.makeText(context, "Please select an image or video first!", Toast.LENGTH_SHORT).show()
                    } else {
                        isProcessing = true
                        errorMessage = null

                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val file = uriToFile(context, selectedMediaUri!!)
                                if (file != null) {
                                    val mimeType = context.contentResolver.getType(selectedMediaUri!!) ?: "media/*"
                                    val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
                                    val body = MultipartBody.Part.createFormData("media", file.name, requestFile)

                                    val response = apiService.translateMedia(body)

                                    withContext(Dispatchers.Main) {
                                        if (response.isSuccessful && response.body() != null) {
                                            englishTranslation = response.body()?.english_text ?: "Thank You"
                                            sinhalaTranslation = response.body()?.sinhala_text ?: "ස්තූතියි"
                                        } else {
                                            errorMessage = "Processing failed. Please try again."
                                        }
                                    }
                                } else {
                                    withContext(Dispatchers.Main) {
                                        errorMessage = "Unable to read selected media file."
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    errorMessage = "Connection Error: ${e.message}"
                                }
                            } finally {
                                withContext(Dispatchers.Main) {
                                    isProcessing = false
                                }
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF042F62)
                ),
                enabled = !isProcessing
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (selectedMediaType == MediaType.VIDEO) "Processing Video..." else "Preprocessing Image...",
                        fontSize = 16.sp,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = if (selectedMediaType == MediaType.VIDEO) "🔍 Translate Video" else "🔍 Translate Image",
                        fontSize = 18.sp,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(25.dp))

            if (englishTranslation != null && sinhalaTranslation != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(25.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.LightGray.copy(alpha = 0.95f)
                    )
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
                                text = "Translation Result",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color(0xFF042F62)
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
                            text = "🤟 $englishTranslation",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "සිංහල : $sinhalaTranslation",
                            color = Color.DarkGray,
                            fontSize = 16.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onPlayAudio(sinhalaTranslation!!, "si") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("🔊 Sinhala")
                            }

                            OutlinedButton(
                                onClick = { onPlayAudio(englishTranslation!!, "en") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("🔊 English")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                mediaPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2682CC)
                            )
                        ) {
                            Text("New Media")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun MediaVideoPlayer(
    videoUri: Uri,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            VideoView(context).apply {
                setVideoURI(videoUri)
                val mediaController = MediaController(context)
                mediaController.setAnchorView(this)
                setMediaController(mediaController)
                setOnPreparedListener { mp ->
                    mp.isLooping = true
                    start()
                }
            }
        },
        update = { videoView ->
            videoView.setVideoURI(videoUri)
            videoView.start()
        },
        onRelease = { videoView ->
            videoView.stopPlayback()
        }
    )
}