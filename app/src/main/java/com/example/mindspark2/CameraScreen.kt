package com.example.mindspark2.camera

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
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
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.mindspark2.history.HistoryActivity
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.jvm.java

enum class CameraMode {
    PHOTO, VIDEO
}

// ==========================================
// Top-Level Helper Functions
// ==========================================

fun sendFeedback(context: Context) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:support@mindspark.com")
        putExtra(Intent.EXTRA_SUBJECT, "HandTalk App Feedback")
    }
    try {
        context.startActivity(Intent.createChooser(intent, "Send Feedback Via"))
    } catch (e: Exception) {
        Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
    }
}

fun openWebUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
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
    } catch (e: Exception) {
        Toast.makeText(context, "No gallery app found", Toast.LENGTH_SHORT).show()
    }
}

// Save Photo to Gallery
fun takePhotoAndSaveToGallery(
    context: Context,
    imageCapture: ImageCapture?,
    onSuccess: (Uri) -> Unit
) {
    val capture = imageCapture ?: run {
        Toast.makeText(context, "Camera not ready", Toast.LENGTH_SHORT).show()
        return
    }

    val name = "HandTalk_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())}.jpg"
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
                    Toast.makeText(context, "Photo saved to Gallery!", Toast.LENGTH_SHORT).show()
                    onSuccess(uri)
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("CameraX", "Photo capture failed: ${exception.message}", exception)
                Toast.makeText(context, "Failed to save photo", Toast.LENGTH_SHORT).show()
            }
        }
    )
}

// Save Video to Gallery
fun startVideoRecording(
    context: Context,
    videoCapture: VideoCapture<Recorder>?,
    activeRecording: Recording?,
    onRecordingStarted: (Recording) -> Unit,
    onRecordingFinished: (Uri) -> Unit
) {
    if (activeRecording != null) {
        activeRecording.stop()
        return
    }

    val capture = videoCapture ?: run {
        Toast.makeText(context, "Video recorder not ready", Toast.LENGTH_SHORT).show()
        return
    }

    val name = "HandTalk_Video_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())}.mp4"
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
                        Toast.makeText(context, "Video saved to Gallery!", Toast.LENGTH_SHORT).show()
                        onRecordingFinished(savedUri)
                    } else {
                        Log.e("CameraX", "Video recording error: ${event.error}")
                        Toast.makeText(context, "Failed to save video", Toast.LENGTH_SHORT).show()
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
fun CameraScreen(navController: NavController, onGestureRecognized: Function<Unit>) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)

    var currentMode by remember { mutableStateOf(CameraMode.PHOTO) }
    var isRecording by remember { mutableStateOf(false) }
    var lensFacing by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    var isFlashOn by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var videoCapture: VideoCapture<Recorder>? by remember { mutableStateOf(null) }
    var activeRecording: Recording? by remember { mutableStateOf(null) }

    var isCaptured by remember { mutableStateOf(false) }
    var capturedType by remember { mutableStateOf(CameraMode.PHOTO) }
    var sourceLanguage by remember { mutableStateOf("Sinhala") }
    var targetLanguage by remember { mutableStateOf("English") }

    var sourceText by remember { mutableStateOf("ආයුබෝවන්") }
    var translatedText by remember { mutableStateOf("Hello") }

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }
    var isTtsReady by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        var tts: TextToSpeech? = null
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
            }
        }
        ttsEngine = tts

        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    fun speakText(text: String, languageName: String) {
        val tts = ttsEngine
        if (tts != null && isTtsReady) {
            val locale = if (languageName.lowercase() == "sinhala") {
                Locale("si", "LK")
            } else {
                Locale.US
            }

            val result = tts.setLanguage(locale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.language = Locale.US
            }

            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TranslationSpeechId")
        }
    }

    if (!cameraPermissionState.status.isGranted) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Camera Permission Required",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Please allow camera access to use sign language translation.",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                    Text(text = "Grant Permission")
                }
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // 1. Camera Viewfinder Area
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
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

                    try {
                        cameraProvider.unbindAll()
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            lensFacing,
                            preview,
                            captureInstance,
                            videoCaptureInstance
                        )

                        if (camera.cameraInfo.hasFlashUnit()) {
                            camera.cameraControl.enableTorch(isFlashOn)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )

                Canvas(modifier = Modifier.size(260.dp, 400.dp)) {
                    val strokeWidth = 3.dp.toPx()
                    val cornerLength = 35.dp.toPx()
                    val color = if (isRecording) Color.Red else Color.White.copy(alpha = 0.85f)

                    drawPath(
                        path = Path().apply {
                            moveTo(0f, cornerLength)
                            lineTo(0f, 0f)
                            lineTo(cornerLength, 0f)
                        },
                        color = color,
                        style = Stroke(width = strokeWidth)
                    )
                    drawPath(
                        path = Path().apply {
                            moveTo(size.width - cornerLength, 0f)
                            lineTo(size.width, 0f)
                            lineTo(size.width, cornerLength)
                        },
                        color = color,
                        style = Stroke(width = strokeWidth)
                    )
                    drawPath(
                        path = Path().apply {
                            moveTo(0f, size.height - cornerLength)
                            lineTo(0f, size.height)
                            lineTo(cornerLength, size.height)
                        },
                        color = color,
                        style = Stroke(width = strokeWidth)
                    )
                    drawPath(
                        path = Path().apply {
                            moveTo(size.width - cornerLength, size.height)
                            lineTo(size.width, size.height)
                            lineTo(size.width, size.height - cornerLength)
                        },
                        color = color,
                        style = Stroke(width = strokeWidth)
                    )
                }
            }

            // 2. Top Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable { activity?.finish() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "←", color = Color.White, fontSize = 26.sp)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable { isFlashOn = !isFlashOn },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isFlashOn) "⚡" else "⚡\u200D⃠",
                            color = Color.White,
                            fontSize = 20.sp
                        )
                    }
                }

                Text(
                    text = "Camera",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Camera Screen එකේ Top Bar එකේ History Button එක
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable {
                                // NavController වෙනුවට direct Activity Intent එකක් භාවිතා කරන්න
                                val intent = Intent(context, HistoryActivity::class.java)
                                context.startActivity(intent)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🕒", color = Color.White, fontSize = 18.sp)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable { showMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "⋮", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier
                                .background(Color.White, shape = RoundedCornerShape(16.dp))
                                .width(220.dp)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Send Feedback", color = Color.Black) },
                                onClick = {
                                    showMenu = false
                                    sendFeedback(context)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Manage permissions", color = Color.Black) },
                                onClick = {
                                    showMenu = false
                                    openAppSettings(context)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Privacy Policy", color = Color.Black) },
                                onClick = {
                                    showMenu = false
                                    openWebUrl(context, "https://mindspark.com/privacy-policy")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Terms of Service", color = Color.Black) },
                                onClick = {
                                    showMenu = false
                                    openWebUrl(context, "https://mindspark.com/terms-of-service")
                                }
                            )
                        }
                    }
                }
            }

            // 3. Bottom Camera Controls
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = if (currentMode == CameraMode.PHOTO) {
                            "Please place your hand inside the box"
                        } else {
                            if (isRecording) "Recording..." else "Tap to start recording"
                        },
                        color = if (isRecording) Color.Red else Color(0xFF00FF66),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 40.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Gallery Thumbnail Button
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.DarkGray)
                            .clickable { openGallery(context) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🖼️", fontSize = 24.sp)
                    }

                    Spacer(modifier = Modifier.width(36.dp))

                    // Shutter / Record Button
                    Box(
                        modifier = Modifier
                            .size(78.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.3f))
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(if (isRecording) RoundedCornerShape(8.dp) else CircleShape)
                                .background(if (currentMode == CameraMode.VIDEO) Color.Red else Color.White)
                                .clickable {
                                    if (currentMode == CameraMode.PHOTO) {
                                        takePhotoAndSaveToGallery(context, imageCapture) { savedUri ->
                                            capturedType = CameraMode.PHOTO
                                            sourceText = "ආයුබෝවන්"
                                            translatedText = "Hello"
                                            isCaptured = true
                                            speakText(translatedText, targetLanguage)
                                        }
                                    } else {
                                        if (isRecording) {
                                            activeRecording?.stop()
                                            activeRecording = null
                                            isRecording = false
                                        } else {
                                            startVideoRecording(
                                                context = context,
                                                videoCapture = videoCapture,
                                                activeRecording = activeRecording,
                                                onRecordingStarted = { recording ->
                                                    activeRecording = recording
                                                    isRecording = true
                                                },
                                                onRecordingFinished = { savedUri ->
                                                    capturedType = CameraMode.VIDEO
                                                    sourceText = "ස්තුතියි"
                                                    translatedText = "Thank You"
                                                    isCaptured = true
                                                    speakText(translatedText, targetLanguage)
                                                }
                                            )
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {}
                    }

                    Spacer(modifier = Modifier.width(36.dp))

                    // Switch Camera Button
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable {
                                lensFacing = if (lensFacing == CameraSelector.DEFAULT_BACK_CAMERA) {
                                    CameraSelector.DEFAULT_FRONT_CAMERA
                                } else {
                                    CameraSelector.DEFAULT_BACK_CAMERA
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🔄", color = Color.White, fontSize = 22.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Bottom Mode Selector
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(Color(0xFF1E1E1E))
                        .padding(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
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

            // 4. Translation Result Overlay
            AnimatedVisibility(
                visible = isCaptured,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.9f))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF1E1E1E))
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = sourceLanguage,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.1f))
                                        .clickable { speakText(sourceText, sourceLanguage) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "🔊", fontSize = 14.sp)
                                }
                            }

                            Text(
                                text = "⇄",
                                color = Color(0xFF00FF66),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    val tempLang = sourceLanguage
                                    sourceLanguage = targetLanguage
                                    targetLanguage = tempLang

                                    val tempText = sourceText
                                    sourceText = translatedText
                                    translatedText = tempText
                                }
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = targetLanguage,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.1f))
                                        .clickable { speakText(translatedText, targetLanguage) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "🔊", fontSize = 14.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = if (capturedType == CameraMode.PHOTO) "📷 Photo Translation" else "🎥 Video Translation",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF2B2B2B))
                                .padding(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = sourceText,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "🔊",
                                    fontSize = 16.sp,
                                    modifier = Modifier.clickable { speakText(sourceText, sourceLanguage) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black)
                                .padding(18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = translatedText,
                                    color = Color(0xFF00FF66),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "🔊",
                                    fontSize = 18.sp,
                                    modifier = Modifier.clickable { speakText(translatedText, targetLanguage) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50.dp))
                                    .background(Color.DarkGray)
                                    .clickable {
                                        ttsEngine?.stop()
                                        isCaptured = false
                                    }
                                    .padding(horizontal = 28.dp, vertical = 12.dp)
                            ) {
                                Text(text = "Retake", color = Color.White, fontSize = 15.sp)
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50.dp))
                                    .background(Color(0xFF00FF66))
                                    .clickable {
                                        ttsEngine?.stop()
                                        isCaptured = false
                                    }
                                    .padding(horizontal = 28.dp, vertical = 12.dp)
                            ) {
                                Text(text = "Done", color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}