# Fix Redeclaration Error of MainActivity

The project currently has two files defining `class MainActivity`:
1. `MainActivity.kt` (Splash Screen)
2. `WelcomeScreen.kt` (Welcome Screen with Login/Register buttons)

This causes a compilation error because of duplicate class definitions. I will rename the one in `WelcomeScreen.kt` to `WelcomeActivity` and integrate it into the app flow.

## Proposed Changes

### [Component: UI]

#### [MODIFY] [WelcomeScreen.kt](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/src/main/java/com/example/mindspark2/WelcomeScreen.kt)
- Rename `class MainActivity` to `class WelcomeActivity`.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/src/main/java/com/example/mindspark2/MainActivity.kt)
- Update navigation from `LoginActivity::class.java` to `WelcomeActivity::class.java`.

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/src/main/AndroidManifest.xml)
- Register `WelcomeActivity` in the manifest.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:compileDebugKotlin` to verify the redeclaration error is resolved.

### Manual Verification
- Deploy the app and verify the flow: Splash Screen (3s) -> Welcome Screen -> Login/Register.
