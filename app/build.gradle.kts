plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "org.leafvibe.leafbridge"
    compileSdk = 35

    defaultConfig {
        // Separate preview package preserves the user's previously installed LeafBridge.
        applicationId = "org.leafvibe.leafbridge.preview"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.2.0-preview"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}