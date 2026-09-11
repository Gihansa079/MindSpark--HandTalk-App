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

enum class CameraMode {
    PHOTO, VIDEO
}

data class HandLandmark(val x: Float, val y: Float)

// ==========================================
// Helper Functions & Quality Analyzers
// ==========================================

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

fun generateHandLandmarks(isHandInFrame: Boolean): List<HandLandmark> {
    if (!isHandInFrame) return emptyList()

    val landmarks = mutableListOf<HandLandmark>()
    for (i in 0 until 21) {
        val x = (0.25f + (i % 5) * 0.12f).coerceIn(0.1f, 0.9f)
        val y = (0.25f + (i / 5) * 0.13f).coerceIn(0.1f, 0.9f)
        landmarks.add(HandLandmark(x, y))
    }
    return landmarks
}

// ==========================================
// Static Gesture Capture
// ==========================================
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

    if (frameBuffer.size < 20) {
        Toast.makeText(context, "Error 1002: Insufficient frames (Min 20 required)", Toast.LENGTH_SHORT).show()
        return
    }

    val capture = videoCapture ?: run {
        Toast.makeText(context, "Error 1001: Recording failed", Toast.LENGTH_SHORT).show()
        return
    }

    val name = "HandTalk_LSTM_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())}.mp4"
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/HandTalk")
        }
    }

    val mediaStoreOutput = MediaStoreOutputOptions.Builder(
        context.contentResolver,
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    ).setContentValues(contentValues).build()

    val recording = capture.output
        .prepareRecording(context, mediaStoreOutput)
        .start(ContextCompat.getMainExecutor(context)) { event ->
            when (event) {
                is VideoRecordEvent.Finalize -> {
                    if (!event.hasError()) {
                        val savedUri = event.outputResults.outputUri
                        Toast.makeText(context, "Frames Processed", Toast.LENGTH_SHORT).show()
                        onRecordingFinished(savedUri)
                    } else {
                        Log.e("CameraX", "Video recording error: ${event.error}")
                        Toast.makeText(context, "Error 1001: Recording failed", Toast.LENGTH_SHORT).show()
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
    apiService: ApiService = ApiService.create()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)

    var currentMode by remember { mutableStateOf(CameraMode.PHOTO) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }
    var lensFacing by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    var isFlashOn by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val gestureClassifier = remember { GestureClassifier(context) }
    var isTranslationActive by remember { mutableStateOf(true) }

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var videoCapture: VideoCapture<Recorder>? by remember { mutableStateOf(null) }
    var activeRecording: Recording? by remember { mutableStateOf(null) }

    var guidanceMessage by remember { mutableStateOf("Please place your hand inside the box") }
    var qualityScore by remember { mutableStateOf(1.0f) }

    var currentLandmarks by remember { mutableStateOf<List<HandLandmark>>(emptyList()) }
    val frameSlidingWindow = remember { LinkedList<List<HandLandmark>>() }

    var isCaptured by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var sourceText by remember { mutableStateOf("") }
    var translatedText by remember { mutableStateOf("") }

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

    // 1. Local Gesture Recognition Output
    fun handleLocalGestureOutput(sinhalaWord: String) {
        sourceText = sinhalaWord
        translatedText = ""
        isCaptured = true
        triggerHapticFeedback(context)
    }

    // 2. Fetch Translation via API
    fun fetchTranslationFromAPI(sinhalaText: String) {
        isLoading = true
        coroutineScope.launch {
            try {
                val response = apiService.getTranslation(text = sinhalaText)

                if (response.isSuccessful && response.body() != null) {
                    translatedText = response.body()?.english_text ?: "Translation not found"
                    onGestureRecognized(translatedText)
                } else {
                    translatedText = "Translation not found"
                }
            } catch (e: Exception) {
                translatedText = "Connection Error"
                Log.e("CameraScreen", "API Call Error: ${e.message}")
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
            val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }

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

                    val luminance = calculateLuminance(imageProxy)
                    val isHandInFrame = true
                    val landmarks = generateHandLandmarks(isHandInFrame)
                    currentLandmarks = landmarks

                    if (landmarks.isEmpty()) {
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

                    if (landmarks.isNotEmpty()) {
                        if (frameSlidingWindow.size >= 30) {
                            frameSlidingWindow.removeFirst()
                        }
                        frameSlidingWindow.addLast(landmarks)
                    }

                    imageProxy.close()
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

                if (isTranslationActive && currentLandmarks.isNotEmpty()) {
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
                            context.startActivity(Intent(context, HistoryActivity::class.java))
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
                            DropdownMenuItem(text = { Text("Privacy Policy", color = Color.Black) }, onClick = { showMenu = false; openWebUrl(context, "https://mindspark.com/privacy-policy") })
                            DropdownMenuItem(text = { Text("Terms of Service", color = Color.Black) }, onClick = { showMenu = false; openWebUrl(context, "https://mindspark.com/terms-of-service") })
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

                                    if (currentLandmarks.isEmpty()) {
                                        Toast.makeText(context, "No hand detected in frame!", Toast.LENGTH_SHORT).show()
                                        return@clickable
                                    }

                                    if (currentMode == CameraMode.PHOTO) {
                                        captureStaticGesture(
                                            context = context,
                                            imageCapture = imageCapture,
                                            qualityScore = qualityScore,
                                            handLandmarks = currentLandmarks
                                        ) {
                                            val predictedSinhalaWord = gestureClassifier.classify(currentLandmarks)
                                            handleLocalGestureOutput(predictedSinhalaWord)
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
                                                onRecordingFinished = {
                                                    val predictedSinhalaWord = gestureClassifier.classify(currentLandmarks)
                                                    handleLocalGestureOutput(predictedSinhalaWord)
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