
package com.example.mindspark2

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder

class DynamicGestureClassifier(
    private val context: Context
) : Closeable {

    companion object {
        private const val MODEL_NAME = "handtalk_dynamic.tflite"
        private const val LABELS_NAME = "dynamic_labels.txt"
        private const val FRAME_COUNT = 30
        private const val IMAGE_SIZE = 128
        private const val CHANNELS = 3
    }

    data class Prediction(
        val label: String,
        val confidence: Float,
        val classIndex: Int
    )

    private val labels: List<String> =
        context.assets.open(LABELS_NAME)
            .bufferedReader()
            .use { reader ->
                reader.readLines()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
            }

    private val interpreter: Interpreter

    init {
        val modelBytes = context.assets.open(MODEL_NAME)
            .use { it.readBytes() }

        val modelBuffer = ByteBuffer
            .allocateDirect(modelBytes.size)
            .order(ByteOrder.nativeOrder())

        modelBuffer.put(modelBytes)
        modelBuffer.rewind()

        interpreter = Interpreter(modelBuffer)

        val expectedShape = intArrayOf(
            1, FRAME_COUNT, IMAGE_SIZE, IMAGE_SIZE, CHANNELS
        )

        require(
            interpreter.getInputTensor(0)
                .shape()
                .contentEquals(expectedShape)
        ) {
            "Dynamic model input shape does not match configuration"
        }

        require(
            interpreter.getInputTensor(0).dataType() ==
                    org.tensorflow.lite.DataType.FLOAT32
        ) {
            "Dynamic model input must be FLOAT32"
        }

        require(
            interpreter.getOutputTensor(0).numElements() == labels.size
        ) {
            "Number of output classes does not match labels"
        }
    }

    fun predict(frames: List<Bitmap>): Prediction {
        require(frames.size == FRAME_COUNT) {
            "Exactly 30 frames are required"
        }

        val inputBuffer = ByteBuffer.allocateDirect(
            FRAME_COUNT * IMAGE_SIZE * IMAGE_SIZE *
                    CHANNELS * 4
        ).order(ByteOrder.nativeOrder())

        val pixels = IntArray(IMAGE_SIZE * IMAGE_SIZE)

        for (frame in frames) {
            val resized = Bitmap.createScaledBitmap(
                frame,
                IMAGE_SIZE,
                IMAGE_SIZE,
                true
            )

            resized.getPixels(
                pixels,
                0,
                IMAGE_SIZE,
                0,
                0,
                IMAGE_SIZE,
                IMAGE_SIZE
            )

            for (pixel in pixels) {
                val red = (pixel shr 16) and 0xFF
                val green = (pixel shr 8) and 0xFF
                val blue = pixel and 0xFF

                // Raw RGB values: 0..255
                // Normalization happens inside the model.
                inputBuffer.putFloat(red.toFloat())
                inputBuffer.putFloat(green.toFloat())
                inputBuffer.putFloat(blue.toFloat())
            }

            if (resized !== frame) {
                resized.recycle()
            }
        }

        inputBuffer.rewind()

        val output = Array(1) {
            FloatArray(labels.size)
        }

        interpreter.run(inputBuffer, output)

        val scores = output[0]
        val bestIndex = scores.indices.maxByOrNull {
            scores[it]
        } ?: error("No prediction scores returned")

        return Prediction(
            label = labels[bestIndex],
            confidence = scores[bestIndex],
            classIndex = bestIndex
        )
    }

    override fun close() {
        interpreter.close()
    }
}
