# Implementation Plan - Fix Unresolved reference 'AppNavigation'

The build error `Unresolved reference 'AppNavigation'` occurs because `CameraActivity.kt` attempts to call an `AppNavigation()` composable that is not defined in the project. Although `AppNavigation.kt` exists, it only contains the `HistoryScreen` composable.

## User Review Required

> [!IMPORTANT]
> I will be adding a `NavHost` to `AppNavigation.kt` to manage navigation between `CameraScreen` and `HistoryScreen`. This is a standard Jetpack Compose navigation implementation.

## Proposed Changes

### Navigation

#### [MODIFY] [AppNavigation.kt](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/src/main/java/com/example/mindspark2/AppNavigation.kt)

- **Add** the `AppNavigation()` composable.
- **Implement** `NavHost` with two destinations:
    - `"camera_screen"` (Start Destination) calling `CameraScreen(navController)`.
    - `"history_screen"` calling `HistoryScreen(navController)`.
- **Add** necessary imports for Compose Navigation (`rememberNavController`, `NavHost`, `composable`).

## Verification Plan

### Automated Tests
- I will run `./gradlew :app:compileDebugKotlin` to verify that the unresolved reference error is resolved and the project builds successfully.

### Manual Verification
- N/A (Build fix)
