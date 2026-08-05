# Migration Plan: kotlinOptions to compilerOptions

The project is currently using the deprecated `kotlinOptions` DSL in `app/build.gradle.kts` for setting the `jvmTarget`. This needs to be migrated to the `compilerOptions` DSL as recommended by Kotlin 2.0 and AGP 8.7+.

## Proposed Changes

### [app module](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/build.gradle.kts)

#### [MODIFY] [build.gradle.kts](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/build.gradle.kts)
- Remove the deprecated `kotlinOptions` block from the `android` extension.
- Consolidate the `jvmTarget` configuration into the existing `tasks.withType<KotlinCompile>` block.
- Update `jvmTarget` to `JVM_17` to match the project's `compileOptions` (currently it is inconsistently set to `JVM_21` in the task configuration).

## Verification Plan

### Automated Tests
- Run Gradle sync to ensure the deprecation warning is gone and the build script is valid.
- `gradlew :app:assembleDebug` to verify that the project still compiles correctly with the updated configuration.
