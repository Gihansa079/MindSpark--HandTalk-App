# Fix Room KSP Error "unexpected jvm signature V"

The project is encountering a KSP error `java.lang.IllegalStateException: unexpected jvm signature V` during the build process. This error is a known incompatibility between Room 2.6.1 and Kotlin 2.0 when using KSP, especially with `suspend` functions returning `Unit`.

Additionally, a wrong import was found in `AppDatabase.kt` which should be corrected.

## Proposed Changes

### Build Configuration

#### [MODIFY] [libs.versions.toml](file:///C:/Users/user/AndroidStudioProjects/mindspark2/gradle/libs.versions.toml)
- Upgrade `room` version from `2.6.1` to `2.7.0` (or `2.8.4` as per latest stable) to support Kotlin 2.0 and KSP2.

#### [MODIFY] [build.gradle.kts (:app)](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/build.gradle.kts)
- Update Room dependencies to use the Version Catalog (`libs.room.*`) instead of hardcoded versions. This ensures consistency and uses the upgraded version.

### Source Code

#### [MODIFY] [AppDatabase.kt](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/src/main/java/com/example/mindspark2/AppDatabase.kt)
- Remove the incorrect import `com.example.mindspark2.data.UserProfileEntity`. Since `AppDatabase` and `UserProfileEntity` are in the same package, an import is not needed, or it should be corrected to `com.example.mindspark2.UserProfileEntity`.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:kspDebugKotlin` to verify that KSP processing completes without errors.
- Run a full build `./gradlew assembleDebug` to ensure everything compiles and links correctly.

### Manual Verification
- Verify in Android Studio that Room-generated classes (if any are inspected) are present.
