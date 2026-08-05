# Implementation Plan - Fix Unresolved Reference 'HistoryViewModel'

The project fails to build because `HistoryViewModel` and `HistoryItem` are not defined. These classes are referenced in `AppNavigation.kt`, `CameraScreen.kt`, and `HistoryScreen.kt`. I will create a new file `HistoryViewModel.kt` to provide these definitions.

## User Review Required

> [!NOTE]
> This change introduces the missing `HistoryViewModel.kt` file which was likely accidentally deleted or omitted during previous refactoring sessions. It restores the expected state of the project.

## Proposed Changes

### [app]

#### [NEW] [HistoryViewModel.kt](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/src/main/java/com/example/mindspark2/HistoryViewModel.kt)
- Define `HistoryItem` data class with `id`, `sinhalaText`, `englishText`, `category`, and `timestamp`.
- Define `HistoryViewModel` class extending `ViewModel`.
- Implement `historyList` as a `MutableStateFlow` exposed as a `StateFlow`.
- Implement `addTranslation(sinhala: String, english: String, category: String)` to prepend new items to the history.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:compileDebugKotlin` to verify the build error is resolved.
