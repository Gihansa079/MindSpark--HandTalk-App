# Fix Unresolved Reference 'icons' in HomeActivity.kt

The project is failing to build because `androidx.compose.material.icons.Icons` and related icon classes are not found. This is because the Material 3 library does not bundle the Material Icons by default. We need to add the `androidx.compose.material:material-icons-extended` dependency.

## Proposed Changes

### Build Configuration

#### [MODIFY] [libs.versions.toml](file:///C:/Users/user/AndroidStudioProjects/mindspark2/gradle/libs.versions.toml)
- Add `androidx-compose-material-icons-extended` to the `[libraries]` section.

#### [MODIFY] [build.gradle.kts](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/build.gradle.kts)
- Add `implementation(libs.androidx.compose.material.icons.extended)` to the `dependencies` block.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:assembleDebug` to verify that the project builds successfully.

### Manual Verification
- Check `HomeActivity.kt` in the IDE to ensure that the imports are no longer marked as unresolved (this happens automatically once Gradle syncs).
