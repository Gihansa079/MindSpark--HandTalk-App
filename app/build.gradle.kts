// Project එකට අවශ්‍ය Plugins එකතු කිරීම (Version Catalog / libs හරහා)
plugins {
    alias(libs.plugins.android.application) // Android Application Plugin එක
    alias(libs.plugins.kotlin.android)      // Kotlin Android support එක ලබාදෙයි
    alias(libs.plugins.kotlin.compose)      // Jetpack Compose Kotlin Compiler support එක
    alias(libs.plugins.ksp)                 // Kotlin Symbol Processing (Room වැනි libraries වල code generation සඳහා)
}

android {
    // App එකේ Package Name එක
    namespace = "com.example.mindspark2"

    // App එක Build කිරීමේදී භාවිතා කරන Android SDK සංස්කරණය
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.mindspark2" // App එකේ Unique Identifier එක
        minSdk = 26                             // App එක run කළ හැකි අවම Android සංස්කරණය (Android 8.0)
        targetSdk = 36                          // App එක target කරන Android සංස්කරණය
        versionCode = 1                         // App එකේ Version අංකය (Internal)
        versionName = "1.0"                     // පරිශීලකයින්ට පෙනෙන Version නම

        // UI testing සඳහා වන Test Runner එක
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false // Code එක compress කිරීම/shrink කිරීම අක්‍රිය කර ඇත
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // Java Version එක සැකසීම (SDK 36 සඳහා Java 17 භාවිත කරයි)
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Kotlin Compiler එකේ Target JVM එක Java 17 ලෙස සැකසීම
    kotlinOptions {
        jvmTarget = "17"
    }

    // Jetpack Compose භාවිතය සක්‍රිය කිරීම
    buildFeatures {
        compose = true
    }

    // TFLite Model File එක Compress වීම වැළැක්වීම
    androidResources {
        noCompress += "tflite"
    }
}

dependencies {
    // Compose සහ Android Core Libraries (Version Catalog හරහා)
    implementation(platform(libs.androidx.compose.bom)) // Compose Libraries වල Versions Manage කරන Bill of Materials
    implementation(libs.androidx.activity.compose)      // Compose සමඟ Activity සම්බන්ධ කිරීම
    implementation(libs.androidx.compose.material3)     // Material Design 3 UI Components
    implementation(libs.androidx.compose.ui)            // Compose UI Core Library
    implementation(libs.androidx.compose.ui.graphics)   // Compose Graphics Support
    implementation(libs.androidx.compose.ui.tooling.preview) // Android Studio හි UI Preview පෙනීමට
    implementation(libs.androidx.core.ktx)               // Kotlin Extensions for Android Core
    implementation(libs.androidx.lifecycle.runtime.ktx)  // App Lifecycle Management Support
    implementation(libs.androidx.lifecycle.runtime.compose) // Compose Lifecycle Listeners
    implementation(libs.androidx.navigation.compose)     // Screens අතර Navigate වීම සඳහා Navigation Support

    // CameraX Libraries (කැමරාව භාවිතයෙන් පින්තූර / වීඩියෝ ගැනුමට)
    val cameraxVersion = "1.3.4"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // Permissions (Camera/Storage අනුමැතීන් ලේසියෙන් පරිශීලකයාගෙන් ලබාගැනීමට)
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")

    // Coil Library (Web URLs වලින් පින්තූර Load කර පෙන්වීමට)
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Room Database (Local Data Storage සඳහා)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler) // KSP භාවිතයෙන් Room Database Code එක Auto-Generate කරයි

    // Testing Libraries (Unit test සහ UI test සඳහා)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Retrofit & Gson (Backend REST API සමඟ සම්බන්ධ වී Data ලබාගැනීමට)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Media3 ExoPlayer (App එක ඇතුළේ වීඩියෝ Play කර පෙන්වීමට)
    implementation("androidx.media3:media3-exoplayer:1.2.1")
    implementation("androidx.media3:media3-ui:1.2.1")

    // TensorFlow Lite (Gesture Classification සඳහා)
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")

    // MediaPipe Hand Landmarker Library එක
    implementation("com.google.mediapipe:tasks-vision:0.10.14")
}
