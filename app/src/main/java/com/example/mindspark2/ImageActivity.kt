package com.example.mindspark2.image

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
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
import com.example.mindspark2.DynamicGestureClassifier
import com.example.mindspark2.ApiService
import com.example.mindspark2.PreferencesManager
import com.example.mindspark2.HistoryRequest
import com.example.mindspark2.RecognitionSaveRequest
import com.example.mindspark2.ProfileActivity
import com.example.mindspark2.camera.GestureClassifier
import com.example.mindspark2.camera.HandLandmark
import com.example.mindspark2.camera.HandLandmarkerHelper
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
            tts?.setSpeechRate(1.0f)
            Log.d("TTS", "Voice engine ready")
        } else {
            isTtsReady = false
            Log.e("TTS", "Voice initialization failed")
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

// Converts a selected gallery image into an upright Bitmap for MediaPipe.
@Suppress("DEPRECATION")
fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    } catch (e: Exception) {
        Log.e("ImageActivity", "Unable to decode selected image", e)
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
    val prefsManager = remember { PreferencesManager(context) }

    val gestureClassifier = remember { GestureClassifier(context) }
    val dynamicGestureClassifier = remember { DynamicGestureClassifier(context) }
    val dynamicSinhalaLabels = remember {
        mapOf(
            "Assent" to "එකඟයි",
            "Good Evening" to "සුබ සන්ධ්‍යාවක්",
            "Good Morning" to "සුබ උදෑසනක්",
            "Good Night" to "සුබ රාත්‍රියක්",
            "Please" to "කරුණාකර",
            "Sorry" to "සමාවෙන්න",
            "Thank you" to "ස්තූතියි",
            "Today" to "අද",
            "Yesterday" to "ඊයේ",
            "welcome" to "සාදරයෙන් පිළිගනිමු"
        )
    }

    var detectedLandmarks by remember {
        mutableStateOf<List<HandLandmark>>(emptyList())
    }

    val handLandmarkerHelper = remember {
        HandLandmarkerHelper(
            context = context,
            onResult = { landmarks ->
                detectedLandmarks = landmarks
                Log.d("ImageActivity", "MediaPipe landmarks = ${landmarks.size}")
            },
            onError = { error ->
                Log.e("ImageActivity", "MediaPipe error: $error")
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            handLandmarkerHelper.close()
            gestureClassifier.close()
            dynamicGestureClassifier.close()
        }
    }

    var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }
    var selectedMediaType by remember { mutableStateOf<MediaType?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var englishTranslation by remember { mutableStateOf<String?>(null) }
    var sinhalaTranslation by remember { mutableStateOf<String?>(null) }
    var showEnglishTranslation by remember { mutableStateOf(false) }
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
            showEnglishTranslation = false
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
                    val mediaUri = selectedMediaUri

                    if (mediaUri == null) {
                        Toast.makeText(
                            context,
                            "Please select an image or video first!",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        isProcessing = true
                        errorMessage = null
                        englishTranslation = null
                        sinhalaTranslation = null
                        showEnglishTranslation = false

                        coroutineScope.launch {
                            try {
                                if (selectedMediaType == MediaType.IMAGE) {
                                    // IMAGE FLOW:
                                    // Gallery image -> MediaPipe -> 21 landmarks ->
                                    // GestureClassifier -> Sinhala -> API -> English
                                    val bitmap = withContext(Dispatchers.IO) {
                                        uriToBitmap(context, mediaUri)
                                    }

                                    if (bitmap == null) {
                                        errorMessage = "Unable to read selected image."
                                        return@launch
                                    }

                                    detectedLandmarks = emptyList()

                                    // HandLandmarkerHelper uses RunningMode.IMAGE, so detect()
                                    // completes synchronously and calls onResult before returning.
                                    handLandmarkerHelper.detect(bitmap)

                                    val landmarksSnapshot = detectedLandmarks.toList()

                                    if (landmarksSnapshot.size != 21) {
                                        errorMessage = "No hand sign detected in this image."
                                        return@launch
                                    }

                                    val prediction = gestureClassifier.classifyWithDetails(landmarksSnapshot)
                                    if (prediction == null) {
                                        errorMessage = "Unknown sign or low-confidence prediction."
                                        return@launch
                                    }

                                    // classIndex is the model's LabelEncoder index, NOT a database gesture_id.
                                    // Resolve it using static_gestures.class_label.
                                    val gesturesResponse = apiService.getStaticGestures()
                                    if (!gesturesResponse.isSuccessful) {
                                        errorMessage = "Cannot load gesture labels (HTTP ${gesturesResponse.code()})."
                                        return@launch
                                    }
                                    val gesture = gesturesResponse.body()
                                        ?.firstOrNull { it.classLabel == prediction.classIndex }
                                    if (gesture == null) {
                                        errorMessage = "No static gesture mapped to class ${prediction.classIndex}. Check static_gestures.class_label."
                                        return@launch
                                    }

                                    val english = gesture.englishText
                                    val sinhala = gesture.sinhalaText
                                    englishTranslation = english
                                    sinhalaTranslation = sinhala

                                    val userId = prefsManager.userId
                                    if (userId <= 0) {
                                        errorMessage = "Translation shown, but please log in to save history."
                                        return@launch
                                    }

                                    // The current history.translation_id FK points to the OLD translations table.
                                    // Keep it null; do not assign translation_new.translation_id to that FK.
                                    try {
                                        val recognitionResult = apiService.saveRecognition(
                                            RecognitionSaveRequest(
                                                userId = userId,
                                                gestureId = gesture.gestureId,
                                                gestureType = "static",
                                                confidenceScore = prediction.confidence.toDouble()
                                            )
                                        )
                                        if (!recognitionResult.isSuccessful) {
                                            Log.e("ImageActivity", "Save recognition HTTP ${recognitionResult.code()}")
                                        }
                                        val historyResult = apiService.saveHistory(
                                            HistoryRequest(
                                                userId = userId,
                                                translationId = null,
                                                englishText = english,
                                                sinhalaText = sinhala,
                                                recognitionType = "static",
                                                inputType = "image",
                                                confidenceScore = prediction.confidence.toDouble()
                                            )
                                        )
                                        if (!historyResult.isSuccessful) {
                                            errorMessage = "Translation shown, but saving history failed (HTTP ${historyResult.code()})."
                                        }
                                    } catch (saveError: Exception) {
                                        Log.e("ImageActivity", "Saving recognition/history failed", saveError)
                                        errorMessage = "Translation shown, but saving to the server failed."
                                    }
                                } else {
                                    // VIDEO FLOW: gallery video -> 30 frames -> CNN+LSTM TFLite.
                                    // No backend /translate-media endpoint is needed for inference.
                                    val frames = withContext(Dispatchers.Default) {
                                        extractDynamicVideoFrames(context, mediaUri)
                                    }
                                    val prediction = try {
                                        withContext(Dispatchers.Default) {
                                            dynamicGestureClassifier.predict(frames)
                                        }
                                    } finally {
                                        frames.forEach { if (!it.isRecycled) it.recycle() }
                                    }
                                    Log.d("ImageActivity", "Dynamic prediction: ${prediction.label} (${prediction.confidence})")
                                    val english = prediction.label.trim()
                                    val sinhala = dynamicSinhalaLabels.entries
                                        .firstOrNull { it.key.equals(english, ignoreCase = true) }?.value
                                    if (sinhala == null) {
                                        errorMessage = "Unknown dynamic label: $english"
                                        return@launch
                                    }
                                    englishTranslation = english
                                    sinhalaTranslation = sinhala

                                    // Save once when recognition succeeds. Translate button only changes UI.
                                    val userId = prefsManager.userId
                                    if (userId > 0) {
                                        try {
                                            val saved = apiService.saveHistory(
                                                HistoryRequest(
                                                    userId = userId,
                                                    translationId = null,
                                                    englishText = english,
                                                    sinhalaText = sinhala,
                                                    recognitionType = "dynamic",
                                                    inputType = "video",
                                                    confidenceScore = prediction.confidence.toDouble()
                                                )
                                            )
                                            if (!saved.isSuccessful) {
                                                errorMessage = "Result shown, but history save failed (HTTP ${saved.code()})."
                                            }
                                        } catch (saveError: Exception) {
                                            Log.e("ImageActivity", "Dynamic history save failed", saveError)
                                            errorMessage = "Result shown, but history could not be saved."
                                        }
                                    } else {
                                        errorMessage = "Result shown. Please log in to save history."
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("ImageActivity", "Media recognition failed", e)
                                errorMessage = "Processing Error: ${e.message}"
                            } finally {
                                isProcessing = false
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
                        text = if (selectedMediaType == MediaType.VIDEO) "Processing Video..." else "Identifying Image...",
                        fontSize = 16.sp,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = if (selectedMediaType == MediaType.VIDEO) "🔍 Recognize Video" else "🔍 Recognize Image",
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
                                text = "Recognition Result",
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
                            text = "🤟 $sinhalaTranslation",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF042F62)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(onClick = { showEnglishTranslation = !showEnglishTranslation }) {
                            Text(if (showEnglishTranslation) "Hide English" else "🌐 Translate to English")
                        }

                        if (showEnglishTranslation) {
                            Text(
                                text = englishTranslation.orEmpty().ifBlank { "English translation unavailable" },
                                color = Color.DarkGray,
                                fontSize = 18.sp
                            )
                        }

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

                            if (showEnglishTranslation && !englishTranslation.isNullOrBlank()) {
                                OutlinedButton(
                                    onClick = { onPlayAudio(englishTranslation!!, "en") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("🔊 English")
                                }
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
                tag = videoUri
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
            if (videoView.tag != videoUri) {
                videoView.tag = videoUri
                videoView.setVideoURI(videoUri)
                videoView.start()
            }
        },
        onRelease = { videoView ->
            videoView.stopPlayback()
        }
    )
}

/** Sample exactly 30 frames, matching the CameraScreen dynamic inference preprocessing. */
private fun extractDynamicVideoFrames(context: Context, videoUri: Uri): List<Bitmap> {
    val retriever = MediaMetadataRetriever()
    val frames = mutableListOf<Bitmap>()
    try {
        retriever.setDataSource(context, videoUri)
        val durationMs = retriever.extractMetadata(
            MediaMetadataRetriever.METADATA_KEY_DURATION
        )?.toLongOrNull() ?: 0L
        require(durationMs > 0L) { "Video has no valid duration" }
        val durationUs = durationMs * 1000L
        for (i in 0 until 30) {
            val timeUs = durationUs * (2L * i + 1L) / 60L
            val frame = retriever.getFrameAtTime(
                timeUs, MediaMetadataRetriever.OPTION_CLOSEST
            ) ?: throw IllegalStateException("Cannot extract frame ${i + 1}/30")
            try {
                val resized = Bitmap.createScaledBitmap(frame, 128, 128, true)
                try {
                    frames.add(
                        resized.copy(Bitmap.Config.ARGB_8888, false)
                            ?: throw IllegalStateException("Cannot convert frame ${i + 1}")
                    )
                } finally {
                    if (resized !== frame) resized.recycle()
                }
            } finally {
                if (!frame.isRecycled) frame.recycle()
            }
        }
        return frames
    } catch (e: Exception) {
        frames.forEach { if (!it.isRecycled) it.recycle() }
        throw e
    } finally {
        retriever.release()
    }
}
