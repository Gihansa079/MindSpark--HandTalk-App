# Implementation Plan: Fix `jvmTarget` Deprecation

Migrate from the deprecated `kotlinOptions` DSL to the `compilerOptions` DSL in `app/build.gradle.kts`.

## Proposed Changes

### [app module](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/build.gradle.kts)

#### [MODIFY] [build.gradle.kts](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/build.gradle.kts)

- Remove the deprecated `kotlinOptions` block inside the `android` extension.
- Update the existing `tasks.withType<KotlinCompile>` configuration to use `JVM_17` (to match `compileOptions`) instead of `JVM_21`, or keep it as `JVM_21` if preferred.
- Given that `compileOptions` is set to `VERSION_17`, I will set `jvmTarget` to `JVM_17` to maintain consistency across the project.

## Verification Plan

### Automated Tests
- Run `gradlew clean assembleDebug` to ensure the project builds without the deprecation warning.
- Run `gradlew help` or any simple task to verify Gradle sync/configuration works.

### Manual Verification
- Check the "Build" tab in Android Studio after sync to confirm the warning is gone.
