# Fix Build Errors and Consolidate Project Structure

The project is currently failing to build due to a syntax error in `AppNavigation.kt` (duplicate package declaration) and numerous "Unresolved reference" and "Conflicting overloads" errors. These stem from inconsistent package declarations, duplicate data class/composable definitions, and missing methods in `HistoryViewModel`.

## User Review Required

> [!IMPORTANT]
> I will be consolidating `HistoryItem` and `HistoryScreen` into single files and removing duplicates from Activity files. This follows best practices but might remove code you were experimenting with in those activities.

## Proposed Changes

### [Core Navigation & Packages]

#### [MODIFY] [AppNavigation.kt](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/src/main/java/com/example/mindspark2/AppNavigation.kt)
- Ensure a single `package com.example.mindspark2` declaration.
- Remove redundant imports for `CameraScreen` and `HistoryScreen` once their packages are fixed.

#### [MODIFY] [CameraScreen.kt](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/src/main/java/com/example/mindspark2/CameraScreen.kt)
#### [MODIFY] [HistoryScreen.kt](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/src/main/java/com/example/mindspark2/HistoryScreen.kt)
#### [MODIFY] [HistoryActivity.kt](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/src/main/java/com/example/mindspark2/HistoryActivity.kt)
#### [MODIFY] [HomeActivity.kt](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/src/main/java/com/example/mindspark2/HomeActivity.kt)
- Change `package com.example.mindspark2.camera` to `package com.example.mindspark2` to match file location and other core components.

### [Data & Logic Consolidation]

#### [MODIFY] [HistoryViewModel.kt](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/src/main/java/com/example/mindspark2/HistoryViewModel.kt)
- Implement `historyList` StateFlow.
- Implement `addTranslation` function to fix unresolved references in `CameraScreen.kt`.

#### [MODIFY] [HistoryActivity.kt](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/src/main/java/com/example/mindspark2/HistoryActivity.kt)
#### [MODIFY] [HomeActivity.kt](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/src/main/java/com/example/mindspark2/HomeActivity.kt)
- Remove duplicate `HistoryItem` data class.
- Remove duplicate `HistoryScreen` composable function.

#### [MODIFY] [CameraActivity.kt](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/src/main/java/com/example/mindspark2/CameraActivity.kt)
- Remove duplicate `HistoryScreen` composable function.

### [Fix Remaining References]

#### [MODIFY] [Login Activity.kt](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/src/main/java/com/example/mindspark2/Login%20Activity.kt)
#### [MODIFY] [RegisterActivity.kt](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/src/main/java/com/example/mindspark2/RegisterActivity.kt)
- Update package/imports to correctly reference `HomeActivity`.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:compileDebugKotlin` to ensure all syntax and reference errors are resolved.

### Manual Verification
- Verify that `AppNavigation` correctly links `CameraScreen` and `HistoryScreen`.
- Ensure translations captured in `CameraScreen` are correctly added to the `HistoryViewModel` and displayed in `HistoryScreen`.
