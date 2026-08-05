# Fix Room/KSP Error: "unexpected jvm signature V"

The project is experiencing a build error `[ksp] java.lang.IllegalStateException: unexpected jvm signature V`. This is a known compatibility issue between Room 2.6.x and KSP2 (used in Kotlin 2.0+). The error occurs when processing `suspend` DAO methods that return `Unit`.

Additionally, I found incorrect imports for `UserProfileEntity` in `AppDatabase.kt` and `ProfileActivity.kt` that need to be fixed.

## Proposed Changes

### Build Configuration

#### [MODIFY] [libs.versions.toml](file:///C:/Users/user/AndroidStudioProjects/mindspark2/gradle/libs.versions.toml)
- Upgrade `room` version to `2.7.0-alpha11` (or higher) to fix the KSP2 compatibility issue.

#### [MODIFY] [build.gradle.kts (app)](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/build.gradle.kts)
- Remove hardcoded `roomVersion = "2.6.1"`.
- Use version catalog references for Room dependencies (`libs.room.runtime`, `libs.room.ktx`, `libs.room.compiler`).

### Source Code

#### [MODIFY] [AppDatabase.kt](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/src/main/java/com/example/mindspark2/AppDatabase.kt)
- Fix incorrect import: `import com.example.mindspark2.data.UserProfileEntity` -> `import com.example.mindspark2.UserProfileEntity` (or just remove it as they are in the same package).

#### [MODIFY] [ProfileActivity.kt](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/src/main/java/com/example/mindspark2/ProfileActivity.kt)
- Fix incorrect import: `import com.example.mindspark2.data.UserProfileEntity` -> `import com.example.mindspark2.UserProfileEntity`.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:kspDebugKotlin` to verify the KSP error is resolved.
- Run `./gradlew assembleDebug` to ensure the project builds successfully.

### Manual Verification
- Verify that the Room database is correctly generated and accessible.
