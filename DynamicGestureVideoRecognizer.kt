package com.example.mindspark2

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.content.Context

object DynamicGestureVideoRecognizer {

    private const val SEQ_LEN = 30
    private const val IMG_SIZE = 128

    fun extractFrames(
        context: Context,
        uri: Uri
    ): FloatArray {

        val retriever = MediaMetadataRetriever()
        val result = FloatArray(
            SEQ_LEN * IMG_SIZE * IMG_SIZE * 3
        )

        try {
            retriever.setDataSource(context, uri)

            val durationMs = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLongOrNull() ?: 0L

            require(durationMs > 0) {
                "Invalid video duration"
            }

            var offset = 0

            for (i in 0 until SEQ_LEN) {

                val timeUs = (
                    (durationMs - 1) * 1000L * i /
                    (SEQ_LEN - 1)
                )

                val frame = retriever.getFrameAtTime(
                    timeUs,
                    MediaMetadataRetriever.OPTION_CLOSEST
                ) ?: throw IllegalStateException(
                    "Cannot decode frame $i"
                )

                val resized = Bitmap.createScaledBitmap(
                    frame, IMG_SIZE, IMG_SIZE, true
                )

                val pixels = IntArray(
                    IMG_SIZE * IMG_SIZE
                )

                resized.getPixels(
                    pixels, 0, IMG_SIZE,
                    0, 0, IMG_SIZE, IMG_SIZE
                )

                for (pixel in pixels) {
                    result[offset++] = (
                        (pixel shr 16) and 0xFF
                    ).toFloat()

                    result[offset++] = (
                        (pixel shr 8) and 0xFF
                    ).toFloat()

                    result[offset++] = (
                        pixel and 0xFF
                    ).toFloat()
                }

                if (resized !== frame) resized.recycle()
                frame.recycle()
            }

        } finally {
            retriever.release()
        }

        return result
    }
}
