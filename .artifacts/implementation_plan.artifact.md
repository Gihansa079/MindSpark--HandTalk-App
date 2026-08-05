# Implementation Plan - Fix Conflicting Overloads and Broken Activities

The project is failing to build due to "Conflicting overloads" for the `HistoryScreen` composable. This is caused by multiple files in the `com.example.mindspark2.camera` package defining the same function. Additionally, several Activity files have been overwritten with dummy composable code and lack their necessary class definitions.

## Proposed Changes

### 1. Consolidate History Components
I will keep `HistoryScreen.kt` as the single source of truth for history-related UI and data models.

#### [MODIFY] [HistoryScreen.kt](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/src/main/java/com/example/mindspark2/HistoryScreen.kt)
- This file already contains the most complete implementation (using `HistoryViewModel`).
- I will ensure it defines the canonical `HistoryItem` and `HistoryScreen`.

#### [MODIFY] [AppNavigation.kt](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/src/main/java/com/example/mindspark2/AppNavigation.kt)
- **Delete** the redundant `HistoryScreen` definition.
- (Optional/Future) This file should ideally house the `NavHost`, but for now, cleaning it is the priority.

#### [MODIFY] [HistoryActivity.kt](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/src/main/java/com/example/mindspark2/HistoryActivity.kt)
- **Delete** the redundant `HistoryScreen`, `HistoryItem`, and `HistoryCard` definitions.
- Restore the `HistoryActivity` class to host the `HistoryScreen`.

### 2. Restore Broken Activities
`CameraActivity` and `HomeActivity` currently contain only a dummy `HistoryScreen` and no Activity class. This will cause runtime crashes when navigating to them.

#### [MODIFY] [CameraActivity.kt](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/src/main/java/com/example/mindspark2/CameraActivity.kt)
- **Delete** the dummy `HistoryScreen`.
- Implement `CameraActivity` class which hosts `CameraScreen`.

#### [MODIFY] [HomeActivity.kt](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/src/main/java/com/example/mindspark2/HomeActivity.kt)
- **Delete** the dummy `HistoryScreen` and `HistoryItem`.
- Implement `HomeActivity` class. Based on the project structure, `HomeActivity` should likely serve as the main dashboard.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:compileDebugKotlin` to verify that the conflicting overloads error is resolved.

### Manual Verification
- Deploy the app and verify:
    - Login -> Home works.
    - Home -> Camera works.
    - Camera -> History works.
