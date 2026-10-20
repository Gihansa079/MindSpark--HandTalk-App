package com.example.mindspark2.camera

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.mindspark2.ApiService
import com.example.mindspark2.history.HistoryActivity
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.LinkedList
import java.util.Locale
import java.io.File
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.graphics.Matrix
import com.example.mindspark2.HistoryRequest
import com.example.mindspark2.RecognitionSaveRequest
import com.example.mindspark2.legal.LegalPagesActivity
import com.example.mindspark2.DynamicGestureClassifier


enum class CameraMode {
    PHOTO, VIDEO
}

data class HandLandmark(val x: Float, val y: Float, val z: Float )


// Helper Functions & Quality Analyzers

fun triggerHapticFeedback(context: Context) {
    val vibrator = ContextCompat.getSystemService(context, Vibrator::class.java)
    vibrator?.let {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            it.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            it.vibrate(100)
        }
    }
}

fun sendFeedback(context: Context) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:support@mindspark.com")
        putExtra(Intent.EXTRA_SUBJECT, "HandTalk App Feedback")
    }
    try {
        context.startActivity(Intent.createChooser(intent, "Send Feedback Via"))
    } catch (_: Exception) {
        Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
    }
}

fun openWebUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "Unable to open link", Toast.LENGTH_SHORT).show()
    }
}

fun openAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    )
    context.startActivity(intent)
}

fun openGallery(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        type = "image/*"
    }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "No gallery app found", Toast.LENGTH_SHORT).show()
    }
}

fun calculateLuminance(imageProxy: ImageProxy): Double {
    val buffer = imageProxy.planes[0].buffer
    val data = ByteArray(buffer.remaining())
    buffer.get(data)
    var sum = 0L
    for (i in data.indices step 16) {
        sum += data[i].toInt() and 0xFF
    }
    return sum.toDouble() / (data.size / 16)
}

fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
    return try {
        val bitmap = imageProxy.toBitmap()

        val rotationDegrees =
            imageProxy.imageInfo.rotationDegrees

        if (rotationDegrees == 0) {
            bitmap
        } else {
            val matrix = Matrix().apply {
                postRotate(rotationDegrees.toFloat())
            }

            Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                bitmap.height,
                matrix,
                true
            )
        }

    } catch (e: Exception) {
        Log.e(
            "CameraScreen",
            "ImageProxy to Bitmap conversion failed",
            e
        )
        null
    }
}


// Static Gesture Capture

fun captureStaticGesture(
    context: Context,
    imageCapture: ImageCapture?,
    qualityScore: Float,
    handLandmarks: List<HandLandmark>,
    onSuccess: (Uri) -> Unit
) {
    if (handLandmarks.size < 21) {
        Toast.makeText(context, "Error 903: No hand detected in frame", Toast.LENGTH_SHORT).show()
        return
    }

    if (qualityScore < 0.6f) {
        Toast.makeText(context, "Error 902: Quality insufficient", Toast.LENGTH_SHORT).show()
        return
    }

    if (handLandmarks.isEmpty()) {
        Toast.makeText(context, "Hand not detected! Please place a hand in frame.", Toast.LENGTH_SHORT).show()
        return
    }
    val capture = imageCapture ?: run {
        Toast.makeText(context, "Error 901: Capture failed", Toast.LENGTH_SHORT).show()
        return
    }

    val name = "HandTalk_VGG19_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())}.jpg"
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/HandTalk")
        }
    }

    val outputOptions = ImageCapture.OutputFileOptions.Builder(
        context.contentResolver,
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    ).build()

    capture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                outputFileResults.savedUri?.let { uri ->
                    Toast.makeText(context, "Gesture captured", Toast.LENGTH_SHORT).show()
                    onSuccess(uri)
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("CameraX", "Photo capture failed: ${exception.message}", exception)
                Toast.makeText(context, "Error 901: Capture failed", Toast.LENGTH_SHORT).show()
            }
        }
    )
}

// ==========================================
// Dynamic Gesture Recording
// ==========================================
fun recordDynamicGesture(
    context: Context,
    videoCapture: VideoCapture<Recorder>?,
    activeRecording: Recording?,
    frameBuffer: LinkedList<List<HandLandmark>>,
    onRecordingStarted: (Recording) -> Unit,
    onRecordingFinished: (Uri) -> Unit
) {
    if (activeRecording != null) {
        activeRecording.stop()
        return
    }


    val capture = videoCapture ?: run {
        Toast.makeText(context, "Error 1001: Recording failed", Toast.LENGTH_SHORT).show()
        return
    }

    // Keep video in app cache only. Only prediction text is saved to History.
    val temporaryVideo = File.createTempFile("handtalk_dynamic_", ".mp4", context.cacheDir)
    val fileOutput = FileOutputOptions.Builder(temporaryVideo).build()

    val recording = capture.output
        .prepareRecording(context, fileOutput)
        .start(ContextCompat.getMainExecutor(context)) { event ->
            when (event) {
                is VideoRecordEvent.Finalize -> {
                    if (!event.hasError()) {
                        val savedUri = Uri.fromFile(temporaryVideo)

                        Log.d("DynamicRecording", "Video saved: $savedUri")

                        // Temporary recording is ready for inference; not added to Gallery.

                        onRecordingFinished(savedUri)

                    } else {
                        Log.e(
                            "DynamicRecording",
                            "Recording failed. Code: ${event.error}",
                            event.cause
                        )
                        temporaryVideo.delete()

                        Toast.makeText(
                            context,
                            "Recording failed: ${event.error}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

    onRecordingStarted(recording)
}
// ==========================================
// Main Composable
// ==========================================

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    navController: NavController,
    onGestureRecognized: (String) -> Unit,
    userId: Int,
    apiService: ApiService = ApiService.create()
)
{
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    // Store only successfully translated recognition results.
    suspend fun saveRecognitionHistory(
        sinhalaText: String,
        englishText: String,
        inputType: String,
        recognitionType: String = "static"
    ) {
        if (userId <= 0) {
            Log.w("History", "Valid user ID unavailable")
            return
        }
        try {
            val response = apiService.saveHistory(
                HistoryRequest(
                    userId = userId,
                    englishText = englishText,
                    sinhalaText = sinhalaText,
                    // Both current photo/video paths use the static GestureClassifier.
                    recognitionType = recognitionType,
                    inputType = inputType
                )
            )
            if (response.isSuccessful) {
                Log.d("History", "History saved for user $userId")
            } else {
                Log.e("History", "Save failed HTTP ${response.code()}: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e("History", "Save failed", e)
        }
    }

    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)

    var currentMode by remember { mutableStateOf(CameraMode.PHOTO) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }
    var lensFacing by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    var isFlashOn by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    var isTranslationActive by remember { mutableStateOf(true) }

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var videoCapture: VideoCapture<Recorder>? by remember { mutableStateOf(null) }
    var activeRecording: Recording? by remember { mutableStateOf(null) }

    var guidanceMessage by remember { mutableStateOf("Please place your hand inside the box") }
    var qualityScore by remember { mutableStateOf(1.0f) }

    var currentLandmarks by remember { mutableStateOf<List<HandLandmark>>(emptyList()) }
    val frameSlidingWindow = remember { LinkedList<List<HandLandmark>>() }

    val staticPredictionWindow = remember {
        LinkedList<GesturePrediction>()
    }

    val gestureClassifier = remember { GestureClassifier(context) }

    val dynamicGestureClassifier = remember {
        DynamicGestureClassifier(context)
    }
    val handLandmarkerHelper = remember {
        HandLandmarkerHelper(
            context = context,
            onResult = { landmarks ->
                currentLandmarks = landmarks
            },
            onError = { errorMessage ->
                Log.e(
                    "CameraScreen",
                    "MediaPipe Error: $errorMessage"
                )
            }
        )
    }

    var isCaptured by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var sourceText by remember { mutableStateOf("") }
    var translatedText by remember { mutableStateOf("") }
    var recognizedInputType by remember { mutableStateOf("camera") }

    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }
    var isTtsReady by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingSeconds = 0
            while (isRecording) {
                delay(1000L)
                recordingSeconds++
            }
        } else {
            recordingSeconds = 0
        }
    }

    DisposableEffect(context) {
        val tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) isTtsReady = true
        }
        ttsEngine = tts
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            handLandmarkerHelper.close()
            gestureClassifier.close()
            dynamicGestureClassifier.close()
        }
    }
// Sinhala translations for Dynamic Gesture labels
    val dynamicSinhalaLabels = mapOf(
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

    // 1. Local Gesture Recognition Output
    fun handleLocalGestureOutput(sinhalaWord: String) {
        sourceText = sinhalaWord
        translatedText = ""
        isCaptured = true
        triggerHapticFeedback(context)

        // Save each successful prediction automatically, independent of Translate.
        // Do not save unknown or empty recognition results.
        if (sinhalaWord.isBlank() || sinhalaWord == "නොදන්නා සංඥාවකි") return
        val inputTypeAtPrediction = recognizedInputType
        val isDynamic = inputTypeAtPrediction == "video"
        coroutineScope.launch {
            val englishWord = if (isDynamic) {
                dynamicSinhalaLabels.entries.firstOrNull { it.value == sinhalaWord }?.key
            } else {
                try {
                    val response = apiService.getStaticGestures()
                    if (response.isSuccessful) {
                        response.body()?.firstOrNull {
                            it.sinhalaText.trim() == sinhalaWord.trim()
                        }?.englishText
                    } else null
                } catch (e: Exception) {
                    Log.w("History", "Static English label lookup failed", e)
                    null
                }
            }
            saveRecognitionHistory(
                sinhalaText = sinhalaWord,
                // History API expects both fields; History screen shows Sinhala only.
                englishText = englishWord ?: sinhalaWord,
                inputType = inputTypeAtPrediction,
                recognitionType = if (isDynamic) "dynamic" else "static"
            )
        }
    }

    // 2. Fetch Translation via API
    fun fetchTranslationFromAPI(sinhalaText: String) {
        if (isLoading || sinhalaText.isBlank()) return

        // Dynamic gesture translation
        if (recognizedInputType == "video") {
            val englishWord = dynamicSinhalaLabels.entries
                .firstOrNull {
                    it.value.trim() == sinhalaText.trim()
                }?.key

            if (englishWord != null) {
                translatedText = englishWord

            } else {
                translatedText = "Translation not found"
            }

            return
        }

        isLoading = true

        coroutineScope.launch {
            try {
                val response = apiService.getStaticGestures()

                if (response.isSuccessful) {
                    val gesture = response.body()?.find {
                        it.sinhalaText.trim() == sinhalaText.trim()
                    }

                    if (gesture != null) {
                        translatedText = gesture.englishText

                        val saveResponse = apiService.saveRecognition(
                            RecognitionSaveRequest(
                                userId = userId,
                                gestureId = gesture.gestureId,
                                gestureType = "static",
                                confidenceScore = null
                            )
                        )

                        if (!saveResponse.isSuccessful) {
                            Log.e(
                                "CameraScreen",
                                "Recognition save failed: ${saveResponse.code()}"
                            )
                        }

                        onGestureRecognized(gesture.englishText)
                    } else {
                        translatedText = "Translation not found"
                    }
                } else {
                    translatedText = "Server error: ${response.code()}"
                }

            } catch (e: Exception) {
                translatedText = "Connection Error"
                Log.e("CameraScreen", "API Call Error", e)
            } finally {
                isLoading = false
            }
        }
    }

    if (!cameraPermissionState.status.isGranted) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black).padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Camera Permission Required", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Please allow camera access to use sign language translation.", color = Color.Gray, fontSize = 14.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = {
                    if (cameraPermissionState.status.shouldShowRationale) {
                        cameraPermissionState.launchPermissionRequest()
                    } else {
                        openAppSettings(context)
                    }
                }) {
                    Text("Grant Permission")
                }
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            val previewView = remember {
                PreviewView(context).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
            }

            LaunchedEffect(lensFacing, isFlashOn) {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val captureInstance = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                imageCapture = captureInstance

                val recorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                    .build()
                val videoCaptureInstance = VideoCapture.withOutput(recorder)
                videoCapture = videoCaptureInstance

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                    if (!isTranslationActive) {
                        guidanceMessage = "Translation Paused"
                        currentLandmarks = emptyList()
                        imageProxy.close()
                        return@setAnalyzer
                    }

                    try {
                        val luminance = calculateLuminance(imageProxy)
                        val bitmap = imageProxyToBitmap(imageProxy)

                        if (bitmap != null) {

                            // Keep existing hand detection for Static Recognition
                            handLandmarkerHelper.detect(bitmap)

                            // Dynamic frames are read from the saved video after recording.
                            // Keep live hand detection unchanged for Static Recognition.

                            bitmap.recycle()
                        }
                        if (currentLandmarks.isEmpty()) {
                            guidanceMessage = "No hand detected! Place hand inside frame"
                            qualityScore = 0.0f
                        } else if (luminance < 35) {
                            guidanceMessage = "Too dark! Increase light (Code 601)"
                            qualityScore = 0.4f
                        } else if (luminance > 240) {
                            guidanceMessage = "Too bright! Reduce direct light"
                            qualityScore = 0.5f
                        } else {
                            guidanceMessage = if (isRecording) "Recording gesture..." else "Hand Detected - Ready"
                            qualityScore = 0.95f
                        }


                        if (currentLandmarks.isNotEmpty()) {
                            if (frameSlidingWindow.size >= 30) {
                                frameSlidingWindow.removeFirst()
                            }
                            frameSlidingWindow.addLast(currentLandmarks)
                        }

// Static prediction smoothing
                        if (currentMode == CameraMode.PHOTO &&
                            currentLandmarks.size == 21) {

                            val prediction = gestureClassifier.classifyWithDetails(
                                currentLandmarks
                            )

                            if (prediction != null) {
                                if (staticPredictionWindow.size >= 5) {
                                    staticPredictionWindow.removeFirst()
                                }
                                staticPredictionWindow.addLast(prediction)
                            }
                        } else {
                            staticPredictionWindow.clear()
                        }
                    } catch (e: Exception) {
                        Log.e("CameraScreen", "Image analysis error", e)
                        currentLandmarks = emptyList()
                    } finally {
                        imageProxy.close()
                    }
                }

                try {
                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner, lensFacing, preview, captureInstance, videoCaptureInstance, imageAnalysis
                    )
                    if (camera.cameraInfo.hasFlashUnit()) {
                        camera.cameraControl.enableTorch(isFlashOn)
                    }

                } catch (e: Exception) {
                    Toast.makeText(context, "Camera Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
            // Draw MediaPipe landmarks over the FULL camera preview
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                if (
                    isTranslationActive &&
                    currentLandmarks.size == 21
                ) {
                    currentLandmarks.forEach { landmark ->

                        val px = landmark.x * size.width
                        val py = landmark.y * size.height

                        drawCircle(
                            color = Color(0xFF00FF66),
                            radius = 4.dp.toPx(),
                            center = Offset(px, py)
                        )
                    }
                }
            }

            Canvas(modifier = Modifier.align(Alignment.Center).size(260.dp, 400.dp)) {
                val strokeWidth = 3.dp.toPx()
                val cornerLength = 35.dp.toPx()

                val frameColor = when {
                    !isTranslationActive -> Color.Gray
                    currentLandmarks.isEmpty() -> Color.Red
                    isRecording -> Color.Red
                    qualityScore < 0.6f -> Color.Yellow
                    else -> Color(0xFF00FF66)
                }

                drawPath(Path().apply { moveTo(0f, cornerLength); lineTo(0f, 0f); lineTo(cornerLength, 0f) }, frameColor, style = Stroke(strokeWidth))
                drawPath(Path().apply { moveTo(size.width - cornerLength, 0f); lineTo(size.width, 0f); lineTo(size.width, cornerLength) }, frameColor, style = Stroke(strokeWidth))
                drawPath(Path().apply { moveTo(0f, size.height - cornerLength); lineTo(0f, size.height); lineTo(cornerLength, size.height) }, frameColor, style = Stroke(strokeWidth))
                drawPath(Path().apply { moveTo(size.width - cornerLength, size.height); lineTo(size.width, size.height); lineTo(size.width, size.height - cornerLength) }, frameColor, style = Stroke(strokeWidth))

            }

            // Top Navigation
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).clickable { activity?.finish() },
                        contentAlignment = Alignment.Center
                    ) { Text("←", color = Color.White, fontSize = 26.sp) }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).clickable { isFlashOn = !isFlashOn },
                        contentAlignment = Alignment.Center
                    ) { Text(if (isFlashOn) "⚡" else "⚡\u200D⃠", color = Color.White, fontSize = 20.sp) }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isTranslationActive) Color(0xFF00FF66).copy(alpha = 0.2f) else Color.Red.copy(alpha = 0.2f))
                        .clickable {
                            try {
                                isTranslationActive = !isTranslationActive
                                triggerHapticFeedback(context)
                                Toast.makeText(
                                    context,
                                    if (isTranslationActive) "Translation Activated" else "Translation Paused",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error 2901: State toggle failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isTranslationActive) Color(0xFF00FF66) else Color.Red)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isTranslationActive) "ON" else "OFF",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).clickable {
                            val intent = Intent(context, HistoryActivity::class.java).apply {
                                putExtra("USER_ID", userId)
                            }
                            context.startActivity(intent)
                        },
                        contentAlignment = Alignment.Center
                    ) { Text("🕒", color = Color.White, fontSize = 18.sp) }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).clickable { showMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⋮", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(Color.White, shape = RoundedCornerShape(16.dp)).width(220.dp)
                        ) {
                            DropdownMenuItem(text = { Text("Send Feedback", color = Color.Black) }, onClick = { showMenu = false; sendFeedback(context) })
                            DropdownMenuItem(text = { Text("Manage permissions", color = Color.Black) }, onClick = { showMenu = false; openAppSettings(context) })
                            DropdownMenuItem(
                                text = {
                                    Text("Privacy Policy", color = Color.Black)
                                },
                                onClick = {
                                    showMenu = false

                                    val intent = Intent(
                                        context,
                                        LegalPagesActivity::class.java
                                    ).apply {
                                        putExtra(
                                            LegalPagesActivity.EXTRA_PAGE,
                                            LegalPagesActivity.PAGE_PRIVACY
                                        )
                                    }

                                    context.startActivity(intent)
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text("Terms of Service", color = Color.Black)
                                },
                                onClick = {
                                    showMenu = false

                                    val intent = Intent(
                                        context,
                                        LegalPagesActivity::class.java
                                    ).apply {
                                        putExtra(
                                            LegalPagesActivity.EXTRA_PAGE,
                                            LegalPagesActivity.PAGE_TERMS
                                        )
                                    }

                                    context.startActivity(intent)
                                }
                            )
                        }
                    }
                }
            }

            // Bottom Controls
            Column(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(50.dp)).background(Color.Black.copy(alpha = 0.6f)).padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    val formattedTime = String.format("%02d:%02d", recordingSeconds / 60, recordingSeconds % 60)
                    Text(
                        text = when {
                            !isTranslationActive -> "Translation OFF (Paused)"
                            isRecording -> "🔴 Recording: $formattedTime"
                            else -> guidanceMessage
                        },
                        color = when {
                            !isTranslationActive -> Color.Gray
                            currentLandmarks.isEmpty() -> Color.Red
                            isRecording -> Color.Red
                            qualityScore < 0.6f -> Color.Yellow
                            else -> Color(0xFF00FF66)
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(56.dp).clip(CircleShape).background(Color.DarkGray).clickable { openGallery(context) },
                        contentAlignment = Alignment.Center
                    ) { Text("🖼️", fontSize = 24.sp) }

                    Spacer(modifier = Modifier.width(36.dp))

                    Box(
                        modifier = Modifier.size(78.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.3f)).padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize().clip(if (isRecording) RoundedCornerShape(8.dp) else CircleShape)
                                .background(if (currentMode == CameraMode.VIDEO) Color.Red else Color.White)
                                .clickable {
                                    if (!isTranslationActive) {
                                        Toast.makeText(context, "Please turn ON translation first", Toast.LENGTH_SHORT).show()
                                        return@clickable
                                    }

                                    if (
                                        currentLandmarks.isEmpty() &&
                                        !(currentMode == CameraMode.VIDEO && isRecording)
                                    ) {
                                        Toast.makeText(
                                            context,
                                            "No hand detected in frame!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return@clickable
                                    }
                                    if (currentMode == CameraMode.PHOTO) {
                                        // Validate before showing a prediction or saving a photo.
                                        val capturedLandmarks = currentLandmarks.toList()
                                        if (capturedLandmarks.size < 21) {
                                            Toast.makeText(context, "No hand detected in frame!", Toast.LENGTH_SHORT).show()
                                            return@clickable
                                        }
                                        if (qualityScore < 0.6f) {
                                            Toast.makeText(context, "Quality insufficient. Improve lighting.", Toast.LENGTH_SHORT).show()
                                            return@clickable
                                        }
                                        if (imageCapture == null) {
                                            Toast.makeText(context, "Camera not ready yet", Toast.LENGTH_SHORT).show()
                                            return@clickable
                                        }

                                        // Start saving the photo without waiting for onImageSaved
                                        // to run inference. The saved image is still kept in Gallery.
                                        captureStaticGesture(
                                            context = context,
                                            imageCapture = imageCapture,
                                            qualityScore = qualityScore,
                                            handLandmarks = capturedLandmarks
                                        ) { savedUri ->
                                            Log.d("CameraScreen", "Photo saved: $savedUri")
                                        }

                                        // Show the recognition result immediately from the
                                        // landmarks captured at the button press.
                                        recognizedInputType = "camera"
                                        val recentPredictions = staticPredictionWindow.toList()

                                        val prediction = if (recentPredictions.size >= 5) {

                                            val grouped = recentPredictions.groupBy {
                                                it.classIndex
                                            }

                                            val majority = grouped.maxByOrNull {
                                                it.value.size
                                            }

                                            if (majority != null && majority.value.size >= 4) {
                                                majority.value.maxByOrNull {
                                                    it.confidence
                                                }
                                            } else {
                                                null
                                            }

                                        } else {
                                            null
                                        }
                                        if (prediction != null) {
                                            Log.d(
                                                "StaticModelTest",
                                                "Class Index: ${prediction.classIndex}, " +
                                                        "Sinhala Word: ${prediction.sinhalaText}, " +
                                                        "Confidence: ${prediction.confidence}"
                                            )
                                            handleLocalGestureOutput(prediction.sinhalaText)
                                        } else {
                                            Log.w("StaticModelTest", "No confident prediction")
                                            handleLocalGestureOutput("නොදන්නා සංඥාවකි")
                                        }
                                    } else {
                                        if (isRecording) {
                                            activeRecording?.stop()
                                            activeRecording = null
                                            isRecording = false
                                        } else {
                                            recordDynamicGesture(
                                                context = context,
                                                videoCapture = videoCapture,
                                                activeRecording = activeRecording,
                                                frameBuffer = frameSlidingWindow,
                                                onRecordingStarted = { recording ->
                                                    activeRecording = recording
                                                    isRecording = true
                                                },
                                                onRecordingFinished = { savedUri ->
                                                    Log.d("DynamicTest", "Recording finished: $savedUri")
                                                    // Do not display a hardcoded result. Wait for the CNN+LSTM prediction.
                                                    recognizedInputType = "video"
                                                    translatedText = ""
                                                    isCaptured = false
                                                    Toast.makeText(context, "Processing recorded gesture...", Toast.LENGTH_SHORT).show()
                                                    coroutineScope.launch(Dispatchers.Default) {
                                                        val frames = mutableListOf<Bitmap>()
                                                        val retriever = MediaMetadataRetriever()
                                                        try {
                                                            retriever.setDataSource(context, savedUri)
                                                            val durationMs = retriever.extractMetadata(
                                                                MediaMetadataRetriever.METADATA_KEY_DURATION
                                                            )?.toLongOrNull() ?: 0L
                                                            require(durationMs > 0L) { "Video has no valid duration" }

                                                            // Diagnostic test: compare normal vs horizontally mirrored video.
                                                            // Keep the original output until training orientation is confirmed.
                                                            val durationUs = durationMs * 1000L
                                                            val mirroredFrames = mutableListOf<Bitmap>()
                                                            try {
                                                                for (i in 0 until 30) {
                                                                    // Uniform chronological sampling, avoiding exact first/last frame.
                                                                    val timeUs = durationUs * (2L * i + 1L) / 60L
                                                                    val frame = retriever.getFrameAtTime(
                                                                        timeUs,
                                                                        MediaMetadataRetriever.OPTION_CLOSEST
                                                                    ) ?: throw IllegalStateException("Cannot extract frame $i")
                                                                    try {
                                                                        val scaled = Bitmap.createScaledBitmap(frame, 128, 128, true)
                                                                        try {
                                                                            val normal = scaled.copy(Bitmap.Config.ARGB_8888, false)
                                                                                ?: error("Cannot copy frame $i")
                                                                            frames.add(normal)
                                                                            val flip = Matrix().apply { preScale(-1f, 1f) }
                                                                            mirroredFrames.add(
                                                                                Bitmap.createBitmap(
                                                                                    normal, 0, 0, 128, 128, flip, true
                                                                                )
                                                                            )
                                                                        } finally {
                                                                            if (scaled !== frame) scaled.recycle()
                                                                        }
                                                                    } finally {
                                                                        frame.recycle()
                                                                    }
                                                                }
                                                                Log.d("DynamicModelTest", "Video frames extracted: ${frames.size}")
                                                                val prediction = dynamicGestureClassifier.predict(frames)
                                                                Log.d("DynamicModelTest", "NORMAL: ${prediction.label}, confidence=${prediction.confidence}")
                                                                val mirrorPrediction = dynamicGestureClassifier.predict(mirroredFrames)
                                                                Log.d("DynamicModelTest", "MIRRORED: ${mirrorPrediction.label}, confidence=${mirrorPrediction.confidence}")
                                                                // Do NOT automatically pick the highest confidence;
                                                                // the correct orientation must match training data.
                                                                withContext(Dispatchers.Main) {
                                                                    val sinhalaWord = dynamicSinhalaLabels.entries
                                                                        .firstOrNull { it.key.equals(prediction.label.trim(), ignoreCase = true) }
                                                                        ?.value ?: "නොදන්නා සංඥාවකි"
                                                                    handleLocalGestureOutput(sinhalaWord)
                                                                }
                                                            } finally {
                                                                mirroredFrames.forEach { if (!it.isRecycled) it.recycle() }
                                                            }
                                                        } catch (e: Exception) {
                                                            Log.e("DynamicModelTest", "Dynamic video inference failed", e)
                                                            withContext(Dispatchers.Main) {
                                                                Toast.makeText(
                                                                    context,
                                                                    "Dynamic recognition failed: ${e.message}",
                                                                    Toast.LENGTH_LONG
                                                                ).show()
                                                            }
                                                        } finally {
                                                            retriever.release()
                                                            frames.forEach { if (!it.isRecycled) it.recycle() }
                                                            savedUri.path?.let { File(it).delete() }
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {}
                    }

                    Spacer(modifier = Modifier.width(36.dp))

                    Box(
                        modifier = Modifier.size(56.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)).clickable {
                            lensFacing = if (lensFacing == CameraSelector.DEFAULT_BACK_CAMERA) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                        },
                        contentAlignment = Alignment.Center
                    ) { Text("🔄", color = Color.White, fontSize = 22.sp) }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier.clip(RoundedCornerShape(50.dp)).background(Color(0xFF1E1E1E)).padding(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(50.dp))
                                .background(if (currentMode == CameraMode.PHOTO) Color(0xFF2C2C2C) else Color.Transparent)
                                .clickable {
                                    currentMode = CameraMode.PHOTO
                                    if (isRecording) {
                                        activeRecording?.stop()
                                        activeRecording = null
                                        isRecording = false
                                    }
                                }
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "📷 Photo",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(if (currentMode == CameraMode.VIDEO) Color(0xFF2C2C2C) else Color.Transparent)
                                .clickable {
                                    currentMode = CameraMode.VIDEO
                                }
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "🎥 Video",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Translation Display / Result Overlay
            AnimatedVisibility(
                visible = isCaptured,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1E1E1E).copy(alpha = 0.95f))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Gesture Recognized",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.1f))
                                    .clickable { isCaptured = false },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "✕", color = Color.White, fontSize = 16.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = sourceText,
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (translatedText.isNotEmpty()) {
                            Text(
                                text = translatedText,
                                color = Color(0xFF00FF66),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        if (translatedText.isEmpty()) {
                            Button(
                                onClick = { fetchTranslationFromAPI(sourceText) },
                                enabled = !isLoading,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF66)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.Black
                                    )
                                } else {
                                    Text(
                                        text = "🌐 Translate to English",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    if (isTtsReady) {
                                        ttsEngine?.language = Locale("si", "LK")
                                        ttsEngine?.speak(
                                            sourceText,
                                            TextToSpeech.QUEUE_FLUSH,
                                            null,
                                            null
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(50.dp)
                            ) {
                                Text(text = "🔊 සිංහල")
                            }

                            if (translatedText.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(12.dp))

                                Button(
                                    onClick = {
                                        if (isTtsReady) {
                                            ttsEngine?.language = Locale.US
                                            ttsEngine?.speak(
                                                translatedText,
                                                TextToSpeech.QUEUE_FLUSH,
                                                null,
                                                null
                                            )
                                        }
                                    },
                                    shape = RoundedCornerShape(50.dp)
                                ) {
                                    Text(text = "🔊 English")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
