# Implementation Plan - Fix KSP IllegalStateException: unexpected jvm signature V

The project is encountering a `java.lang.IllegalStateException: unexpected jvm signature V` during the KSP task. This is a known compatibility issue between Room 2.6.x and Kotlin 2.0.x when using KSP2 (which is enabled by default in KSP 2.0+). The error is triggered by `suspend` functions in Room DAOs that return `Unit`.

## Proposed Changes

### Build Configuration

#### [MODIFY] [libs.versions.toml](file:///C:/Users/user/AndroidStudioProjects/mindspark2/gradle/libs.versions.toml)
- Upgrade `room` version from `2.6.1` to `2.8.4` (or the latest version compatible with Kotlin 2.0).

#### [MODIFY] [build.gradle.kts (app)](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/build.gradle.kts)
- Update Room dependencies to use the version catalog (`libs.room.runtime`, `libs.room.ktx`, `libs.room.compiler`) instead of hardcoded strings with a local `roomVersion` variable.

#### [MODIFY] [gradle.properties](file:///C:/Users/user/AndroidStudioProjects/mindspark2/gradle.properties)
- Add `ksp.useKSP2=false` as a fallback if the Room upgrade doesn't fully resolve the issue, although Room 2.7.0+ should support KSP2. I will first try with just the Room upgrade.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:kspDebugKotlin` to verify the KSP task completes successfully.
- Run a full build: `./gradlew assembleDebug`.

### Manual Verification
- Verify that the `UserDao` still works as expected in the app (if a device is available for testing, otherwise build success is the primary indicator).
