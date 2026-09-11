# Fix "Unresolved reference 'gestureClassifier'" Build Error

The project fails to build because the variable `gestureClassifier` is used in `CameraScreen.kt` but is never defined or instantiated. The `GestureClassifier` class exists in `gestureClassifier.kt`.

## User Review Required

> [!IMPORTANT]
> **Package Inconsistency**: Both `CameraScreen.kt` and `gestureClassifier.kt` declare `package com.example.mindspark2.camera`, but they are located in `app/src/main/java/com/example/mindspark2/`. While this might not block compilation in all environments, it is non-standard. I will keep the package as-is to minimize changes to other files (like `AppNavigation.kt` which imports this package), but be aware of this discrepancy.

## Proposed Changes

### [Component: Camera]

#### [MODIFY] [CameraScreen.kt](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/src/main/java/com/example/mindspark2/CameraScreen.kt)
- Instantiate `GestureClassifier` inside the `CameraScreen` composable using `remember`.
- Use `DisposableEffect` to properly close the `GestureClassifier` (and its TFLite interpreter) when the composable is disposed.
- Pass the `gestureClassifier` instance to the callbacks or ensure it is accessible in their scope.

#### [MODIFY] [gestureClassifier.kt](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/src/main/java/com/example/mindspark2/gestureClassifier.kt)
- Update `classify` method signature to accept `List<HandLandmark>`.
- Implement the landmark-to-buffer mapping:
    ```kotlin
    landmarks.forEach { landmark ->
        inputBuffer.putFloat(landmark.x)
        inputBuffer.putFloat(landmark.y)
        inputBuffer.putFloat(0.0f) // Assuming z=0 as HandLandmark only has x,y
    }
    ```

## Verification Plan

### Automated Tests
- Run `./gradlew :app:compileDebugKotlin` to verify that the unresolved reference error is resolved.

### Manual Verification
- Deploy the app to a device/emulator and verify that the camera screen opens without crashing.
