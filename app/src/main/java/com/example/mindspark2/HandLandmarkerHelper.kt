package com.example.mindspark2.camera

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult

class HandLandmarkerHelper(
    private val context: Context,
    private val onResult: (List<HandLandmark>) -> Unit,
    private val onError: (String) -> Unit
) {

    companion object {

        private const val TAG =
            "HandLandmarkerHelper"

        private const val MODEL_NAME =
            "hand_landmarker.task"
    }


    private var handLandmarker:
            HandLandmarker? = null


    init {

        setupHandLandmarker()
    }


    private fun setupHandLandmarker() {

        try {

            // -----------------------------------
            // MediaPipe model
            // -----------------------------------

            val baseOptions =
                BaseOptions.builder()
                    .setModelAssetPath(
                        MODEL_NAME
                    )
                    .build()


            // -----------------------------------
            // Hand Landmarker options
            // -----------------------------------

            val options =
                HandLandmarker.HandLandmarkerOptions
                    .builder()
                    .setBaseOptions(
                        baseOptions
                    )
                    .setRunningMode(
                        RunningMode.IMAGE
                    )
                    .setNumHands(1)
                    .setMinHandDetectionConfidence(0.3f)
                .setMinHandPresenceConfidence(0.3f)
                    .setMinTrackingConfidence(
                        0.5f
                    )
                    .build()


            handLandmarker =
                HandLandmarker.createFromOptions(
                    context,
                    options
                )


            Log.d(
                TAG,
                "MediaPipe HandLandmarker ready"
            )


        } catch (e: Exception) {

            Log.e(
                TAG,
                "HandLandmarker initialization failed",
                e
            )

            onError(
                e.message
                    ?: "Hand detector initialization failed"
            )
        }
    }


    // =========================================
    // Detect hand
    // =========================================

    fun detect(
        bitmap: Bitmap
    ) {

        val detector =
            handLandmarker ?: return


        try {

            val mpImage =
                BitmapImageBuilder(
                    bitmap
                ).build()


            val result =
                detector.detect(
                    mpImage
                )


            processResult(
                result
            )


        } catch (e: Exception) {

            Log.e(
                TAG,
                "Hand detection failed",
                e
            )

            onError(
                e.message
                    ?: "Hand detection failed"
            )
        }
    }


    // =========================================
    // Convert MediaPipe result
    // =========================================

    private fun processResult(
        result: HandLandmarkerResult
    ) {

        // No hand
        if (
            result.landmarks().isEmpty()
        ) {

            onResult(
                emptyList()
            )

            return
        }


        // First hand
        val hand =
            result.landmarks()[0]


        // Convert to app HandLandmark
        val landmarks =
            hand.map { landmark ->

                HandLandmark(
                    x = landmark.x(),
                    y = landmark.y(),
                    z = landmark.z()
                )
            }


        Log.d(
            TAG,
            "Detected landmarks = ${landmarks.size}"
        )


        onResult(
            landmarks
        )
    }


    // =========================================
    // Close
    // =========================================

    fun close() {

        try {

            handLandmarker?.close()

            handLandmarker = null

            Log.d(
                TAG,
                "HandLandmarker closed"
            )


        } catch (e: Exception) {

            Log.e(
                TAG,
                "Error closing HandLandmarker",
                e
            )
        }
    }
}