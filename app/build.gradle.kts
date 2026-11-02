plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "org.leafvibe.leafbridge"
    compileSdk = 35

    defaultConfig {
        applicationId = "org.leafvibe.leafbridge"
        minSdk = 26
        targetSdk = 35
        versionCode = 6
        versionName = "1.5.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}
