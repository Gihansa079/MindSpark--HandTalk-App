# Implementation Plan - Fix Redeclaration of HistoryItem and HistoryScreen

The project has two files defining `HistoryItem` and `HistoryScreen` in the same package (`com.example.mindspark2.history`), causing a "Redeclaration" compiler error.

- [HistoryActivity.kt](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/src/main/java/com/example/mindspark2/HistoryActivity.kt)
- [HistoryScreen.kt](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/src/main/java/com/example/mindspark2/HistoryScreen.kt)

Both files are being used: `HistoryActivity` is launched via Intent from `HomeActivity`, and `HistoryScreen` (composable) is used via Compose Navigation from `CameraScreen`.

## Proposed Changes

### 1. Unified History Model
Create a unified `HistoryItem` data class that can serve both screens, or rename the models to be distinct. Given they represent the same entity with slightly different metadata, a unified model is preferred.

#### [NEW] [HistoryItem.kt](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/src/main/java/com/example/mindspark2/HistoryItem.kt)
- Define a unified `HistoryItem` in `com.example.mindspark2.history`.

### 2. Resolve Composable Name Collision
Rename the `HistoryScreen` in `HistoryScreen.kt` to `LensHistoryScreen` to distinguish it from the one used in `HistoryActivity`.

#### [MODIFY] [HistoryScreen.kt](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/src/main/java/com/example/mindspark2/HistoryScreen.kt)
- Rename `HistoryScreen` to `LensHistoryScreen`.
- Use the unified `HistoryItem` (or rename its local `HistoryItem` if unification is complex).
- Update usages of `HistoryItem` to match the new constructor.

#### [MODIFY] [AppNavigation.kt](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/src/main/java/com/example/mindspark2/AppNavigation.kt)
- Update the route for `"history_screen"` to call `LensHistoryScreen(navController = navController)`.
- Add necessary import for `LensHistoryScreen`.

### 3. Clean up HistoryActivity
#### [MODIFY] [HistoryActivity.kt](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/src/main/java/com/example/mindspark2/HistoryActivity.kt)
- Remove the local definition of `HistoryItem`.
- Ensure it uses the unified `HistoryItem`.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:compileDebugKotlin` to ensure the redeclaration error is gone and the project builds.

### Manual Verification
- Verify that `HomeActivity` -> `History Log` still works.
- Verify that `CameraScreen` -> `History Icon` still works.
