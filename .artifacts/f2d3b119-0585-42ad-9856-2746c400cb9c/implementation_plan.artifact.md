# Fix Unresolved Reference 'icons' in HomeActivity.kt

The project is currently failing to build because it references `androidx.compose.material.icons.Icons` and various Material icons (like `CameraAlt`, `History`, etc.) in `HomeActivity.kt`, but the necessary Material Icons dependencies are missing from the project configuration.

## Proposed Changes

### Build Configuration

#### [MODIFY] [libs.versions.toml](file:///C:/Users/user/AndroidStudioProjects/mindspark2/gradle/libs.versions.toml)
- Add definitions for `androidx.compose.material:material-icons-core` and `androidx.compose.material:material-icons-extended`. These libraries provide the predefined Material Design icons used in the app.

#### [MODIFY] [build.gradle.kts](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/build.gradle.kts)
- Add the new icon libraries to the `dependencies` block. We will use the Compose BOM (Bill of Materials) to manage the versions automatically.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:compileDebugKotlin` to verify that the unresolved reference error is resolved and the project builds successfully.

### Manual Verification
- Sync Gradle in the IDE and ensure that the imports in `HomeActivity.kt` are no longer flagged as errors.
