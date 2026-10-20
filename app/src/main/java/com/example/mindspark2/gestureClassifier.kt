package com.example.mindspark2.camera

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.DataType
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

// Detailed result for recognition and database saving.
// Public fields are unchanged so CameraScreen, ImageActivity, and History keep working.
data class GesturePrediction(
    val classIndex: Int,
    val sinhalaText: String,
    val confidence: Float
)

class GestureClassifier(
    context: Context,
    modelFileName: String = "ssl_static_model.tflite"
) {

    private var interpreter: Interpreter? = null

    companion object {
        private const val TAG = "GestureClassifier"
        private const val LANDMARK_COUNT = 21
        private const val FEATURE_COUNT = 63
        private const val CONFIDENCE_THRESHOLD = 0.35f
        private const val UNKNOWN_SIGN = "නොදන්නා සංඥාවකි"
        private const val LABELS_FILE = "labels.txt"
    }

    // ==========================================
    // MODEL LABELS
    // ==========================================
    // The new model has 20 classes, not the old 7 classes.
    // The exact order MUST match sklearn LabelEncoder.classes_.
    // Never sort these labels again inside Android.
    private val labels: List<String> = try {
        context.assets.open(LABELS_FILE)
            .bufferedReader(Charsets.UTF_8)
            .use { reader ->
                reader.readLines()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
            }
    } catch (e: Exception) {
        Log.e(TAG, "Could not read $LABELS_FILE from assets", e)
        emptyList()
    }

    // Translate only for display and history text.
    // Model indices are always based on labels.txt, not on this map.
    private val sinhalaNames: Map<String, String> = mapOf(
        "I(mama)" to "මම",
        "ayubowan" to "ආයුබෝවන්",
        "brother" to "සහෝදරයා",
        "five" to "පහ",
        "four" to "හතර",
        "good(hodai)" to "හොඳයි",
        "his" to "ඔහුගේ",
        "house" to "ගෙදර / නිවස",
        "i_love_you" to "මම ඔයාට ආදරෙයි",
        "me(mata)" to "මට",
        "my(mage)" to "මගේ",
        "naraka" to "නරකයි",
        "oba" to "ඔබ",
        "one" to "එක",
        "sister" to "සහෝදරිය",
        "three" to "තුන",
        "two" to "දෙක",
        "what" to "මොකක්ද",
        "who" to "කවුද",
        "why" to "ඇයි"
    )

    // ==========================================
    // 1. LOAD TFLITE MODEL
    // ==========================================
    init {
        try {
            require(labels.isNotEmpty()) {
                "$LABELS_FILE is missing or empty"
            }

            val modelBuffer = loadModelFile(
                context,
                modelFileName
            )

            val options = Interpreter.Options().apply {
                setNumThreads(2)
            }

            val loadedInterpreter = Interpreter(
                modelBuffer,
                options
            )

            val inputTensor = loadedInterpreter.getInputTensor(0)
            val outputTensor = loadedInterpreter.getOutputTensor(0)

            val inputShape = inputTensor.shape()
            val outputShape = outputTensor.shape()

            Log.d(
                TAG,
                "Input shape = ${inputShape.contentToString()}"
            )

            Log.d(
                TAG,
                "Output shape = ${outputShape.contentToString()}"
            )

            Log.d(
                TAG,
                "Input type = ${inputTensor.dataType()}"
            )

            Log.d(
                TAG,
                "Output type = ${outputTensor.dataType()}"
            )

            Log.d(TAG, "Loaded ${labels.size} labels: $labels")

            // New model expects [1, 63] input and [1, 20] output.
            // Use labels.size for the output size to detect mismatches.
            val validInput = inputShape.contentEquals(
                intArrayOf(1, FEATURE_COUNT)
            )

            val validOutput = outputShape.contentEquals(
                intArrayOf(1, labels.size)
            )

            val validTypes =
                inputTensor.dataType() == DataType.FLOAT32 &&
                        outputTensor.dataType() == DataType.FLOAT32

            if (!validInput || !validOutput || !validTypes) {
                loadedInterpreter.close()

                Log.e(
                    TAG,
                    "Model shape/type mismatch. " +
                            "Expected input [1,63] FLOAT32 and " +
                            "output [1,${labels.size}] FLOAT32."
                )
            } else {
                interpreter = loadedInterpreter

                Log.d(
                    TAG,
                    "New static TFLite model loaded successfully"
                )
            }

        } catch (e: Exception) {
            Log.e(
                TAG,
                "Error initializing TFLite model",
                e
            )

            interpreter = null
        }
    }

    // ==========================================
    // 2. LOAD MODEL FROM ASSETS
    // ==========================================
    private fun loadModelFile(
        context: Context,
        modelFileName: String
    ): ByteBuffer {

        context.assets.openFd(modelFileName).use { descriptor ->
            FileInputStream(
                descriptor.fileDescriptor
            ).use { inputStream ->
                return inputStream.channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    descriptor.startOffset,
                    descriptor.declaredLength
                )
            }
        }
    }

    // ==========================================
    // 3. EXISTING CLASSIFY FUNCTION
    // ==========================================
    // Kept unchanged for CameraScreen.kt and ImageActivity.kt.
    fun classify(
        landmarks: List<HandLandmark>?
    ): String {

        val prediction = classifyWithDetails(landmarks)

        return prediction?.sinhalaText ?: UNKNOWN_SIGN
    }

    // ==========================================
    // 4. DETAILED CLASSIFICATION
    // ==========================================
    // Kept unchanged for existing history/database call sites.
    fun classifyWithDetails(
        landmarks: List<HandLandmark>?
    ): GesturePrediction? {

        val tflite = interpreter ?: run {
            Log.e(TAG, "Interpreter not initialized")
            return null
        }

        if (
            landmarks == null ||
            landmarks.size != LANDMARK_COUNT
        ) {
            Log.e(
                TAG,
                "Expected 21 landmarks but received " +
                        "${landmarks?.size ?: 0}"
            )

            return null
        }

        return try {

            // ==================================
            // Prepare 63 input features
            // ==================================
            val inputBuffer = ByteBuffer.allocateDirect(
                FEATURE_COUNT * java.lang.Float.BYTES
            ).order(ByteOrder.nativeOrder())

            // Training used raw MediaPipe x, y, z values.
            // Do NOT subtract wrist coordinates or rescale them.
            for (landmark in landmarks) {
                inputBuffer.putFloat(landmark.x)
                inputBuffer.putFloat(landmark.y)
                inputBuffer.putFloat(landmark.z)
            }

            inputBuffer.rewind()

            // ==================================
            // Run model prediction
            // ==================================
            val outputArray = Array(1) {
                FloatArray(labels.size)
            }

            synchronized(tflite) {
                tflite.run(
                    inputBuffer,
                    outputArray
                )
            }

            val probabilities = outputArray[0]

            // ==================================
            // Find predicted class
            // ==================================
            val maxIndex = probabilities.indices.maxByOrNull {
                probabilities[it]
            } ?: return null

            if (maxIndex !in labels.indices) {
                Log.e(TAG, "Invalid class index")
                return null
            }

            val confidence = probabilities[maxIndex]
            val englishLabel = labels[maxIndex]
            val sinhalaLabel = sinhalaNames[englishLabel] ?: englishLabel

            Log.d(
                TAG,
                "---------- Prediction ----------"
            )

            for (i in probabilities.indices) {
                Log.d(
                    TAG,
                    "Class $i (${labels[i]}) = ${probabilities[i]}"
                )
            }

            Log.d(TAG, "Predicted class index = $maxIndex")
            Log.d(TAG, "Model class label = $englishLabel")
            Log.d(TAG, "Sinhala sign = $sinhalaLabel")
            Log.d(TAG, "Confidence = $confidence")

            // ==================================
            // Confidence threshold
            // ==================================
            if (
                !confidence.isFinite() ||
                confidence < CONFIDENCE_THRESHOLD
            ) {
                Log.d(
                    TAG,
                    "Unknown sign: low confidence"
                )

                return null
            }

            // ==================================
            // Return complete result
            // ==================================
            GesturePrediction(
                classIndex = maxIndex,
                sinhalaText = sinhalaLabel,
                confidence = confidence
            )

        } catch (e: Exception) {
            Log.e(
                TAG,
                "Classification error",
                e
            )

            null
        }
    }

    // ==========================================
    // 5. RELEASE MODEL RESOURCES
    // ==========================================
    fun close() {
        try {
            interpreter?.close()
            interpreter = null

            Log.d(
                TAG,
                "Interpreter closed"
            )

        } catch (e: Exception) {
            Log.e(
                TAG,
                "Error closing interpreter",
                e
            )
        }
    }
}
