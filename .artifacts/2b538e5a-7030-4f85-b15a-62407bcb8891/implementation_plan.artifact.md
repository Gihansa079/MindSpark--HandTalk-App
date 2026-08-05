# Implementation Plan - Fix Room KSP "unexpected jvm signature V" Error

The project is encountering a build error `[ksp] java.lang.IllegalStateException: unexpected jvm signature V` when compiling with Kotlin 2.0.21 and KSP. This is a known issue in the KSP2 backend when processing Room DAO `suspend` functions that return `Unit`.

## User Review Required

> [!IMPORTANT]
> I am proposing an upgrade of the Room library from `2.6.1` to `2.8.4`. This version includes fixes for KSP2 compatibility. While Room is generally backward compatible, upgrading between minor versions should be verified.

## Proposed Changes

### Build Configuration

#### [MODIFY] [libs.versions.toml](file:///C:/Users/user/AndroidStudioProjects/mindspark2/gradle/libs.versions.toml)
- Update `room` version to `2.8.4`.

#### [MODIFY] [app/build.gradle.kts](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/build.gradle.kts)
- Replace hardcoded Room dependencies with references from the version catalog (`libs.room.runtime`, `libs.room.ktx`, and `libs.room.compiler`).

### Source Code

#### [MODIFY] [UserDao.kt](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/src/main/java/com/example/mindspark2/UserDao.kt)
- Explicitly add the `: Unit` return type to the `suspend` function `insertOrUpdateUser` as an additional safeguard.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:kspDebugKotlin` to verify the KSP processing now completes without error.
- Run a full build: `./gradlew assembleDebug`.

### Manual Verification
- Verify that the Room database still functions correctly by deploying the app (if a device is available and applicable).
