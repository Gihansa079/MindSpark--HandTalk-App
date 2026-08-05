# Implementation Plan - Fix Redeclaration of MainActivity

The build is failing because `MainActivity` is declared in two different files:
1. `MainActivity.kt`: Contains the Splash Screen logic.
2. `WelcomeScreen.kt`: Contains the Welcome Screen logic.

I will rename the `MainActivity` class in `WelcomeScreen.kt` to `WelcomeActivity` and update the application flow so that the Splash Screen navigates to the Welcome Screen.

## Proposed Changes

### [UI Components]

#### [MODIFY] [WelcomeScreen.kt](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/src/main/java/com/example/mindspark2/WelcomeScreen.kt)
- Rename `class MainActivity` to `class WelcomeActivity`.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/src/main/java/com/example/mindspark2/MainActivity.kt)
- Update `LaunchedEffect` to navigate to `WelcomeActivity` instead of `LoginActivity`.

### [Manifest]

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/src/main/AndroidManifest.xml)
- Add `<activity android:name=".WelcomeActivity" android:exported="false" />` to the manifest.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:compileDebugKotlin` to ensure the redeclaration error is resolved.

### Manual Verification
- Deploy the app to a device or emulator.
- Verify the following flow:
    1. App starts with the Splash Screen (`MainActivity`).
    2. After 3 seconds, it navigates to the Welcome Screen (`WelcomeActivity`).
    3. Clicking "Login" in the Welcome Screen navigates to the Login Screen (`LoginActivity`).
