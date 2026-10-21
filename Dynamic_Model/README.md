# HandTalk Android Integration

## Files

- handtalk_dynamic.tflite
- hand_landmarker.task
- dynamic_labels.txt
- dynamic_model_config.json
- DynamicGestureClassifier.kt
- DynamicGestureVideoRecognizer.kt

## Android assets

Copy these files into:

app/src/main/assets/

- handtalk_dynamic.tflite
- hand_landmarker.task
- dynamic_labels.txt
- dynamic_model_config.json

## Kotlin files

Copy into:

app/src/main/java/com/example/mindspark2/

- DynamicGestureClassifier.kt
- DynamicGestureVideoRecognizer.kt

## Model preprocessing

- 30 frames per video
- 128 x 128 RGB
- Float32
- Pixel values: 0 to 255
- Do NOT divide by 255

## Inference example

val frames = DynamicGestureVideoRecognizer.extractFrames(
    context, videoUri
)

val classifier = DynamicGestureClassifier(context)

val result = classifier.predict(frames)

println(result.label)
println(result.confidence)

classifier.close()

## Important

If conversion_mode is SELECT_TF_OPS,
the Android project needs the TensorFlow Lite
Select TF Ops runtime.

MediaPipe hand_landmarker.task is for hand detection.
The dynamic CNN+LSTM model uses RGB frames,
not landmark coordinate arrays.

Video decoding and frame sampling on Android may
differ from the OpenCV training pipeline.
Validate predictions on the same video files.
