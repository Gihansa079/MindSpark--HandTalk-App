# Fix Syntax Error in CameraScreen.kt

The project build is failing due to a syntax error in `CameraScreen.kt` where two variable declarations are placed on the same line without a separator.

## Proposed Changes

### [Component: UI]

#### [MODIFY] [CameraScreen.kt](file:///C:/Users/user/AndroidStudioProjects/mindspark2/app/src/main/java/com/example/mindspark2/CameraScreen.kt)
- Fix the syntax error on line 66 by separating `lensFacing` and `isFlashOn` declarations into individual lines.
- Clean up redundant/duplicate `Box` nesting in the Shutter Button implementation (around line 335) to improve code readability and remove suspicious comments.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:compileDebugKotlin` to verify that the syntax error is resolved and the project builds successfully.

### Manual Verification
- None required as this is a syntax fix.
