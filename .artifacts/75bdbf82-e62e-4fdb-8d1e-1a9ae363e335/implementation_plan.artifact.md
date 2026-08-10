# Implementation Plan - Fix Type Inference and Missing API Methods

The build error `Not enough information to infer type argument for 'K'` in `LearnActivity.kt` is a secondary symptom of missing method definitions in the `ApiService` interface. Specifically, `getAllFavorites`, `addFavorite`, and `removeFavorite` are used in `LearnActivity.kt` but not defined in `ApiService.kt`. This causes the Kotlin compiler to fail at type inference when processing the results of these calls.

## User Review Required

> [!IMPORTANT]
> This fix involves adding missing API endpoint definitions to `ApiService.kt`. I am assuming the endpoint paths and return types based on their usage in `LearnActivity.kt`.

## Proposed Changes

### [Network Layer]

#### [MODIFY] [ApiService.kt](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/src/main/java/com/example/mindspark2/ApiService.kt)
- Add `getAllFavorites`, `addFavorite`, and `removeFavorite` to the `ApiService` interface.

### [UI Layer]

#### [MODIFY] [LearnActivity.kt](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/src/main/java/com/example/mindspark2/LearnActivity.kt)
- Explicitly declare the type of `favoriteTexts` as `Set<String>` to ensure stable type inference.
- Add type arguments to `emptySet()` calls within the `try-catch` block for clarity.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:compileDebugKotlin` to verify that the compilation errors are resolved.

### Manual Verification
- N/A (Build fix)
