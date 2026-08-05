# Implementation Plan - MindSparks UI Overhaul

The goal is to update the entire app's UI to match the provided screenshot. This includes updating existing screens (Splash, Login, Register, Home, Camera, Translation Result) and adding new ones (Welcome, History, Tutorial, Settings).

## User Review Required

> [!IMPORTANT]
> The overhaul involves a significant change in the visual identity of the app (renaming "HandTalk" to "MindSparks", new color palette, and new layout structures).

> [!NOTE]
> I will be creating new files for the screens that don't exist yet and updating the existing ones.

## Proposed Changes

### Core & Theme
- Update `Color.kt` and `Theme.kt` to match the vibrant blue and white theme from the screenshot.
- Define a common `PrimaryBlue` and other supporting colors.

### Screens [MODIFY]

#### [MainActivity.kt](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/src/main/java/com/example/mindspark2/MainActivity.kt)
- Update `SplashScreen` to match the screenshot (Logo, "MindSparks" name, "Sinhala Sign Language Translator" subtext, loading dots).
- Add navigation logic to the new `WelcomeScreen`.

#### [Login Activity.kt](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/src/main/java/com/example/mindspark2/Login%20Activity.kt)
- Update `LoginScreen` layout:
    - Centered "Login" title.
    - Input fields with leading icons and password visibility toggle.
    - Blue button with rounded corners.
    - "Forgot Password?" and "Register" links styling.
    - Background wave/decorations.

#### [RegisterActivity.kt](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/src/main/java/com/example/mindspark2/RegisterActivity.kt)
- Update `RegisterScreen` layout:
    - Centered "Register" title.
    - Input fields for Full Name, Email, Password, Confirm Password with icons.
    - Blue button and "Login" link styling.
    - Background wave/decorations.

#### [HomeActivity.kt](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/src/main/java/com/example/mindspark2/HomeActivity.kt)
- Completely redesign `HomeScreen`:
    - Top blue wave header with "Home", Hamburger, and Notification icons.
    - "Hello, User!" greeting.
    - Grid layout for 6 main features: Capture Image, Capture Video, History, Tutorial, Settings, Profile.

#### [CameraScreen.kt](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/src/main/java/com/example/mindspark2/CameraScreen.kt)
- Refine `CameraScreen`:
    - Top bar with back, flash, and flip icons.
    - Green guide box.
    - Bottom controls (Gallery, Shutter, Switch Camera).

#### [ImageActivity.kt](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/src/main/java/com/example/mindspark2/ImageActivity.kt)
- Update `MediaTranslationScreen` to match "Translation Result" screen:
    - Blue header bar.
    - Result image card.
    - Sinhala and English translation sections with audio icons.
    - Bottom buttons: Play All, Share, Translate Again.

### New Screens [NEW]

#### [WelcomeScreen.kt](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/src/main/java/com/example/mindspark2/WelcomeScreen.kt)
- Implement the "Welcome to MindSparks" screen with the character illustration and Login/Register buttons.

#### [HistoryScreen.kt](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/src/main/java/com/example/mindspark2/HistoryScreen.kt)
- Implement the "History" list with date, time, and play icons.

#### [TutorialScreen.kt](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/src/main/java/com/example/mindspark2/TutorialScreen.kt)
- Implement the "Tutorial" list with categories like Introduction, Basic Alphabet, etc.

#### [SettingsScreen.kt](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/src/main/java/com/example/mindspark2/SettingsScreen.kt)
- Implement the "Settings" screen with profile info, account, language, voice, theme, help, and logout.

## Verification Plan

### Manual Verification
- Render Compose Previews for each screen to ensure pixel-perfect matches with the screenshot.
- Deploy to the device/emulator to verify navigation flow and interactions.
