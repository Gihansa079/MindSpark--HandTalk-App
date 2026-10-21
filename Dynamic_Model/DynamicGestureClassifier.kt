package com.example.mindspark2

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

class DynamicGestureClassifier(context: Context) {

    private val labels = context.assets.open(
        "dynamic_labels.txt"
    ).bufferedReader().readLines()
        .filter { it.isNotBlank() }

    private val interpreter: Interpreter

    init {
        val modelBytes = context.assets.open(
            "handtalk_dynamic.tflite"
        ).use { it.readBytes() }

        val buffer = ByteBuffer.allocateDirect(
            modelBytes.size
        ).order(ByteOrder.nativeOrder())

        buffer.put(modelBytes)
        buffer.rewind()

        interpreter = Interpreter(buffer)
    }

    data class Prediction(
        val label: String,
        val confidence: Float
    )

    fun predict(frames: FloatArray): Prediction {

        require(frames.size == 30 * 128 * 128 * 3) {
            "Expected 30 RGB frames of size 128x128"
        }

        val input = ByteBuffer.allocateDirect(
            frames.size * 4
        ).order(ByteOrder.nativeOrder())

        frames.forEach { input.putFloat(it) }
        input.rewind()

        val output = Array(1) {
            FloatArray(labels.size)
        }

        interpreter.run(input, output)

        val scores = output[0]
        val index = scores.indices.maxByOrNull {
            scores[it]
        } ?: 0

        return Prediction(
            label = labels[index],
            confidence = scores[index]
        )
    }

    fun close() {
        interpreter.close()
    }
}
