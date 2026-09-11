package com.example.mindspark2.camera

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class GestureClassifier(
    context: Context,
    modelFileName: String = "ssl_static_model.tflite"
) {

    private var interpreter: Interpreter? = null

    // Kaggle Folder Names: A to Z Alphabetical Order
    private val labels = listOf(
        "ආයුබෝවන්",        // Index 0: ayubowan
        "හොඳයි",           // Index 1: good(hodai)
        "ඔහුගේ / ඇයගේ",     // Index 2: his
        "ගෙදර / නිවස",     // Index 3: house
        "මම ඔයාට ආදරෙයි",   // Index 4: i_love_you
        "නරකයි",           // Index 5: naraka
        "ඔබ"               // Index 6: oba
    )

    init {
        try {
            val assetFileDescriptor = context.assets.openFd(modelFileName)
            val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
            val fileChannel = fileInputStream.channel
            val startOffset = assetFileDescriptor.startOffset
            val declaredLength = assetFileDescriptor.declaredLength
            val modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

            val options = Interpreter.Options()
            interpreter = Interpreter(modelBuffer, options)
        } catch (e: Exception) {
            Log.e("GestureClassifier", "Error initializing TFLite interpreter", e)
        }
    }

    fun classify(landmarks: List<HandLandmark>?): String {
        val tflite = interpreter
        if (tflite == null || landmarks.isNullOrEmpty() || landmarks.size < 21) {
            return "නොදන්නා සංඥාවකි"
        }

        // 21 landmarks * 3 coordinates (x, y, z) * 4 bytes per float = 252 bytes
        val inputBuffer = ByteBuffer.allocateDirect(1 * 63 * 4)
        inputBuffer.order(ByteOrder.nativeOrder())
        inputBuffer.rewind()

        // 1. Base Reference point (Wrist landmark - Point 0)
        val baseWristX = landmarks[0].x
        val baseWristY = landmarks[0].y
       // val baseWristZ = landmarks[0].z

        Log.d("GestureClassifier", "Wrist Absolute (0): x=$baseWristX, y=$baseWristY")

        // 2. Relative normalization to match Colab model training setup
        for (i in 0 until 21) {
            val landmark = landmarks[i]
            inputBuffer.putFloat(landmark.x - baseWristX)
            inputBuffer.putFloat(landmark.y - baseWristY)
            //inputBuffer.putFloat(landmark.z - baseWristZ)
        }

        val outputArray = Array(1) { FloatArray(labels.size) }
        tflite.run(inputBuffer, outputArray)

        val probabilities = outputArray[0]

        // 3. Log all output scores to Android Studio Logcat
        for (i in probabilities.indices) {
            Log.d("GestureClassifier", "Class $i (${labels.getOrNull(i)}): ${probabilities[i]}")
        }

        val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: -1

        return if (maxIndex != -1 && maxIndex < labels.size && probabilities[maxIndex] > 0.35f) {
            Log.d("GestureClassifier", "Final Prediction: ${labels[maxIndex]} (Index $maxIndex)")
            labels[maxIndex]
        } else {
            "නොදන්නා සංඥාවකි"
        }
    }

    fun close() {
        interpreter?.close()
    }
}